package com.sadamda.view.tryon;

import com.sadamda.model.tryon.Background;
import com.sadamda.view.ProductImages;
import com.sadamda.view.Theme;

import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * 배경 고르기 버튼: 실제 배경 사진을 꽉 채우고, 아래쪽을 어둡게 해 그 위에 이름을 흰 글씨로 쓴다.
 * 윗줄은 작은 글씨로 "봄 · 일본", 아랫줄은 굵게 짧은 장소 이름. 고른 배경은 굵은 검정 테두리와 ✓.
 * background 가 null 이면 "원래 배경" 버튼이고, 올린 사진을 보여준다.
 */
class BackgroundTile extends JButton {
    private static final int ARC = 16;
    private final Background background;
    private BufferedImage photo;
    private boolean chosen;

    BackgroundTile(Background background) {
        this.background = background;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(120, 112));
        setMinimumSize(new Dimension(80, 96));
        String name = background == null ? "원래 배경 그대로 (올린 사진의 배경)" : background.name();
        setToolTipText(background == null ? name : name + " · 출처: Pinterest");
        getAccessibleContext().setAccessibleName(name);
    }

    @Override
    public void updateUI() {
        setUI(new BasicButtonUI());
        setFont(UIManager.getFont("Label.font"));
    }

    Background background() {
        return background;
    }

    void setChosen(boolean chosen) {
        this.chosen = chosen;
        repaint();
    }

    /** "원래 배경" 버튼에 보여줄 올린 사진. */
    void setPhoto(BufferedImage photo) {
        this.photo = photo;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = TryOnUi.smooth(graphics);
        int w = getWidth();
        int h = getHeight();
        int inset = 4; // 고른 테두리 자리
        RoundRectangle2D shape = new RoundRectangle2D.Float(inset, inset, w - inset * 2, h - inset * 2, ARC, ARC);

        BufferedImage image = image();
        Graphics2D clipped = (Graphics2D) g.create();
        clipped.clip(shape);
        clipped.setColor(Theme.SECONDARY);
        clipped.fill(shape);
        if (image != null) {
            TryOnUi.drawCover(clipped, image, inset, inset, w - inset * 2, h - inset * 2, background == null ? 0.12 : 0.5);
        }
        boolean dark = image != null;
        if (dark) {
            int top = inset + (h - inset * 2) / 4;
            clipped.setPaint(new GradientPaint(0, top, new Color(0, 0, 0, 0), 0, h - inset, new Color(0, 0, 0, 225)));
            clipped.fillRect(inset, top, w - inset * 2, h - inset - top);
        }
        clipped.dispose();

        // 글씨: 윗줄 작게(계절 · 나라), 아랫줄 굵게(장소)
        String caption = background == null ? "사진 그대로" : background.caption();
        String title = background == null ? "원래 배경" : background.shortName();
        Font base = getFont();
        Font small = base.deriveFont(Font.PLAIN, base.getSize2D() * 0.78f);
        Font bold = base.deriveFont(Font.BOLD, base.getSize2D() * 0.9f);
        int textWidth = w - inset * 2 - 12;
        FontMetrics boldMetrics = g.getFontMetrics(bold);
        FontMetrics smallMetrics = g.getFontMetrics(small);
        // 장소 이름은 버튼이 좁으면 두 줄로 나눠 끝까지 보이게 한다
        java.util.List<String> titleLines = TryOnUi.wrap(title, boldMetrics, textWidth, 2);
        int lineHeight = boldMetrics.getAscent() + 1;
        int titleBase = h - inset - 7 - boldMetrics.getDescent() - lineHeight * (titleLines.size() - 1);
        int captionBase = titleBase - boldMetrics.getAscent() - 2;
        g.setColor(dark ? Color.WHITE : Theme.TEXT);
        g.setFont(small);
        g.drawString(TryOnUi.ellipsize(caption, smallMetrics, textWidth), inset + 6, captionBase);
        g.setFont(bold);
        for (String line : titleLines) {
            g.drawString(line, inset + 6, titleBase);
            titleBase += lineHeight;
        }

        if (chosen) {
            g.setColor(Theme.PRIMARY);
            g.setStroke(new BasicStroke(4f));
            g.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, ARC + 4, ARC + 4));
            float badge = Math.max(22f, base.getSize2D() * 1.4f);
            float bx = w - inset - badge - 5;
            float by = inset + 5;
            g.setColor(Theme.PRIMARY);
            g.fill(new Ellipse2D.Float(bx, by, badge, badge));
            TryOnUi.drawCheck(g, bx + badge * 0.18f, by + badge * 0.16f, badge * 0.64f, Color.WHITE, Math.max(2f, badge / 9f));
        } else if (isFocusOwner()) {
            g.setColor(Theme.PRIMARY);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 4f}, 0f));
            g.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, ARC + 4, ARC + 4));
        }
        if (getModel().isPressed()) {
            g.setColor(new Color(0, 0, 0, 40));
            g.fill(shape);
        }
        g.dispose();
    }

    /** 보여줄 사진. 배경 사진은 아직 없으면 내려받기를 시작하고, 받으면 다시 그린다. */
    private BufferedImage image() {
        if (background == null) {
            return photo;
        }
        BufferedImage image = ProductImages.get(background.imageUrl());
        if (image == null && !ProductImages.failed(background.imageUrl())) {
            ProductImages.request(background.imageUrl(), this::repaint);
        }
        return image;
    }
}
