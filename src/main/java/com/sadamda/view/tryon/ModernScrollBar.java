package com.sadamda.view.tryon;

import com.sadamda.view.Theme;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

/**
 * 옛날식 화살표·네모 막대 대신, 앱의 알약 모양 버튼과 어울리는 얇고 둥근 스크롤 막대.
 * 화살표 버튼은 없고, 손잡이는 평소 연한 회색, 마우스를 올리면 진한 회색, 끌면 검정이다.
 * 글씨 크기를 바꾸면 Swing 이 막대 모양(UI)을 기본값으로 되돌리므로, updateUI 에서 다시 이 모양을 넣는다.
 */
class ModernScrollBar extends JScrollBar {
    static final int THICKNESS = 12;

    ModernScrollBar(int orientation) {
        super(orientation);
        setOpaque(false);
        setUnitIncrement(24);
    }

    @Override
    public void updateUI() {
        setUI(new UI());
    }

    /** 스크롤 영역의 두 막대를 이 모양으로 바꾸고 테두리를 없앤다. */
    static JScrollPane apply(JScrollPane scroll) {
        scroll.setVerticalScrollBar(new ModernScrollBar(JScrollBar.VERTICAL));
        scroll.setHorizontalScrollBar(new ModernScrollBar(JScrollBar.HORIZONTAL));
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(null);
        return scroll;
    }

    private static class UI extends BasicScrollBarUI {
        private static final Color THUMB = new Color(0xCFC5B5);
        private static final Color TRACK = new Color(0, 0, 0, 12);

        @Override
        protected void configureScrollBarColors() {
            thumbColor = THUMB;
            trackColor = TRACK;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return hiddenButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return hiddenButton();
        }

        private static JButton hiddenButton() {
            JButton button = new JButton();
            Dimension zero = new Dimension(0, 0);
            button.setPreferredSize(zero);
            button.setMinimumSize(zero);
            button.setMaximumSize(zero);
            button.setFocusable(false);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            return button;
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return scrollbar.getOrientation() == JScrollBar.VERTICAL
                    ? new Dimension(THICKNESS, 48) : new Dimension(48, THICKNESS);
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            return new Dimension(THICKNESS * 3, THICKNESS * 3);
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent c, Rectangle r) {
            Graphics2D g = TryOnUi.smooth(graphics);
            g.setColor(TRACK);
            g.fill(pill(r));
            g.dispose();
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g = TryOnUi.smooth(graphics);
            g.setColor(isDragging ? Theme.PRIMARY : isThumbRollover() ? Theme.HINT : THUMB);
            g.fill(pill(r));
            g.dispose();
        }

        /** 막대 칸 안쪽에 2px 여백을 둔 알약 모양. */
        private RoundRectangle2D pill(Rectangle r) {
            float inset = 2f;
            float w = r.width - inset * 2;
            float h = r.height - inset * 2;
            float arc = Math.min(w, h);
            return new RoundRectangle2D.Float(r.x + inset, r.y + inset, w, h, arc, arc);
        }
    }
}
