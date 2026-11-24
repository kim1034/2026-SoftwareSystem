package com.sadamda.view.product;

import com.sadamda.view.ProductImages;
import com.sadamda.view.Theme;

import com.sadamda.model.review.Review;

import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.Color;
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
 * 상품 카드 한 장: 연한 회색 칸의 사진(오른쪽 아래 찜 하트), 브랜드(굵게, 뒤에 ">"), 상품명(두 줄까지),
 * 크고 굵은 가격, 회색 "♥ 찜 수 ★ 별 평균 (별 준 사람 수)" (UC-04 2단계).
 * 카드 전체가 버튼이라 마우스로 누르거나 Tab 으로 와서 Space 로 누르면 상세(UC-06)로 간다.
 * 하트는 따로 된 버튼이라 누르면 상세로 가지 않고 바로 찜한다 (UC-10 1a).
 */
class ProductCard extends JButton {
    private static final float PHOTO_RATIO = 4f / 3; // 세로 ÷ 가로. 쇼핑몰 사진이 3:4
    private static final int PHOTO_ARC = 16;
    private static final int GAP_BELOW_PHOTO = 14;
    private static final int GAP_BELOW_BRAND = 2;
    private static final int GAP_BELOW_NAME = 6;
    private static final int GAP_BELOW_PRICE = 8;

    /** 카드의 찜 하트와 찜·리뷰 줄에 필요한 것. 상품 목록 화면이 context 로 채운다. */
    interface Actions {
        boolean isWished(Long productId);

        int wishCount(Long productId);

        /** 찜을 바꾼다. 비회원이면 로그인 화면으로 보내고 아무것도 바꾸지 않는다. */
        void toggleWish(Long productId);

        List<Review> reviews(Long productId);
    }

    private final ProductItem item;
    private final Actions actions;
    private final JButton heart;

    ProductCard(ProductItem item, Actions actions, Runnable onOpen) {
        this.item = item;
        this.actions = actions;
        String price = price(item.price());
        getAccessibleContext().setAccessibleName((item.brand().isEmpty() ? "" : item.brand() + " ") + item.name() + ", " + price);
        setToolTipText(item.name()); // 두 줄을 넘겨 잘린 이름도 마우스를 올리면 다 보인다
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addActionListener(e -> onOpen.run());
        ProductImages.request(item.imageUrl(), this::repaint);

        setLayout(null);
        heart = new HeartButton();
        heart.addActionListener(e -> {
            actions.toggleWish(item.id());
            updateHeart();
            repaint(); // 찜 수 다시 그리기
        });
        add(heart);
        updateHeart();
    }

    @Override
    public void doLayout() {
        int photoHeight = photoHeight(getWidth());
        heart.setBounds(getWidth() - HeartButton.SIZE - 4, photoHeight - HeartButton.SIZE - 4, HeartButton.SIZE, HeartButton.SIZE);
    }

    private void updateHeart() {
        boolean wished = actions.isWished(item.id());
        heart.setSelected(wished);
        heart.getAccessibleContext().setAccessibleName(item.name() + (wished ? " 찜 해제하기" : " 찜하기"));
        heart.setToolTipText(wished ? "찜 해제하기" : "찜하기");
        heart.repaint();
    }

    @Override
    public void updateUI() {
        setUI(new BasicButtonUI());
    }

