package com.sadamda.view.product;

import com.sadamda.external.RemoteImages;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 상품·룩북 사진을 화면 스레드 밖에서 내려받아 원하는 크기의 그림으로 만든다.
 * 한 번 만든 그림은 메모리에 두고, 받지 못한 사진은 "사진 준비 중" 기본 그림으로 보여준다.
 */
public final class ProductImages {
    private static final Map<String, ImageIcon> ICONS = new ConcurrentHashMap<>();
    private static final Map<String, List<Runnable>> WAITERS = new HashMap<>();
    private static final ExecutorService LOADER = Executors.newFixedThreadPool(6, runnable -> {
        Thread thread = new Thread(runnable, "product-image-loader");
        thread.setDaemon(true); // 내려받는 중이어도 프로그램은 바로 꺼진다
        return thread;
    });

    private ProductImages() {
    }

    /**
     * 이미 만든 그림이 있으면 바로 돌려준다. 없으면 기본 그림을 돌려주고,
     * 내려받기가 끝나면 화면 스레드에서 onLoaded 를 부른다 (그림을 바꿔 끼우라는 신호).
     */
    public static ImageIcon get(String url, int width, int height, Runnable onLoaded) {
        String key = width + "x" + height + "|" + url;
        ImageIcon icon = ICONS.get(key);
        if (icon != null) {
            return icon;
        }
        if (url == null || url.isBlank()) {
            return new ImageIcon(placeholder(width, height));
        }
        boolean first;
        synchronized (WAITERS) {
            List<Runnable> waiters = WAITERS.computeIfAbsent(key, k -> new ArrayList<>());
            first = waiters.isEmpty();
            waiters.add(onLoaded == null ? () -> { } : onLoaded); // 같은 사진을 기다리는 카드가 여럿이어도 모두 알린다
        }
        if (first) {
            LOADER.submit(() -> {
                BufferedImage image = RemoteImages.fetchImage(url);
                ICONS.put(key, new ImageIcon(image == null ? placeholder(width, height) : fit(image, width, height)));
                List<Runnable> waiters;
                synchronized (WAITERS) {
                    waiters = WAITERS.remove(key);
                }
                if (waiters != null) {
                    SwingUtilities.invokeLater(() -> waiters.forEach(Runnable::run));
                }
            });
        }
        return new ImageIcon(placeholder(width, height));
    }

    /** 사진이 이미 준비됐는지 (다시 그릴 필요가 있는지 판단). */
    public static boolean isReady(String url, int width, int height) {
        return ICONS.containsKey(width + "x" + height + "|" + url);
    }

    /** 틀 안에 비율을 지켜 넣고 남는 곳은 흰색으로 채운다. */
    private static BufferedImage fit(BufferedImage source, int width, int height) {
        double scale = Math.min((double) width / source.getWidth(), (double) height / source.getHeight());
        int w = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, (width - w) / 2, (height - h) / 2, w, h, null);
        g.dispose();
        return target;
    }

    private static BufferedImage placeholder(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xF3F4F6));
        g.fillRect(0, 0, width, height);
        g.setColor(ShopUi.HINT);
        g.setFont(new Font(Font.DIALOG, Font.PLAIN, Math.max(12, Math.min(width, height) / 12)));
        String text = "사진 준비 중";
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (width - metrics.stringWidth(text)) / 2, height / 2 + metrics.getAscent() / 2);
        g.dispose();
        return image;
    }
}
