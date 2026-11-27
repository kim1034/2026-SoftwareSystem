package com.sadamda.view;

import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 상단의 [글씨 크기] 손잡이 (UC-15). 알약 모양 울타리 안에서 손잡이를 좌우로 당긴다.
 * - 오른쪽으로 당기고 있으면 본문 글씨가 1px 씩 커지고, 왼쪽이면 1px 씩 작아진다 (STEP_MS 마다 한 번, 너무 빠르지 않게).
 * - 놓으면 손잡이가 가운데로 부드럽게 돌아온다.
 * - 당기지 않고 누르기만 하면 보통·크게·아주 크게 고르기 창을 연다. 울타리의 −/+ 쪽을 누르면 1px 씩 바뀐다.
 * 글씨 크기와 상관없이 같은 크기로 그린다 (상단 메뉴와 같은 규칙).
 */
public final class FontSizeSlider extends javax.swing.JPanel {
    private static final String LABEL = "글씨 크기";
    /** 손잡이가 가운데에서 좌우로 움직일 수 있는 거리(px). */
    private static final int TRAVEL = 30;
    /** 이만큼(TRAVEL 대비) 당기면 크기가 바뀌기 시작한다. */
    private static final float TRIGGER = 0.55f;
    /** 당기고 있는 동안 1px 씩 바꾸는 간격(ms). */
    private static final int STEP_MS = 400;
    private static final float FONT_SIZE = FontSize.NORMAL.px() - 2;

    private final IntConsumer onStep;
    private final IntSupplier currentPx;
    private final Runnable onClick;
    private final Runnable onChanged;

    private float offset;
    private boolean dragging;
    private boolean moved;
    private boolean changed;
    private float grabX;
    private int direction;
    private final Timer repeat = new Timer(STEP_MS, e -> step());
    private final Timer spring = new Timer(15, e -> springBack());

    /**
     * @param onStep    +1(크게) 또는 -1(작게)
     * @param currentPx 지금 본문 글씨 크기 (당기는 동안 손잡이에 보여 준다)
     * @param onClick   당기지 않고 눌렀을 때
     * @param onChanged 당기거나 −/+ 를 눌러 크기를 바꾼 뒤 놓았을 때 (저장)
     */
    public FontSizeSlider(IntConsumer onStep, IntSupplier currentPx, Runnable onClick, Runnable onChanged) {
        this.onStep = onStep;
        this.currentPx = currentPx;
        this.onClick = onClick;
        this.onChanged = onChanged;
        setLayout(null);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("오른쪽으로 당기면 크게, 왼쪽으로 당기면 작게");
        getAccessibleContext().setAccessibleName("글씨 크기");
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                spring.stop();
                if (knobBounds().contains(e.getPoint())) {
                    dragging = true;
                    moved = false;
                    changed = false;
                    grabX = e.getX() - offset;
                } else if (e.getX() < getWidth() / 2) { // 울타리의 − 쪽
                    onStep.accept(-1);
                    onChanged.run();
                } else { // + 쪽
                    onStep.accept(1);
                    onChanged.run();
                }
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging) {
                    return;
                }
                offset = Math.max(-TRAVEL, Math.min(TRAVEL, e.getX() - grabX));
                if (Math.abs(offset) > 3) {
                    moved = true;
                }
                int pulled = offset >= TRAVEL * TRIGGER ? 1 : offset <= -TRAVEL * TRIGGER ? -1 : 0;
                if (pulled != direction) {
                    direction = pulled;
                    repeat.stop();
                    if (pulled != 0) {
                        step(); // 당기자마자 한 번, 그 뒤로는 STEP_MS 마다
                        repeat.setInitialDelay(STEP_MS);
                        repeat.start();
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dragging) {
                    return;
                }
                dragging = false;
                direction = 0;
                repeat.stop();
                spring.start();
                if (!moved) {
                    onClick.run();
                } else if (changed) {
                    onChanged.run();
                }
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void step() {
        onStep.accept(direction);
        changed = true;
        repaint();
    }

    /** 놓으면 가운데로 돌아온다. 남은 거리의 일부씩 줄여 부드럽게 멈춘다. */
    private void springBack() {
        offset *= 0.6f;
        if (Math.abs(offset) < 0.5f) {
            offset = 0;
            spring.stop();
        }
        repaint();
    }

    private Font font() {
        return UIManager.getFont("Button.font").deriveFont(Font.BOLD, FONT_SIZE);
    }

    private Dimension knobSize() {
        FontMetrics fm = getFontMetrics(font());
        return new Dimension(fm.stringWidth(LABEL) + 36, fm.getHeight() + 16);
    }

    private RoundRectangle2D knobBounds() {
        Dimension knob = knobSize();
        float x = (getWidth() - knob.width) / 2f + offset;
        float y = (getHeight() - knob.height) / 2f;
        return new RoundRectangle2D.Float(x, y, knob.width, knob.height, knob.height, knob.height);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension knob = knobSize();
        return new Dimension(knob.width + TRAVEL * 2 + 10, knob.height + 8);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        int w = getWidth();
        int h = getHeight();
        // 울타리
        RoundRectangle2D track = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, h - 3, h - 3);
        g.setColor(Theme.SOFT);
        g.fill(track);
        g.setColor(Theme.LINE);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(track);
        // 양 끝의 − / + 표시
        g.setColor(Theme.HINT);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float cy = h / 2f;
        float minusX = 5 + TRAVEL / 2f;
        float plusX = w - 5 - TRAVEL / 2f;
        g.draw(new Line2D.Float(minusX - 5, cy, minusX + 5, cy));
        g.draw(new Line2D.Float(plusX - 5, cy, plusX + 5, cy));
        g.draw(new Line2D.Float(plusX, cy - 5, plusX, cy + 5));
        // 손잡이
        RoundRectangle2D knob = knobBounds();
        g.setColor(Theme.CARD);
        g.fill(knob);
        g.setColor(dragging ? Theme.GOLD_BORDER : Theme.PRIMARY);
        g.setStroke(new BasicStroke(dragging ? 2f : 1.4f));
        g.draw(new RoundRectangle2D.Double(knob.getX() + 0.5, knob.getY() + 0.5, knob.getWidth() - 1,
                knob.getHeight() - 1, knob.getHeight() - 1, knob.getHeight() - 1));
        // 당기는 동안은 지금 크기를 보여 준다
        String text = dragging && moved ? currentPx.getAsInt() + "px" : LABEL;
        g.setFont(font());
        g.setColor(Theme.TEXT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (knob.getCenterX() - fm.stringWidth(text) / 2.0),
                (float) (knob.getY() + (knob.getHeight() - fm.getHeight()) / 2 + fm.getAscent()));
        g.dispose();
    }
}
