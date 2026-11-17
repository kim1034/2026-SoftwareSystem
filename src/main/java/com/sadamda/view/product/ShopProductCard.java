package com.sadamda.view.product;

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
 * 큰 이미지 상품 카드 (FR-05): 사진, 브랜드, 상품명, 가격, [유행]·[품절] 표시.
 * 누르거나 키보드로 고르고 Enter/Space 를 누르면 onOpen 을 부른다.
 */
public class ShopProductCard extends JPanel {
    public static final int IMAGE_WIDTH = 220;
    public static final int IMAGE_HEIGHT = 250;

    private final Product product;
    private final JLabel image = new JLabel();
    private boolean hover;

    public ShopProductCard(Product product, Consumer<Product> onOpen) {
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

    private void loadImage() {
        image.setIcon(ProductThumbnails.get(product.mainImageUrl(), IMAGE_WIDTH, IMAGE_HEIGHT, () ->
                image.setIcon(ProductThumbnails.get(product.mainImageUrl(), IMAGE_WIDTH, IMAGE_HEIGHT, null))));
    }

    private JPanel texts() {
        JPanel texts = new JPanel();
        texts.setLayout(new javax.swing.BoxLayout(texts, javax.swing.BoxLayout.Y_AXIS));
        texts.setOpaque(false);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);
        if (product.isTrending()) {
            badges.add(badge("유행", ShopUi.TREND));
        }
        if (product.isSoldOut()) {
            badges.add(badge("품절", ShopUi.HINT));
        }
        badges.add(ShopUi.hint(product.getCategory() == null ? "" : product.getCategory().label()));
        stack(texts, badges, 0);
        stack(texts, ShopUi.hint(shortBrand(product.getBrand())), 6);
        JLabel name = ShopUi.label("<html><div style='width:" + (IMAGE_WIDTH - 20) + "px'>"
                + ShopUi.escape(product.getName()) + "</div></html>", 1f, Font.PLAIN, ShopUi.TEXT);
        stack(texts, name, 2);
        stack(texts, ShopUi.label(ShopUi.price(product.getPrice()), 1.15f, Font.BOLD, ShopUi.TEXT), 6);
        return texts;
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
        JLabel badge = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public void updateUI() {
                super.updateUI();
                Font base = javax.swing.UIManager.getFont("Label.font");
                setFont(base.deriveFont(Font.BOLD, base.getSize2D() * 0.85f));
            }
        };
        badge.setForeground(Color.WHITE);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return badge;
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
