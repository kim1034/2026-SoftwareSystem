package com.sadamda.view.product;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/** 브랜드 이름 뒤의 가는 ">" 표시. 글씨 크기(fontSize)에 맞춰 커진다. 상품 카드 두 종류가 함께 쓴다. */
final class ChevronIcon implements Icon {
    private final int width;
    private final int height;
    private final Color color;

    ChevronIcon(float fontSize, Color color) {
        this.width = Math.max(6, Math.round(fontSize * 0.32f));
        this.height = Math.max(10, Math.round(fontSize * 0.58f));
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D chevron = new Path2D.Float();
        chevron.moveTo(x + 1, y + 1);
        chevron.lineTo(x + width - 1, y + height / 2f);
        chevron.lineTo(x + 1, y + height - 1);
        g2.draw(chevron);
        g2.dispose();
    }
}
