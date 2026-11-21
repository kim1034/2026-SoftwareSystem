package com.sadamda.model.tryon;

import com.sadamda.common.AppException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 내 사진으로 입어보기에 올린 전신 사진 (UC-09).
 *
 * 개인정보 (NFR-08)
 * - 파일로 저장하지 않고 이 객체(메모리)에만 둔다. [사진 지우기]·다른 사진·앱 종료 때 사라진다.
 * - 원본 파일 내용은 들고 있지 않는다. AI 로 보낼 때는 다시 만든 JPEG 를 보내서 촬영 위치(GPS) 같은 EXIF 정보가 빠진다.
 * - 휴대폰 사진은 EXIF 방향값을 읽어 바로 세운다 (ImageIO 는 방향값을 무시해서 옆으로 누운 사진이 된다).
 */
public final class UserPhoto {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    public static final List<String> EXTENSIONS = List.of("jpg", "jpeg", "png");
    /** 이보다 작으면 옷과 얼굴을 알아보기 어렵다. */
    static final int MIN_WIDTH = 200;
    static final int MIN_HEIGHT = 300;
    /** AI 로 보내는 사진의 긴 변(px). */
    private static final int AI_SIDE = 1536;

    private final String fileName;
    private final BufferedImage image;

    private UserPhoto(String fileName, BufferedImage image) {
        this.fileName = fileName;
        this.image = image;
    }

    /**
     * 사진을 확인한다. 순서: 동의(2a) → 비어 있음 → 형식(3a) → 용량(3a) → 열 수 있는지 → 크기.
     * 문제가 있으면 화면에 그대로 보여줄 문장으로 AppException.
     */
    public static UserPhoto of(String fileName, byte[] bytes, boolean agreed) {
        if (!agreed) {
            throw new AppException("사진 이용에 동의해야 내 사진으로 입어 볼 수 있습니다.");
        }
        if (bytes == null || bytes.length == 0) {
            throw new AppException("사진을 골라 주세요.");
        }
        if (!hasAllowedExtension(fileName)) {
            throw new AppException("JPG 또는 PNG 사진만 올릴 수 있습니다.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new AppException("사진 용량은 10MB 이하여야 합니다.");
        }
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException | RuntimeException e) {
            decoded = null;
        }
        if (decoded == null) {
            throw new AppException("사진 파일을 열 수 없습니다. 다른 사진을 골라 주세요.");
        }
        BufferedImage upright = orient(decoded, exifOrientation(bytes));
        if (upright.getWidth() < MIN_WIDTH || upright.getHeight() < MIN_HEIGHT) {
            throw new AppException("사진이 너무 작습니다. 전신이 크게 나온 사진을 올려 주세요.");
        }
        return new UserPhoto(fileName, upright);
    }

    /** 마지막 . 뒤 확장자가 jpg·jpeg·png 인지 (대소문자 무시). */
    static boolean hasAllowedExtension(String fileName) {
        if (fileName == null) {
            return false;
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public String fileName() {
        return fileName;
    }

    /** 방향을 바로잡은 사진. 화면 미리보기에 쓴다. */
    public BufferedImage image() {
        return image;
    }

    /** AI 로 보낼 사진: 긴 변 1536px 이하로 줄인 JPEG. 원본의 EXIF 정보는 들어가지 않는다. */
    public byte[] jpegForAi() {
        double scale = Math.min(1.0, (double) AI_SIDE / Math.max(image.getWidth(), image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(scaled, "jpg", out);
        } catch (IOException e) {
            throw new AppException("사진을 준비하지 못했습니다. 다른 사진을 골라 주세요.");
        }
        return out.toByteArray();
    }

    /** JPEG 의 EXIF 방향값(1~8). 없거나 읽을 수 없으면 1(그대로). */
    static int exifOrientation(byte[] jpeg) {
        if (jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
            return 1;
        }
        int pos = 2;
        while (pos + 4 <= jpeg.length && (jpeg[pos] & 0xFF) == 0xFF) {
            int marker = jpeg[pos + 1] & 0xFF;
            int length = ((jpeg[pos + 2] & 0xFF) << 8) | (jpeg[pos + 3] & 0xFF);
            if (marker == 0xDA || length < 2) {
                return 1; // 그림 데이터 시작: 그 앞에 EXIF 가 없었다
            }
            int start = pos + 4;
            if (marker == 0xE1 && start + 6 <= jpeg.length && "Exif".equals(new String(jpeg, start, 4,
                    java.nio.charset.StandardCharsets.US_ASCII))) {
                return tiffOrientation(jpeg, start + 6, Math.min(jpeg.length, pos + 2 + length));
            }
            pos += 2 + length;
        }
        return 1;
    }

    private static int tiffOrientation(byte[] data, int tiff, int end) {
        if (tiff + 8 > end) {
            return 1;
        }
        boolean little = data[tiff] == 'I' && data[tiff + 1] == 'I';
        long ifd = readInt(data, tiff + 4, little);
        int dir = (int) (tiff + ifd);
        if (ifd < 8 || dir + 2 > end) {
            return 1;
        }
        int count = readShort(data, dir, little);
        for (int i = 0; i < count; i++) {
            int entry = dir + 2 + i * 12;
            if (entry + 12 > end) {
                return 1;
            }
            if (readShort(data, entry, little) == 0x0112) {
                int value = readShort(data, entry + 8, little);
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    private static int readShort(byte[] data, int at, boolean little) {
        int a = data[at] & 0xFF;
        int b = data[at + 1] & 0xFF;
        return little ? (b << 8) | a : (a << 8) | b;
    }

    private static long readInt(byte[] data, int at, boolean little) {
        long value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = little ? i * 8 : (3 - i) * 8;
            value |= (long) (data[at + i] & 0xFF) << shift;
        }
        return value;
    }

    /** EXIF 방향값대로 돌리거나 뒤집어 바로 세운다. 투명한 부분은 흰색이 된다. */
    static BufferedImage orient(BufferedImage source, int orientation) {
        int w = source.getWidth();
        int h = source.getHeight();
        boolean swap = orientation >= 5 && orientation <= 8;
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2 -> { t.translate(w, 0); t.scale(-1, 1); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.translate(0, h); t.scale(1, -1); }
            case 5 -> { t.rotate(-Math.PI / 2); t.scale(-1, 1); }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { t.translate(h, w); t.rotate(Math.PI / 2); t.scale(-1, 1); }
            case 8 -> { t.translate(0, w); t.rotate(-Math.PI / 2); }
            default -> { }
        }
        BufferedImage target = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, target.getWidth(), target.getHeight());
        g.drawImage(source, t, null);
        g.dispose();
        return target;
    }
}
