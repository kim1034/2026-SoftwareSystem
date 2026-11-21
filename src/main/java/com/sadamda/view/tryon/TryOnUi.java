package com.sadamda.view.tryon;

import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 옷 입혀보기 화면들이 함께 쓰는 그리기 도구.
 * 그림 글자(이모지·✓ 등)는 PC 글꼴에 따라 네모로 나올 수 있어서 아이콘은 직접 그린다.
 */
final class TryOnUi {
    /** 연한 초록 (✓ 표시). 흰 바탕 대비 5.9:1. */
    static final Color OK = new Color(0x1B6E2E);

    private TryOnUi() {
    }

    static Graphics2D smooth(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    /** 흰 바탕 둥근 카드 패널 (Theme.card 와 같은 모양, 여백은 작게). */
    static JPanel card(LayoutManager layout, int padding) {
        JPanel card = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = smooth(graphics);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 24, 24);
                g.setColor(Theme.CARD);
                g.fill(shape);
                g.setColor(Theme.LINE);
                g.draw(shape);
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(padding, padding + 4, padding, padding + 4));
        return card;
    }

    /** Theme 버튼은 좌우 여백이 넓어서, 버튼이 많은 이 화면에서는 좁힌다. 높이는 48px 그대로 (NFR-02). */
    static <T extends JComponent> T compact(T button) {
        button.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        return button;
    }

    static JButton secondary(String text) {
        return compact(Theme.secondaryButton(text));
    }

    static JButton primary(String text) {
        return compact(Theme.primaryButton(text));
    }

    /** 그림을 칸에 꽉 차게(넘치는 쪽은 잘라) 그린다. */
    static void drawCover(Graphics2D g, BufferedImage image, int x, int y, int width, int height, double focusY) {
        double scale = Math.max((double) width / image.getWidth(), (double) height / image.getHeight());
        int w = (int) Math.ceil(image.getWidth() * scale);
        int h = (int) Math.ceil(image.getHeight() * scale);
        int dx = x + (width - w) / 2;
        int dy = y + (int) Math.round((height - h) * focusY);
        g.drawImage(image, dx, dy, w, h, null);
    }

    /** 그림 전체가 보이게 가운데 맞춰 그린다. */
    static void drawContain(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
        double scale = Math.min((double) width / image.getWidth(), (double) height / image.getHeight());
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        g.drawImage(image, x + (width - w) / 2, y + (height - h) / 2, w, h, null);
    }

    /** 폭에 맞춰 최대 maxLines 줄로 나눈다. 넘치면 마지막 줄 끝을 … 으로 줄인다. 한글은 글자 단위로 끊는다. */
    static List<String> wrap(String text, FontMetrics metrics, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        String rest = text == null ? "" : text.trim();
        while (!rest.isEmpty() && lines.size() < maxLines) {
            int end = 1;
            while (end < rest.length() && metrics.stringWidth(rest.substring(0, end + 1)) <= width) {
                end++;
            }
            if (end < rest.length()) {
                int space = rest.lastIndexOf(' ', end);
                if (space > 0) {
                    end = space; // 띄어쓰기에서 끊는다 ("벚꽃 / 가로수길", 영어 상품명)
                }
            }
            String line = rest.substring(0, end).trim();
            rest = rest.substring(end).trim();
            if (lines.size() == maxLines - 1 && !rest.isEmpty()) {
                line = ellipsize(line + " " + rest, metrics, width);
                rest = "";
            }
            lines.add(line);
        }
        return lines;
    }

    /** 폭을 넘으면 끝을 … 으로 줄인다. */
    static String ellipsize(String text, FontMetrics metrics, int width) {
        if (text == null) {
            return "";
        }
        if (metrics.stringWidth(text) <= width) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && metrics.stringWidth(cut + "…") > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.trim() + "…";
    }

    /** 사진기 모양 아이콘. 버튼 글자색으로 그려서 고른 상태(흰 글씨)에서도 보인다. */
    static Icon cameraIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = smooth(graphics);
                g.setColor(c.getForeground());
                float s = size;
                g.setStroke(new BasicStroke(Math.max(1.6f, s / 12f)));
                g.draw(new RoundRectangle2D.Float(x + s * 0.06f, y + s * 0.28f, s * 0.88f, s * 0.62f, s * 0.18f, s * 0.18f));
                g.fill(new RoundRectangle2D.Float(x + s * 0.32f, y + s * 0.14f, s * 0.36f, s * 0.18f, s * 0.08f, s * 0.08f));
                g.draw(new java.awt.geom.Ellipse2D.Float(x + s * 0.33f, y + s * 0.42f, s * 0.34f, s * 0.34f));
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /** ✓ 표시를 그린다 (글꼴에 ✓ 글자가 없을 수 있어서). */
    static void drawCheck(Graphics2D g, float x, float y, float size, Color color, float stroke) {
        Path2D check = new Path2D.Float();
        check.moveTo(x + size * 0.12f, y + size * 0.52f);
        check.lineTo(x + size * 0.40f, y + size * 0.80f);
        check.lineTo(x + size * 0.90f, y + size * 0.22f);
        g.setColor(color);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(check);
    }

    /** 글자 앞에 붙이는 초록 ✓ 아이콘. */
    static Icon checkIcon(int size) {
        return checkIcon(size, OK);
    }

    static Icon checkIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = smooth(graphics);
                drawCheck(g, x, y, size, color, Math.max(2f, size / 7f));
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /**
     * 버튼을 왼쪽부터 늘어놓고, 폭이 모자라면 다음 줄로 넘기는 배치. FlowLayout 과 달리 가장자리에 여백을 두지 않아
     * 아래 칸들과 왼쪽 끝이 맞는다. 줄마다 남는 폭은 그 줄 버튼들에 나눠 주어 오른쪽 끝도 맞춘다.
     */
    static final class Rows implements java.awt.LayoutManager {
        private final int gap;

        Rows(int gap) {
            this.gap = gap;
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public java.awt.Dimension preferredLayoutSize(java.awt.Container parent) {
            int width = availableWidth(parent);
            int rowsHeight = 0;
            int rowHeight = 0;
            int x = 0;
            int widest = 0;
            for (Component c : parent.getComponents()) {
                java.awt.Dimension d = c.getPreferredSize();
                if (x > 0 && x + gap + d.width > width) {
                    rowsHeight += rowHeight + gap;
                    widest = Math.max(widest, x);
                    x = 0;
                    rowHeight = 0;
                }
                x += (x > 0 ? gap : 0) + d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            java.awt.Insets in = parent.getInsets();
            return new java.awt.Dimension(Math.max(widest, x) + in.left + in.right,
                    rowsHeight + rowHeight + in.top + in.bottom);
        }

        @Override
        public java.awt.Dimension minimumLayoutSize(java.awt.Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(java.awt.Container parent) {
            java.awt.Insets in = parent.getInsets();
            int width = parent.getWidth() - in.left - in.right;
            List<Component> row = new ArrayList<>();
            int x = 0;
            int y = in.top;
            for (Component c : parent.getComponents()) {
                java.awt.Dimension d = c.getPreferredSize();
                if (x > 0 && x + gap + d.width > width) {
                    y = placeRow(row, in.left, y, width);
                    row.clear();
                    x = 0;
                }
                x += (x > 0 ? gap : 0) + d.width;
                row.add(c);
            }
            placeRow(row, in.left, y, width);
        }

        /** 한 줄을 놓는다. 남는 폭은 버튼마다 똑같이 나눠 준다. 다음 줄의 y 를 돌려준다. */
        private int placeRow(List<Component> row, int left, int y, int width) {
            if (row.isEmpty()) {
                return y;
            }
            int used = gap * (row.size() - 1);
            int height = 0;
            for (Component c : row) {
                used += c.getPreferredSize().width;
                height = Math.max(height, c.getPreferredSize().height);
            }
            int extra = Math.max(0, width - used) / row.size();
            int x = left;
            for (Component c : row) {
                int w = c.getPreferredSize().width + extra;
                c.setBounds(x, y, w, height);
                x += w + gap;
            }
            return y + height + gap;
        }

        /** 자리 폭. 아직 놓이지 않았으면 부모의 폭을 쓴다 (처음 그릴 때). */
        private int availableWidth(java.awt.Container parent) {
            java.awt.Container sized = parent;
            while (sized.getWidth() == 0 && sized.getParent() != null) {
                sized = sized.getParent();
            }
            java.awt.Insets in = parent.getInsets();
            int width = sized.getWidth() - in.left - in.right;
            return width > 0 ? width : Integer.MAX_VALUE;
        }
    }

    /** "<", ">", "&" 를 HTML 글자로 바꾼다 (상품명을 HTML 라벨에 넣을 때). */
    static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
