package com.sadamda.view.product;

import com.sadamda.view.Theme;
import com.sadamda.model.product.Product;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * 큰 이미지 상품 카드 (FR-05): 사진, 브랜드, 상품명, 가격, 맨 아래에 왼쪽 [유행]·[품절]·분류 칩과 오른쪽 찜 하트.
 * 누르거나 키보드로 고르고 Enter/Space 를 누르면 onOpen 을 부른다. 하트는 누르면 바로 찜한다 (UC-10 1a).
 */
public class ShopProductCard extends JPanel {
    public static final int IMAGE_WIDTH = 220;
    public static final int IMAGE_HEIGHT = 250;

    private final Product product;
    private final JLabel image = new JLabel();
    private boolean hover;

    public ShopProductCard(Product product, Consumer<Product> onOpen) {
        this(product, onOpen, null);
    }

    /** wish 가 null 이면 찜 하트를 두지 않는다. */
    ShopProductCard(Product product, Consumer<Product> onOpen, ProductCard.Actions wish) {
        super(new BorderLayout(0, 10));
        this.product = product;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 14, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        getAccessibleContext().setAccessibleName(product.getName() + ", " + ShopUi.price(product.getPrice()));
        setToolTipText(product.getName());

        image.setHorizontalAlignment(SwingConstants.CENTER);
        loadImage();
        add(image, BorderLayout.NORTH);
        add(texts(), BorderLayout.CENTER);
        // 맨 아래 줄: 왼쪽 칩, 오른쪽 하트. 같은 줄의 카드들은 높이가 같아서 상품명 줄 수와 상관없이 가지런하다.
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(badges(), BorderLayout.WEST);
        if (wish != null) {
            bottom.add(heart(wish), BorderLayout.EAST);
        }
        add(bottom, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onOpen.accept(product);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    onOpen.accept(product);
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
                scrollRectToVisible(new java.awt.Rectangle(0, 0, getWidth(), getHeight())); // 키보드로 옮겨도 보이게
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    public Product product() {
        return product;
    }

    /** 카드 오른쪽 맨 아래의 찜 하트. 하트를 누르면 카드를 연 것으로 치지 않는다. */
    private HeartButton heart(ProductCard.Actions wish) {
        HeartButton heart = new HeartButton();
        heart.setPreferredSize(new java.awt.Dimension(HeartButton.SIZE, HeartButton.SIZE));
        Runnable update = () -> {
            boolean wished = wish.isWished(product.getId());
            heart.setSelected(wished);
            heart.getAccessibleContext().setAccessibleName(product.getName() + (wished ? " 찜 해제하기" : " 찜하기"));
            heart.setToolTipText(wished ? "찜 해제하기" : "찜하기");
            heart.repaint();
        };
        heart.addActionListener(e -> {
            wish.toggleWish(product.getId());
            update.run();
        });
        update.run();
        return heart;
    }

    private void loadImage() {
        image.setIcon(ProductThumbnails.get(product.mainImageUrl(), IMAGE_WIDTH, IMAGE_HEIGHT, () ->
                image.setIcon(ProductThumbnails.get(product.mainImageUrl(), IMAGE_WIDTH, IMAGE_HEIGHT, null))));
    }

    /** 브랜드(굵게, 뒤에 ">") → 상품명 → 크고 굵은 가격. */
    private JPanel texts() {
        JPanel texts = new JPanel();
        texts.setLayout(new javax.swing.BoxLayout(texts, javax.swing.BoxLayout.Y_AXIS));
        texts.setOpaque(false);

        String brandName = shortBrand(product.getBrand());
        JLabel brand = ShopUi.label(brandName, 0.95f, Font.BOLD, ShopUi.TEXT);
        if (!brandName.isEmpty()) {
            brand.setIcon(new ChevronIcon(brand.getFont().getSize2D(), ShopUi.TEXT));
            brand.setHorizontalTextPosition(SwingConstants.LEFT);
            brand.setIconTextGap(8);
        }
        stack(texts, brand, 0);
        JLabel name = ShopUi.label("<html><div style='width:" + (IMAGE_WIDTH - 20) + "px'>"
                + ShopUi.escape(product.getName()) + "</div></html>", 1f, Font.PLAIN, ShopUi.TEXT);
        stack(texts, name, 2);
        stack(texts, ShopUi.label(ShopUi.price(product.getPrice()), 1.35f, Font.BOLD, ShopUi.TEXT), 6);
        return texts;
    }

    /** 카드 맨 아래 왼쪽의 작은 표시: [유행]·[품절]·분류. 글자와 같은 왼쪽 선에 맞춘다. */
    private JPanel badges() {
        JPanel badges = new JPanel();
        badges.setLayout(new javax.swing.BoxLayout(badges, javax.swing.BoxLayout.X_AXIS));
        badges.setOpaque(false);
        java.util.List<JLabel> chips = new java.util.ArrayList<>();
        if (product.isTrending()) {
            chips.add(badge("유행", ShopUi.TREND));
        }
        if (product.isSoldOut()) {
            chips.add(badge("품절", ShopUi.HINT));
        }
        if (product.getCategory() != null) {
            chips.add(chip(product.getCategory().label(), Theme.SECONDARY, Theme.HINT, Font.PLAIN));
        }
        for (int i = 0; i < chips.size(); i++) {
            if (i > 0) {
                badges.add(javax.swing.Box.createHorizontalStrut(6));
            }
            badges.add(chips.get(i));
        }
        badges.add(javax.swing.Box.createHorizontalGlue());
        return badges;
    }

    private static void stack(JPanel panel, javax.swing.JComponent component, int gap) {
        if (gap > 0) {
            panel.add(javax.swing.Box.createVerticalStrut(gap));
        }
        component.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(component);
    }

    /** "구호 플러스 (kuho plus)" → "구호 플러스" (카드에서는 짧게 보여준다). */
    static String shortBrand(String brand) {
        if (brand == null) {
            return "";
        }
        String shortened = brand.replaceAll("\\s*\\(.*\\)", "").trim();
        return shortened.isEmpty() ? brand : shortened;
    }

    /** 색 바탕 흰 글씨의 작은 표시. */
    static JLabel badge(String text, Color color) {
        return chip(text, color, Color.WHITE, Font.BOLD);
    }

    /** 바탕색 background, 글자색 foreground 의 작은 표시. */
    private static JLabel chip(String text, Color background, Color foreground, int style) {
        JLabel chip = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(background);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public void updateUI() {
                super.updateUI();
                Font base = javax.swing.UIManager.getFont("Label.font");
                setFont(base.deriveFont(style, base.getSize2D() * 0.85f));
            }
        };
        chip.setForeground(foreground);
        chip.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return chip;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
        g2.setColor(ShopUi.CARD);
        g2.fill(shape);
        boolean highlighted = hover || isFocusOwner();
        g2.setColor(highlighted ? ShopUi.PRIMARY : ShopUi.LINE);
        g2.setStroke(new BasicStroke(highlighted ? 2.5f : 1f));
        g2.draw(shape);
        g2.dispose();
    }
}
