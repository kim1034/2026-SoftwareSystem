package com.sadamda.view;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 상단 메뉴의 선 아이콘 (장바구니·주문 내역·내 정보 등).
 * 그림 글자(이모지)는 PC 글꼴에 따라 네모로 나올 수 있어서 직접 그린다.
 */
public final class MenuIcon implements Icon {
    public enum Shape { CART, RECEIPT, PERSON, PERSON_PLUS, FAMILY, LOGOUT, GEAR, SEARCH }

    private final Shape shape;
    private final int size;
    private final Color color;

    public MenuIcon(Shape shape, int size, Color color) {
        this.shape = shape;
        this.size = size;
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = Theme.smooth(graphics);
        g.translate(x, y);
        float s = size;
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(1.6f, s / 13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (shape) {
            case CART -> {
                Path2D cart = new Path2D.Float();
                cart.moveTo(s * 0.06, s * 0.14);
                cart.lineTo(s * 0.20, s * 0.14);
                cart.lineTo(s * 0.32, s * 0.64);
                cart.lineTo(s * 0.80, s * 0.64);
                cart.lineTo(s * 0.92, s * 0.30);
                cart.lineTo(s * 0.25, s * 0.30);
                g.draw(cart);
                g.fill(new Ellipse2D.Float(s * 0.33f, s * 0.76f, s * 0.12f, s * 0.12f));
                g.fill(new Ellipse2D.Float(s * 0.68f, s * 0.76f, s * 0.12f, s * 0.12f));
            }
            case RECEIPT -> {
                g.draw(new RoundRectangle2D.Float(s * 0.20f, s * 0.08f, s * 0.60f, s * 0.84f, s * 0.12f, s * 0.12f));
                for (float line : new float[]{0.32f, 0.50f, 0.68f}) {
                    g.draw(new Line2D.Float(s * 0.34f, s * line, s * 0.66f, s * line));
                }
            }
            case PERSON, PERSON_PLUS -> {
                float cx = shape == Shape.PERSON ? 0.5f : 0.42f;
                g.draw(new Ellipse2D.Float(s * (cx - 0.17f), s * 0.08f, s * 0.34f, s * 0.34f));
                g.draw(new Arc2D.Float(s * (cx - 0.34f), s * 0.52f, s * 0.68f, s * 0.70f, 0, 180, Arc2D.OPEN));
                if (shape == Shape.PERSON_PLUS) {
                    g.draw(new Line2D.Float(s * 0.86f, s * 0.30f, s * 0.86f, s * 0.58f));
                    g.draw(new Line2D.Float(s * 0.72f, s * 0.44f, s * 1.00f, s * 0.44f));
                }
            }
            case FAMILY -> { // 어른 둘이 나란히
                g.draw(new Ellipse2D.Float(s * 0.14f, s * 0.12f, s * 0.26f, s * 0.26f));
                g.draw(new Arc2D.Float(s * 0.02f, s * 0.48f, s * 0.50f, s * 0.70f, 0, 180, Arc2D.OPEN));
                g.draw(new Ellipse2D.Float(s * 0.60f, s * 0.22f, s * 0.22f, s * 0.22f));
                g.draw(new Arc2D.Float(s * 0.52f, s * 0.56f, s * 0.42f, s * 0.58f, 0, 180, Arc2D.OPEN));
            }
            case LOGOUT -> {
                Path2D door = new Path2D.Float();
                door.moveTo(s * 0.52, s * 0.12);
                door.lineTo(s * 0.18, s * 0.12);
                door.lineTo(s * 0.18, s * 0.88);
                door.lineTo(s * 0.52, s * 0.88);
                g.draw(door);
                g.draw(new Line2D.Float(s * 0.40f, s * 0.50f, s * 0.90f, s * 0.50f));
                Path2D arrow = new Path2D.Float();
                arrow.moveTo(s * 0.74, s * 0.34);
                arrow.lineTo(s * 0.90, s * 0.50);
                arrow.lineTo(s * 0.74, s * 0.66);
                g.draw(arrow);
            }
            case GEAR -> {
                g.draw(new Ellipse2D.Float(s * 0.36f, s * 0.36f, s * 0.28f, s * 0.28f));
                g.draw(new Ellipse2D.Float(s * 0.18f, s * 0.18f, s * 0.64f, s * 0.64f));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    g.draw(new Line2D.Double(s * (0.5 + 0.32 * Math.cos(a)), s * (0.5 + 0.32 * Math.sin(a)),
                            s * (0.5 + 0.44 * Math.cos(a)), s * (0.5 + 0.44 * Math.sin(a))));
                }
            }
            case SEARCH -> {
                g.draw(new Ellipse2D.Float(s * 0.12f, s * 0.12f, s * 0.56f, s * 0.56f));
                g.draw(new Line2D.Float(s * 0.60f, s * 0.60f, s * 0.88f, s * 0.88f));
            }
        }
        g.dispose();
    }
}
