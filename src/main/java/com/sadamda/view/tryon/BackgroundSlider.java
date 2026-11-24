package com.sadamda.view.tryon;

import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * 배경 버튼을 가로로 늘어놓고 옆으로 넘겨 보는 줄.
 * - 양쪽 둥근 [‹] [›] 버튼으로 한 화면씩 부드럽게 넘긴다 (끝에 닿으면 흐리게)
 * - 마우스로 끌거나, 휠을 굴려도 넘어간다
 * - 아래 스크롤 막대는 얇고 둥근 앱 모양(ModernScrollBar)
 * - 키보드로 버튼에 오면 그 버튼이 보이게 넘긴다
 */
class BackgroundSlider extends JPanel {
    private static final int GAP = 8;

    private final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, 0));
    private final JScrollPane scroll = ModernScrollBar.apply(new JScrollPane(row));
    private final ArrowButton previous = new ArrowButton(false);
    private final ArrowButton next = new ArrowButton(true);
    private final Drag drag = new Drag();
    private Timer animation;

    BackgroundSlider() {
        super(new BorderLayout(8, 0));
        setOpaque(false);
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        row.addMouseListener(drag);
        row.addMouseMotionListener(drag);

        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // 휠을 굴리면 가로로 넘어간다
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getHorizontalScrollBar().setUnitIncrement(48);
        scroll.getHorizontalScrollBar().getModel().addChangeListener(e -> updateArrows());

        previous.addActionListener(e -> slide(-1));
        next.addActionListener(e -> slide(1));
        add(centered(previous), BorderLayout.WEST);
        add(scroll, BorderLayout.CENTER);
        add(centered(next), BorderLayout.EAST);
        updateArrows();
    }

    /** 줄의 버튼을 바꾼다. 끌어서 넘긴 뒤에는 버튼이 눌리지 않게 onPick 을 감싼다. */
    void setTiles(List<BackgroundTile> tiles, List<Runnable> onPick) {
        row.removeAll();
        for (int i = 0; i < tiles.size(); i++) {
            BackgroundTile tile = tiles.get(i);
            Runnable pick = onPick.get(i);
            tile.addActionListener(e -> {
                if (!drag.consume()) {
                    pick.run();
                }
            });
            tile.addMouseListener(drag);
            tile.addMouseMotionListener(drag);
            tile.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    show(tile);
                }
            });
            row.add(tile);
        }
        row.revalidate();
        row.repaint();
        SwingUtilities.invokeLater(this::updateArrows);
    }

    /** 처음으로 되돌린다 (계절을 바꿨을 때). */
    void scrollToStart() {
        stopAnimation();
        scroll.getViewport().setViewPosition(new Point(0, 0));
    }

    /** 그 버튼이 보이게 넘긴다 (고른 배경). */
    void show(Component tile) {
        SwingUtilities.invokeLater(() -> {
            Rectangle bounds = tile.getBounds();
            bounds.grow(GAP * 2, 0);
            row.scrollRectToVisible(bounds);
            updateArrows();
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateArrows();
    }

    /** 한 화면(버튼 하나만큼 겹치게)씩 부드럽게 넘긴다. */
    private void slide(int direction) {
        BoundedRangeModel model = scroll.getHorizontalScrollBar().getModel();
        int tileWidth = row.getComponentCount() == 0 ? 120 : row.getComponent(0).getWidth() + GAP;
        int step = Math.max(tileWidth, scroll.getViewport().getWidth() - tileWidth);
        int from = model.getValue();
        int to = Math.max(model.getMinimum(), Math.min(model.getMaximum() - model.getExtent(), from + direction * step));
        stopAnimation();
        long start = System.currentTimeMillis();
        int duration = 220;
        animation = new Timer(15, e -> {
            double t = Math.min(1.0, (System.currentTimeMillis() - start) / (double) duration);
            double eased = 1 - Math.pow(1 - t, 3); // 천천히 멈춘다
            model.setValue((int) Math.round(from + (to - from) * eased));
            if (t >= 1.0) {
                stopAnimation();
            }
        });
        animation.start();
    }

    private void stopAnimation() {
        if (animation != null) {
            animation.stop();
            animation = null;
        }
    }

    private void updateArrows() {
        BoundedRangeModel model = scroll.getHorizontalScrollBar().getModel();
        boolean scrollable = model.getMaximum() - model.getMinimum() > model.getExtent();
        previous.setEnabled(isEnabled() && scrollable && model.getValue() > model.getMinimum());
        next.setEnabled(isEnabled() && scrollable && model.getValue() + model.getExtent() < model.getMaximum());
    }

    private static JPanel centered(Component component) {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(component);
        return holder;
    }

    /** 마우스로 끌어서 넘기기. 조금이라도 끌었으면 손을 뗄 때 버튼이 눌린 것으로 치지 않는다. */
    private class Drag extends MouseAdapter {
        private int startX;
        private int startValue;
        private boolean dragged;

        @Override
        public void mousePressed(MouseEvent e) {
            stopAnimation();
            startX = e.getXOnScreen();
            startValue = scroll.getHorizontalScrollBar().getValue();
            dragged = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            int dx = e.getXOnScreen() - startX;
            if (Math.abs(dx) > 6) {
                dragged = true;
                row.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            if (dragged) {
                scroll.getHorizontalScrollBar().setValue(startValue - dx);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            row.setCursor(Cursor.getDefaultCursor());
        }

        /** 방금 끌었는지 알려 주고 지운다. */
        boolean consume() {
            boolean was = dragged;
            dragged = false;
            return was;
        }
    }

    /** 둥근 [‹] [›] 버튼. 앱의 연한 회색 알약 버튼과 같은 색, 더 넘길 곳이 없으면 흰 바탕에 흐린 화살표. */
    private static class ArrowButton extends JButton {
        private static final int SIZE = 48; // 누르는 영역 48px (NFR-02)
        private final boolean forward;

        ArrowButton(boolean forward) {
            this.forward = forward;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(forward ? "다음 배경 보기" : "앞 배경 보기");
            getAccessibleContext().setAccessibleName(forward ? "다음 배경 보기" : "앞 배경 보기");
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SIZE, SIZE);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = TryOnUi.smooth(graphics);
            Ellipse2D circle = new Ellipse2D.Float(1, 1, SIZE - 2, SIZE - 2);
            if (isEnabled()) {
                g.setColor(getModel().isPressed() ? Theme.SECONDARY.darker() : Theme.SECONDARY);
                g.fill(circle);
            } else {
                g.setColor(Theme.CARD);
                g.fill(circle);
                g.setColor(Theme.LINE);
                g.draw(circle);
            }
            float c = SIZE / 2f;
            float d = SIZE * 0.13f;
            Path2D chevron = new Path2D.Float();
            float sign = forward ? 1 : -1;
            chevron.moveTo(c - sign * d * 0.6f, c - d * 1.3f);
            chevron.lineTo(c + sign * d * 0.7f, c);
            chevron.lineTo(c - sign * d * 0.6f, c + d * 1.3f);
            g.setColor(isEnabled() ? Theme.PRIMARY : Theme.LINE);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(chevron);
            if (isFocusOwner()) {
                g.setColor(Theme.PRIMARY);
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Float(3, 3, SIZE - 6, SIZE - 6));
            }
            g.dispose();
        }
    }
}
