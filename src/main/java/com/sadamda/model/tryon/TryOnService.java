package com.sadamda.model.tryon;

import com.sadamda.common.AppException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AI 합성 이미지 보관함. 업로드한 인물 사진은 합성 후 바로 삭제한다 (NFR-08). 담당: 정우진
 *
 * 이미지는 이 PC 의 폴더에 PNG 파일로 둔다. (기본: 사용자 홈/.sadamda/tryon)
 * - 보관함: locker/{회원id}/{보관id}_{상품id}_{저장시각}.png  → 프로그램을 다시 켜도 남는다
 * - 업로드 사진: uploads/{회원id}/...                         → 합성이 끝나면 지운다
 */
public class TryOnService {
    /** UC-09 4단계 기본 배경 템플릿 (FR-14). 이름 → {위쪽 색, 아래쪽 색, 바닥 색}. 그림은 코드로 그려 이미지 파일이 따로 없다. */
    private static final Map<String, Color[]> BACKGROUNDS = new LinkedHashMap<>();
    private static final int BACKGROUND_WIDTH = 1024;
    private static final int BACKGROUND_HEIGHT = 1536;

    static {
        BACKGROUNDS.put("흰 스튜디오", new Color[]{new Color(250, 250, 250), new Color(232, 232, 232), new Color(214, 214, 214)});
        BACKGROUNDS.put("베이지 실내", new Color[]{new Color(246, 238, 226), new Color(228, 212, 190), new Color(196, 170, 140)});
        BACKGROUNDS.put("맑은 하늘", new Color[]{new Color(150, 196, 240), new Color(226, 240, 252), new Color(214, 206, 186)});
        BACKGROUNDS.put("초록 공원", new Color[]{new Color(206, 232, 246), new Color(186, 220, 170), new Color(120, 168, 96)});
    }

    private final Path lockerDir;
    private final Path uploadDir;
    private final List<TryOnImage> images = new ArrayList<>();
    private long nextId = 1;

    public TryOnService() {
        this(Path.of(System.getProperty("user.home"), ".sadamda", "tryon"));
    }

    public TryOnService(Path baseDir) {
        this.lockerDir = baseDir.resolve("locker");
        this.uploadDir = baseDir.resolve("uploads");
        loadSavedImages();
    }

    /** UC-09 4단계 기본 배경 템플릿 id 목록 (FR-14). id 는 화면에 그대로 보여줄 이름이다. */
    public List<String> findBackgroundTemplates() {
        return new ArrayList<>(BACKGROUNDS.keySet());
    }

    /** UC-09 6단계 템플릿 id 로 배경 이미지(PNG, 1024×1536)를 만든다. 없는 id 면 AppException. */
    public byte[] loadBackgroundTemplate(String templateId) {
        Color[] colors = templateId == null ? null : BACKGROUNDS.get(templateId);
        if (colors == null) {
            throw new AppException("배경을 찾을 수 없습니다. 목록에서 다시 골라 주세요.");
        }
        BufferedImage image = new BufferedImage(BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        int floorY = (int) (BACKGROUND_HEIGHT * 0.78);
        g.setPaint(new GradientPaint(0, 0, colors[0], 0, floorY, colors[1]));
        g.fillRect(0, 0, BACKGROUND_WIDTH, floorY);
        g.setPaint(new GradientPaint(0, floorY, colors[2], 0, BACKGROUND_HEIGHT, colors[2].darker()));
        g.fillRect(0, floorY, BACKGROUND_WIDTH, BACKGROUND_HEIGHT - floorY);
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AppException("배경을 만들지 못했습니다. 다시 시도해 주세요.");
        }
    }

