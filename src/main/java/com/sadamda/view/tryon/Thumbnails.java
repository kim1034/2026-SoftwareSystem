package com.sadamda.view.tryon;

import com.sadamda.external.RemoteImages;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 상품 사진을 화면 밖에서 내려받아 작은 그림으로 만든다. 받지 못한 사진은 회색 기본 그림이 된다. */
final class Thumbnails {
    private static final Map<String, ImageIcon> ICONS = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final ExecutorService LOADER = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "thumbnail-loader");
        thread.setDaemon(true);
        return thread;
    });

    private Thumbnails() {
    }

    /**
     * 이미 만든 그림이 있으면 바로 돌려준다. 없으면 기본 그림을 돌려주고,
     * 내려받기가 끝나면 화면 스레드에서 onLoaded 를 부른다 (다시 그리라는 신호).
     */
    static ImageIcon get(String url, int size, Runnable onLoaded) {
        String key = size + "|" + url;
        ImageIcon icon = ICONS.get(key);
        if (icon != null) {
            return icon;
        }
        if (url != null && LOADING.add(key)) {
            LOADER.submit(() -> {
                BufferedImage image = RemoteImages.fetchImage(url);
                ICONS.put(key, new ImageIcon(image == null ? placeholder(size) : fit(image, size)));
                SwingUtilities.invokeLater(onLoaded);
            });
        }
        return new ImageIcon(placeholder(size));
    }

    /** 정사각형 안에 비율을 지켜 넣는다. */
    private static BufferedImage fit(BufferedImage source, int size) {
        double scale = Math.min((double) size / source.getWidth(), (double) size / source.getHeight());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, (size - width) / 2, (size - height) / 2, width, height, null);
        g.dispose();
        return target;
    }

    private static BufferedImage placeholder(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(235, 235, 235));
        g.fillRect(0, 0, size, size);
        g.setColor(new Color(200, 200, 200));
        g.drawRect(0, 0, size - 1, size - 1);
        g.dispose();
        return image;
    }
}
