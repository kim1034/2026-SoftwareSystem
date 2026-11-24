package com.sadamda.view.product;

import com.sadamda.view.Theme;

import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 상품 사진 오른쪽 아래의 찜 하트 (UC-10 1a). 상품 목록 카드(ProductCard)와 홈 카드(ShopProductCard)가 함께 쓴다.
 * 찜하지 않았으면 흰 테두리 하트, 찜했으면 빨간 하트. 크기는 48px 정사각형으로 놓는다 (NFR-02).
 */
final class HeartButton extends JButton {
    private static final Color WISHED = new Color(0xE53935);

    static final int SIZE = 48;

    HeartButton() {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void updateUI() {
        setUI(new BasicButtonUI());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float size = getWidth() * 0.66f;
        float x = (getWidth() - size) / 2;
        float y = (getHeight() - size) / 2 + size * 0.04f;
        Path2D heartShape = heart(x, y, size);
        // 밝은 사진 위에서도 하얀 테두리가 보이게, 테두리 바깥에 진한 그림자를 먼저 그린다.
        g2.setColor(new Color(0, 0, 0, 90));
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(heartShape);
        g2.setColor(isSelected() ? WISHED : new Color(0, 0, 0, 45)); // 찜 안 했으면 안쪽을 살짝 어둡게
        g2.fill(heartShape);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(heartShape);
        if (isFocusOwner()) { // 키보드 사용자를 위한 테두리
            g2.setColor(Theme.TEXT);
            g2.draw(new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 12, 12));
        }
        g2.dispose();
    }

    private static Path2D heart(float x, float y, float size) {
        Path2D path = new Path2D.Float();
        path.moveTo(x + size / 2, y + size * 0.92f);
        path.curveTo(x - size * 0.10f, y + size * 0.50f, x + size * 0.02f, y - size * 0.02f, x + size / 2, y + size * 0.24f);
        path.curveTo(x + size * 0.98f, y - size * 0.02f, x + size * 1.10f, y + size * 0.50f, x + size / 2, y + size * 0.92f);
        path.closePath();
        return path;
    }
}