    /** UC-09 9단계 [보관함에 저장] (FR-16). productId 는 대표 상품이며 없으면 null. */
    public synchronized TryOnImage save(Long memberId, Long productId, byte[] image) {
        if (memberId == null) {
            throw new AppException("로그인한 뒤에 보관함에 저장할 수 있습니다.");
        }
        if (image == null || image.length == 0) {
            throw new AppException("저장할 이미지가 없습니다.");
        }
        long id = nextId++;
        long millis = System.currentTimeMillis();
        Path file = lockerDir.resolve(String.valueOf(memberId))
                .resolve(id + "_" + (productId == null ? 0 : productId) + "_" + millis + ".png");
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, image);
        } catch (IOException e) {
            throw new AppException("이미지를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        TryOnImage saved = new TryOnImage(id, memberId, productId, file.toString(), toDateTime(millis));
        images.add(saved);
        return saved;
    }

    /** 보관함 목록 (FR-16). 최근에 저장한 것부터. */
    public synchronized List<TryOnImage> findByMember(Long memberId) {
        return images.stream()
                .filter(image -> image.memberId().equals(memberId))
                .sorted(Comparator.comparing(TryOnImage::id).reversed())
                .collect(Collectors.toList());
    }

    /** 보관함에서 이미지 1개를 지운다 (NFR-08: 사용자가 언제든 삭제 가능). 본인 것이 아니면 AppException. */
    public synchronized void delete(Long memberId, Long imageId) {
        TryOnImage target = images.stream()
                .filter(image -> image.id().equals(imageId) && image.memberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new AppException("보관함에서 이미지를 찾을 수 없습니다."));
        deleteFile(Path.of(target.imagePath()));
        images.remove(target);
    }

    /** 회원의 보관함 이미지와 업로드 사진을 모두 지운다 (NFR-08, 회원 탈퇴 등). */
    public synchronized void deleteAll(Long memberId) {
        for (TryOnImage image : findByMember(memberId)) {
            deleteFile(Path.of(image.imagePath()));
            images.remove(image);
        }
        deleteUploads(memberId);
    }

    /** UC-09 3단계: 업로드한 인물·배경 사진을 합성이 끝날 때까지 잠시 보관한다. 돌려준 경로로 나중에 지운다. */
    public synchronized String storeUpload(Long memberId, byte[] photo) {
        if (photo == null || photo.length == 0) {
            throw new AppException("사진이 없습니다.");
        }
        Path dir = uploadDir.resolve(String.valueOf(memberId == null ? 0 : memberId));
        try {
            Files.createDirectories(dir);
            Path file = Files.createTempFile(dir, "upload_", ".img");
            Files.write(file, photo);
            return file.toString();
        } catch (IOException e) {
            throw new AppException("사진을 보관하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /** NFR-08: 합성이 끝난(또는 실패한) 뒤 업로드 사진 1장을 지운다. */
    public synchronized void deleteUpload(String uploadPath) {
        if (uploadPath == null) {
            return;
        }
        Path file = Path.of(uploadPath).toAbsolutePath().normalize();
        if (file.startsWith(uploadDir.toAbsolutePath().normalize())) { // 업로드 폴더 밖의 파일은 건드리지 않는다
            deleteFile(file);
        }
    }

    /** NFR-08: 회원의 업로드 사진을 모두 지운다. 지운 개수를 돌려준다. */
    public synchronized int deleteUploads(Long memberId) {
        Path dir = uploadDir.resolve(String.valueOf(memberId == null ? 0 : memberId));
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> targets = files.collect(Collectors.toList());
            targets.forEach(TryOnService::deleteFile);
            return targets.size();
        } catch (IOException e) {
            throw new AppException("업로드 사진을 지우지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /** 프로그램을 다시 켰을 때 보관함 폴더의 파일 이름으로 목록을 되살린다. */
    private void loadSavedImages() {
        if (!Files.isDirectory(lockerDir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(lockerDir, 2)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".png")).collect(Collectors.toList())) {
                try {
                    String[] parts = file.getFileName().toString().replace(".png", "").split("_");
                    long id = Long.parseLong(parts[0]);
                    long productId = Long.parseLong(parts[1]);
                    long memberId = Long.parseLong(file.getParent().getFileName().toString());
                    images.add(new TryOnImage(id, memberId, productId == 0 ? null : productId,
                            file.toString(), toDateTime(Long.parseLong(parts[2]))));
                    nextId = Math.max(nextId, id + 1);
                } catch (RuntimeException ignored) {
                    // 이름 규칙이 다른 파일은 보관함 항목이 아니다
                }
            }
        } catch (IOException ignored) {
            // 폴더를 읽지 못하면 빈 보관함으로 시작한다
        }
    }

    private static LocalDateTime toDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    private static void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new AppException("파일을 지우지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }
}
