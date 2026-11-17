package com.sadamda.view.tryon;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** 옷 입혀보기 화면 왼쪽의 큰 캐릭터 칸. 모서리가 둥글고, 합성 이미지를 비율에 맞춰 크게 보여준다. */
class CharacterView extends JPanel {
    private static final int ARC = 80;
    private BufferedImage image;
    private String message = "";

    CharacterView() {
        setOpaque(false);
        setPreferredSize(new Dimension(480, 640));
    }

    /** 보여줄 캐릭터 이미지. null 이면 안내 문구만 보인다. */
    void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    /** 이미지가 없을 때 가운데에 보여줄 안내 문구. */
    void setMessage(String message) {
        this.message = message == null ? "" : message;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        RoundRectangle2D frame = new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 4, ARC, ARC);
        g.setColor(Color.WHITE);
        g.fill(frame);

        if (image != null) {
            double scale = Math.min((double) (getWidth() - 4) / image.getWidth(),
                    (double) (getHeight() - 4) / image.getHeight());
            int width = (int) Math.round(image.getWidth() * scale);
            int height = (int) Math.round(image.getHeight() * scale);
            Graphics2D clipped = (Graphics2D) g.create();
            clipped.setClip(frame);
            clipped.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
            clipped.dispose();
        } else {
            g.setColor(new Color(70, 70, 70)); // 흰 바탕과 명도 대비 4.5:1 이상 (NFR-01)
            g.setFont(getFont());
            FontMetrics metrics = g.getFontMetrics();
            String[] lines = message.split("\n");
            int y = getHeight() / 2 - (lines.length - 1) * metrics.getHeight() / 2;
            for (String line : lines) {
                g.drawString(line, (getWidth() - metrics.stringWidth(line)) / 2, y);
                y += metrics.getHeight();
            }
        }

        g.setColor(new Color(40, 40, 40));
        g.setStroke(new BasicStroke(3));
        g.draw(frame);
        g.dispose();
    }
}
