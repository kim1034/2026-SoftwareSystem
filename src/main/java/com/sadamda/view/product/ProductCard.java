package com.sadamda.view.product;

import com.sadamda.view.ProductImages;
import com.sadamda.view.Theme;

import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 상품 카드 한 장: 연한 회색 칸의 사진, 상품명(두 줄까지), 가격 (UC-04 2단계).
 * 카드 전체가 버튼이라 마우스로 누르거나 Tab 으로 와서 Space 로 누르면 상세(UC-06)로 간다.
 */
class ProductCard extends JButton {
    private static final float PHOTO_RATIO = 4f / 3; // 세로 ÷ 가로. 쇼핑몰 사진이 3:4
    private static final int PHOTO_ARC = 16;
    private static final int GAP_BELOW_PHOTO = 12;
    private static final int GAP_BELOW_NAME = 4;

    private final ProductItem item;

    ProductCard(ProductItem item, Runnable onOpen) {
        this.item = item;
        String price = price(item.price());
        getAccessibleContext().setAccessibleName(item.name() + ", " + price);
        setToolTipText(item.name()); // 두 줄을 넘겨 잘린 이름도 마우스를 올리면 다 보인다
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addActionListener(e -> onOpen.run());
        ProductImages.request(item.imageUrl(), this::repaint);
    }

    @Override
    public void updateUI() {
        setUI(new BasicButtonUI());
    }

    /** 폭이 width 일 때 카드 높이. 상품명은 늘 두 줄 자리를 잡아 카드 높이를 맞춘다. */
    int heightFor(int width) {
        FontMetrics name = getFontMetrics(nameFont());
        FontMetrics price = getFontMetrics(priceFont());
        return photoHeight(width) + GAP_BELOW_PHOTO + name.getHeight() * 2 + GAP_BELOW_NAME + price.getHeight();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int width = getWidth();
        int photoHeight = photoHeight(width);
        paintPhoto(g2, width, photoHeight);

        g2.setColor(Theme.TEXT);
        g2.setFont(nameFont());
        FontMetrics nameMetrics = g2.getFontMetrics();
        int y = photoHeight + GAP_BELOW_PHOTO;
        for (String line : wrap(item.name(), nameMetrics, width, 2)) {
            g2.drawString(line, 0, y + nameMetrics.getAscent());
            y += nameMetrics.getHeight();
        }

        y = photoHeight + GAP_BELOW_PHOTO + nameMetrics.getHeight() * 2 + GAP_BELOW_NAME;
        g2.setFont(priceFont());
        g2.drawString(price(item.price()), 0, y + g2.getFontMetrics().getAscent());

        if (isFocusOwner()) { // 키보드로 왔을 때 사진 둘레에 테두리 (포커스 표시)
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, width - 3, photoHeight - 3, PHOTO_ARC, PHOTO_ARC));
        }
        g2.dispose();
    }

    /** 사진을 칸에 꽉 차게(넘치는 쪽은 잘라) 그린다. 아직 없으면 안내 문구. */
    private void paintPhoto(Graphics2D g2, int width, int height) {
        Shape box = new RoundRectangle2D.Float(0, 0, width, height, PHOTO_ARC, PHOTO_ARC);
        g2.setColor(Theme.PAGE);
        g2.fill(box);

        BufferedImage image = ProductImages.get(item.imageUrl());
        if (image != null) {
            double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
            int drawWidth = (int) Math.round(image.getWidth() * scale);
            int drawHeight = (int) Math.round(image.getHeight() * scale);
            Graphics2D clipped = (Graphics2D) g2.create();
            clipped.clip(box);
            clipped.drawImage(image, (width - drawWidth) / 2, (height - drawHeight) / 2, drawWidth, drawHeight, null);
            clipped.dispose();
            return;
        }

        String message = ProductImages.failed(item.imageUrl()) ? "사진 없음" : "사진을 불러오는 중";
        g2.setColor(Theme.HINT);
        g2.setFont(nameFont());
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(message, (width - metrics.stringWidth(message)) / 2,
                (height - metrics.getHeight()) / 2 + metrics.getAscent());
    }

    private static int photoHeight(int width) {
        return Math.round(width * PHOTO_RATIO);
    }

    private static Font nameFont() {
        return UIManager.getFont("Label.font");
    }

    private static Font priceFont() {
        Font base = UIManager.getFont("Label.font");
        return base.deriveFont(Font.BOLD, base.getSize2D() * 1.1f);
    }

    private static String price(int won) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(won) + "원";
    }

    /** 띄어쓰기 기준으로 줄을 나눈다. maxLines 를 넘으면 마지막 줄 끝을 "…"로 줄인다. */
    static List<String> wrap(String text, FontMetrics metrics, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= width || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        lines.add(line.toString());

        if (lines.size() > maxLines) {
            List<String> kept = new ArrayList<>(lines.subList(0, maxLines));
            kept.set(maxLines - 1, kept.get(maxLines - 1) + " " + String.join(" ", lines.subList(maxLines, lines.size())));
            lines = kept;
        }
        // 한 단어가 칸보다 길거나 마지막 줄이 넘치면 글자 단위로 줄이고 "…"를 붙인다.
        for (int i = 0; i < lines.size(); i++) {
            String current = lines.get(i);
            if (metrics.stringWidth(current) > width) {
                while (!current.isEmpty() && metrics.stringWidth(current + "…") > width) {
                    current = current.substring(0, current.length() - 1);
                }
                lines.set(i, current.stripTrailing() + "…");
            }
        }
        return lines;
    }
}
