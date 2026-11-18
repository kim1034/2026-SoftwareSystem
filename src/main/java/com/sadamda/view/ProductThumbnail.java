package com.sadamda.view;

import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** 장바구니·주문서·주문 내역 줄의 작은 상품 사진 (3:4, 칸에 꽉 차게). 글씨 크기를 따라 커진다. */
public class ProductThumbnail extends JComponent {
    private final String url;

    public ProductThumbnail(String url) {
        this.url = url;
        ProductImages.request(url, this::repaint);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = Math.round(UIManager.getFont("Label.font").getSize2D() * 6);
        return new Dimension(width, Math.round(width * 4f / 3));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        Dimension size = getPreferredSize();
        RoundRectangle2D box = new RoundRectangle2D.Float(0, 0, size.width, size.height, 12, 12);
        g2.setColor(Theme.PAGE);
        g2.fill(box);
        BufferedImage image = ProductImages.get(url);
        if (image != null) {
            double scale = Math.max(size.width / (double) image.getWidth(), size.height / (double) image.getHeight());
            int width = (int) Math.round(image.getWidth() * scale);
            int height = (int) Math.round(image.getHeight() * scale);
            g2.clip(box);
            g2.drawImage(image, (size.width - width) / 2, (size.height - height) / 2, width, height, null);
        } else {
            String text = ProductImages.failed(url) ? "사진 없음" : "…";
            g2.setColor(Theme.HINT);
            g2.setFont(UIManager.getFont("Label.font"));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (size.width - fm.stringWidth(text)) / 2,
                    (size.height - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();
    }
}