    /** 폭이 width 일 때 카드 높이. 브랜드는 한 줄, 상품명은 늘 두 줄 자리를 잡아 카드 높이를 맞춘다. */
    int heightFor(int width) {
        return photoHeight(width) + GAP_BELOW_PHOTO + getFontMetrics(brandFont()).getHeight() + GAP_BELOW_BRAND
                + getFontMetrics(nameFont()).getHeight() * 2 + GAP_BELOW_NAME
                + getFontMetrics(priceFont()).getHeight() + GAP_BELOW_PRICE + getFontMetrics(statsFont()).getHeight();
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

        // 브랜드: 굵게 + 뒤에 ">"
        int y = photoHeight + GAP_BELOW_PHOTO;
        g2.setColor(Theme.TEXT);
        g2.setFont(brandFont());
        FontMetrics brandMetrics = g2.getFontMetrics();
        if (!item.brand().isEmpty()) {
            String brand = wrap(item.brand(), brandMetrics, width - 24, 1).get(0);
            g2.drawString(brand, 0, y + brandMetrics.getAscent());
            ChevronIcon chevron = new ChevronIcon(brandFont().getSize2D(), Theme.TEXT);
            chevron.paintIcon(this, g2, brandMetrics.stringWidth(brand) + 8,
                    y + (brandMetrics.getHeight() - chevron.getIconHeight()) / 2);
        }
        y += brandMetrics.getHeight() + GAP_BELOW_BRAND;

        // 상품명: 보통 굵기, 두 줄까지
        g2.setFont(nameFont());
        FontMetrics nameMetrics = g2.getFontMetrics();
        for (String line : wrap(item.name(), nameMetrics, width, 2)) {
            g2.drawString(line, 0, y + nameMetrics.getAscent());
            y += nameMetrics.getHeight();
        }
        y = photoHeight + GAP_BELOW_PHOTO + brandMetrics.getHeight() + GAP_BELOW_BRAND + nameMetrics.getHeight() * 2
                + GAP_BELOW_NAME;

        // 가격: 크고 굵게
        g2.setFont(priceFont());
        g2.drawString(price(item.price()), 0, y + g2.getFontMetrics().getAscent());

        // 찜·별점: 작은 회색 글씨
        y += g2.getFontMetrics().getHeight() + GAP_BELOW_PRICE;
        g2.setColor(Theme.HINT);
        g2.setFont(statsFont());
        g2.drawString(statsText(), 0, y + g2.getFontMetrics().getAscent());

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

    /**
     * "♥ 찜 수   ★ 별 평균 (별 준 사람 수)" 예: "♥ 6.2천   ★ 4.7 (74)", "♥ 477   ★ 5 (41)".
     * 별점은 리뷰를 쓸 때만 줄 수 있어서 별 준 사람 수는 리뷰 수와 같다. 리뷰가 없으면 "★ 0 (0)".
     */
    private String statsText() {
        List<Review> reviews = actions.reviews(item.id());
        double average = reviews.stream().mapToInt(Review::rating).average().orElse(0);
        return "♥ " + shortCount(actions.wishCount(item.id())) + "   ★ " + rating(average)
                + " (" + shortCount(reviews.size()) + ")";
    }

    /** 4.666 → "4.7", 5.0 → "5". */
    private static String rating(double average) {
        String text = String.format(Locale.KOREA, "%.1f", average);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    /** 1234 → "1.2천", 12345 → "1.2만". */
    static String shortCount(int count) {
        if (count >= 10000) {
            return trim(count / 10000.0) + "만";
        }
        if (count >= 1000) {
            return trim(count / 1000.0) + "천";
        }
        return String.valueOf(count);
    }

    private static String trim(double value) {
        String text = String.format(Locale.KOREA, "%.1f", Math.floor(value * 10) / 10);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private static int photoHeight(int width) {
        return Math.round(width * PHOTO_RATIO);
    }

    private static Font nameFont() {
        return UIManager.getFont("Label.font").deriveFont(Font.PLAIN);
    }

    private static Font brandFont() {
        Font base = UIManager.getFont("Label.font");
        return base.deriveFont(Font.BOLD, base.getSize2D() * 0.95f);
    }

    private static Font priceFont() {
        Font base = UIManager.getFont("Label.font");
        return base.deriveFont(Font.BOLD, base.getSize2D() * 1.35f);
    }

    private static Font statsFont() {
        Font base = UIManager.getFont("Label.font");
        return base.deriveFont(Font.PLAIN, base.getSize2D() * 0.85f);
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
