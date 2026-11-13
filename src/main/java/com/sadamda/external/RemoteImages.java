package com.sadamda.external;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 상품 이미지 URL 을 내려받는 공용 도구. 한 번 받은 이미지는 메모리에 둔다.
 * 받지 못한 이미지는 null 로 돌려주므로, 쓰는 쪽에서 기본 그림을 대신 보여준다.
 */
public final class RemoteImages {
    private static final byte[] FAILED = new byte[0];
    private static final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private RemoteImages() {
    }

    /** 이미지 파일 내용. 주소가 없거나 받지 못하면 null. 화면 스레드에서 부르지 않는다. */
    public static byte[] fetch(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        byte[] bytes = CACHE.computeIfAbsent(url, RemoteImages::download);
        return bytes == FAILED ? null : bytes;
    }

    /** 내려받아 그림으로 푼다. 받지 못했거나 읽을 수 없는 형식이면 null. */
    public static BufferedImage fetchImage(String url) {
        return decode(fetch(url));
    }

    public static BufferedImage decode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** 긴 변이 maxSide 를 넘지 않게 줄인 JPEG 로 바꾼다 (AI 서비스로 보내는 용량 줄이기). 읽을 수 없으면 null. */
    public static byte[] toJpeg(byte[] bytes, int maxSide) {
        BufferedImage source = decode(bytes);
        if (source == null) {
            return null;
        }
        double scale = Math.min(1.0, (double) maxSide / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(target, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] toPng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 만들지 못했습니다", e);
        }
    }

    private static byte[] download(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 (Sadamda Swing)")
                    .GET().build();
            HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200 && response.body().length > 0) {
                return response.body();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            // 받지 못한 이미지는 기본 그림으로 대신한다
        }
        return FAILED;
    }
}
