package com.sadamda.view.recommend;

import com.sadamda.model.product.Product;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.view.product.ProductImages;
import com.sadamda.view.product.ShopUi;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

/**
 * 룩북 코디 카드 (UC-07 2단계): 코디 사진, 제목, 상황·계절, 구성 상품 수와 합계 가격.
 * 관리자가 따로 넣은 대표 사진이 있으면 그 사진을, 없으면 구성 상품 사진 4장을 모아 보여준다.
 */
class LookbookCard extends JPanel {
    static final int SIDE = 280;

    private boolean hover;

    LookbookCard(Lookbook lookbook, List<Product> products, Consumer<Lookbook> onOpen) {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 16, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        getAccessibleContext().setAccessibleName("코디 " + lookbook.getTitle());

        add(picture(lookbook, products, SIDE), BorderLayout.NORTH);

        JPanel texts = new JPanel(new GridLayout(0, 1, 0, 4));
        texts.setOpaque(false);
        texts.add(tags(lookbook));
        texts.add(ShopUi.label("<html><div style='width:" + (SIDE - 10) + "px'>" + ShopUi.escape(lookbook.getTitle())
                + "</div></html>", 1.1f, Font.BOLD, ShopUi.TEXT));
        int total = products.stream().mapToInt(Product::getPrice).sum();
        texts.add(ShopUi.hint("구성 상품 " + products.size() + "개 · 합계 " + ShopUi.price(total)));
        add(texts, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onOpen.accept(lookbook);
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
                    onOpen.accept(lookbook);
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
                scrollRectToVisible(new Rectangle(0, 0, getWidth(), getHeight()));
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    /** 상황·계절 표시 줄. */
    static JComponent tags(Lookbook lookbook) {
        JPanel row = ShopUi.row(6);
        if (lookbook.getOccasion() != null) {
            row.add(tag(lookbook.getOccasion()));
        }
        if (lookbook.getSeason() != null) {
            row.add(tag(lookbook.getSeason()));
        }
        return row;
    }

    private static JLabel tag(String text) {
        JLabel tag = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ShopUi.PRIMARY_SOFT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
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
        tag.setForeground(ShopUi.PRIMARY);
        tag.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        return tag;
    }

    /**
     * 코디 사진. side × side 정사각형.
     * 대표 사진이 구성 상품 사진과 다르면(관리자가 따로 넣은 착용 사진) 그 한 장을, 아니면 상품 사진 최대 4장을 모은다.
     */
    static JComponent picture(Lookbook lookbook, List<Product> products, int side) {
        String cover = lookbook.getImageUrl();
        boolean customCover = cover != null && !cover.isBlank()
                && products.stream().noneMatch(product -> cover.equals(product.mainImageUrl()));
        if (customCover || products.size() < 2) {
            String url = customCover ? cover : products.isEmpty() ? null : products.get(0).mainImageUrl();
            return image(url, side, side);
        }
        JPanel collage = new JPanel(new GridLayout(2, 2, 4, 4));
        collage.setOpaque(false);
        int half = (side - 4) / 2;
        for (int i = 0; i < 4; i++) {
            collage.add(i < products.size() ? image(products.get(i).mainImageUrl(), half, half) : blank());
        }
        return collage;
    }

    private static JLabel image(String url, int width, int height) {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setIcon(ProductImages.get(url, width, height, () ->
                label.setIcon(ProductImages.get(url, width, height, null))));
        return label;
    }

    private static JLabel blank() {
        JLabel blank = new JLabel();
        blank.setOpaque(true);
        blank.setBackground(ShopUi.PAGE);
        return blank;
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
