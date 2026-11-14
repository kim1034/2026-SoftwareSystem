package com.sadamda.view;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 상품 사진(Product.imageUrls)을 화면을 멈추지 않고 불러와 저장해 둔다. 상품 목록·상세·장바구니가 함께 쓴다.
 * 주소가 http(s)로 시작하면 인터넷에서, 아니면 PC 의 파일 경로로 보고 읽는다. JPG·PNG·GIF 를 읽을 수 있다.
 * 저장소(cache, failed, waiting)는 화면 스레드(EDT)에서만 만진다.
 */
public final class ProductImages {
    /** 큰 사진은 이 폭으로 줄여 저장한다. 목록 카드와 상세 화면에 충분하고 메모리를 아낀다. */
    private static final int MAX_WIDTH = 720;

    private static final Map<String, BufferedImage> cache = new HashMap<>();
    private static final Set<String> failed = new HashSet<>();
    private static final Map<String, List<Runnable>> waiting = new HashMap<>();
    private static final ExecutorService loader = Executors.newFixedThreadPool(4, task -> {
        Thread thread = new Thread(task, "product-image-loader");
        thread.setDaemon(true); // 앱을 닫을 때 기다리지 않는다
        return thread;
    });
    private static final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private ProductImages() {
    }

    /** 불러온 사진. 아직 불러오는 중이거나 실패했으면 null. */
    public static BufferedImage get(String source) {
        return source == null ? null : cache.get(source);
    }

    /** 주소가 없거나, 받을 수 없거나, 사진 파일이 아니면 true. */
    public static boolean failed(String source) {
        return source == null || failed.contains(source);
    }

    /** 사진을 불러오기 시작한다. 끝나면(성공·실패 모두) 화면 스레드에서 onDone 을 부른다. 이미 끝났으면 아무것도 안 한다. */
    public static void request(String source, Runnable onDone) {
        if (source == null || cache.containsKey(source) || failed.contains(source)) {
            return;
        }
        List<Runnable> callbacks = waiting.get(source);
        if (callbacks != null) { // 같은 사진을 이미 받는 중이면 끝날 때 함께 알린다.
            callbacks.add(onDone);
            return;
        }
        waiting.put(source, new ArrayList<>(List.of(onDone)));
        loader.execute(() -> {
            BufferedImage image = load(source);
            SwingUtilities.invokeLater(() -> {
                if (image != null) {
                    cache.put(source, image);
                } else {
                    failed.add(source);
                }
                waiting.remove(source).forEach(Runnable::run);
            });
        });
    }

    /** 실패하면 null. 인터넷이 끊겼거나 주소가 바뀐 경우 화면에는 "사진 없음"이 나온다. */
    private static BufferedImage load(String source) {
        try {
            BufferedImage image;
            if (source.startsWith("http://") || source.startsWith("https://")) {
                // 일부 쇼핑몰은 Java 기본 User-Agent 요청을 막아서 일반 브라우저 값을 쓴다.
                HttpRequest request = HttpRequest.newBuilder(URI.create(source))
                        .header("User-Agent", "Mozilla/5.0")
                        .timeout(Duration.ofSeconds(15))
                        .build();
                HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    return null;
                }
                image = ImageIO.read(new ByteArrayInputStream(response.body()));
            } else {
                image = ImageIO.read(new File(source));
            }
            return image == null ? null : shrink(image);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage shrink(BufferedImage image) {
        if (image.getWidth() <= MAX_WIDTH) {
            return image;
        }
        int height = Math.round(image.getHeight() * (MAX_WIDTH / (float) image.getWidth()));
        BufferedImage small = new BufferedImage(MAX_WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = small.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(image, 0, 0, MAX_WIDTH, height, null);
        g2.dispose();
        return small;
    }
}
