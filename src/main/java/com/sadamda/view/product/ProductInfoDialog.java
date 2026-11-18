package com.sadamda.view.product;

import com.sadamda.controller.AppContext;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.view.MainFrame;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.net.URI;

/**
 * 상품 정보 창 (UC-06 요약): 큰 사진, 이름·가격, 소재·색상, 코디 포인트, 추천 연령대·체형, 옵션 고르기,
 * [장바구니 담기] [찜] [판매처에서 보기].
 * 상품 상세 화면(ProductDetailPanel)이 완성되기 전까지 상품 카드를 누르면 이 창이 열린다 (ShopUi.openProduct).
 */
public class ProductInfoDialog extends JDialog {
    private static final int IMAGE_WIDTH = 380;
    private static final int IMAGE_HEIGHT = 460;

    private final MainFrame frame;
    private final AppContext context;
    private final Product product;
    private final JComboBox<ProductOption> optionBox = new JComboBox<>();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));

    public ProductInfoDialog(MainFrame frame, AppContext context, Product product) {
        super(frame, product.getName(), true);
        this.frame = frame;
        this.context = context;
        this.product = product;

        JPanel root = new JPanel(new BorderLayout(28, 0));
        root.setBackground(ShopUi.CARD);
        root.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        root.add(imagePanel(), BorderLayout.WEST);
        root.add(ShopUi.verticalScroll(infoPanel(), ShopUi.CARD), BorderLayout.CENTER);
        setContentPane(root);

        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(new Dimension(1160, 660));
        setLocationRelativeTo(frame);
    }

    private JComponent imagePanel() {
        JLabel image = new JLabel();
        image.setIcon(ProductThumbnails.get(product.mainImageUrl(), IMAGE_WIDTH, IMAGE_HEIGHT, () ->
                image.setIcon(ProductThumbnails.get(product.mainImageUrl(), IMAGE_WIDTH, IMAGE_HEIGHT, null))));
        image.setVerticalAlignment(JLabel.TOP);
        return image;
    }

    private JComponent infoPanel() {
        JPanel info = new JPanel(new GridBagLayout());
        info.setBackground(ShopUi.CARD);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);
        if (product.isTrending()) {
            badges.add(ShopProductCard.badge("이번 시즌 유행", ShopUi.TREND));
        }
        if (product.isSoldOut()) {
            badges.add(ShopProductCard.badge("품절", ShopUi.HINT));
        }
        String path = product.getCategory().label()
                + (product.getSubCategory() == null ? "" : "  ›  " + product.getSubCategory());
        badges.add(ShopUi.hint(path));
        info.add(badges, c);

        c.insets = new Insets(10, 0, 0, 0);
        info.add(ShopUi.hint(product.getBrand() + (product.getCountry() == null ? "" : " · " + product.getCountry())), c);
        info.add(ShopUi.label(wrap(product.getName(), 460), 1.35f, Font.BOLD, ShopUi.TEXT), c);
        info.add(ShopUi.label(ShopUi.price(product.getPrice()), 1.5f, Font.BOLD, ShopUi.PRIMARY), c);

        c.insets = new Insets(18, 0, 0, 0);
        info.add(field("소재·색상", product.getMaterial()), c);
        c.insets = new Insets(8, 0, 0, 0);
        info.add(field("코디 포인트", product.getDescription()), c);
        info.add(field("추천 연령대", String.join(", ", product.getAgeGroupTags())), c);
        info.add(field("어울리는 체형", String.join(", ", product.getBodyTypeTags())), c);

        c.insets = new Insets(22, 0, 0, 0);
        info.add(optionRow(), c);
        c.insets = new Insets(16, 0, 0, 0);
        info.add(buttonRow(), c);

        c.weighty = 1; // 남는 공간은 아래로
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        info.add(filler, c);
        return info;
    }

    private JComponent optionRow() {
        for (ProductOption option : product.getOptions()) {
            optionBox.addItem(option);
        }
        optionBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ProductOption option) {
                    setText(option.color() + " / " + option.size() + (option.stock() > 0 ? "" : "  (품절)"));
                }
                return this;
            }
        });
        optionBox.getAccessibleContext().setAccessibleName("색상과 사이즈");
        ProductOption firstInStock = product.getOptions().stream().filter(o -> o.stock() > 0).findFirst().orElse(null);
        if (firstInStock != null) {
            optionBox.setSelectedItem(firstInStock);
        }
        optionBox.setPreferredSize(new Dimension(240, ShopUi.MIN_TOUCH));
        quantity.setPreferredSize(new Dimension(90, ShopUi.MIN_TOUCH));
        quantity.getAccessibleContext().setAccessibleName("수량");

        JPanel row = ShopUi.row(10, ShopUi.label("옵션", 1f, Font.BOLD, ShopUi.TEXT), optionBox,
                ShopUi.label("수량", 1f, Font.BOLD, ShopUi.TEXT), quantity);
        return row;
    }

    private JComponent buttonRow() {
        JButton cart = ShopUi.primaryButton("장바구니 담기");
        cart.setEnabled(!product.isSoldOut());
        cart.addActionListener(e -> addToCart());

        JButton wish = ShopUi.secondaryButton("찜하기");
        wish.addActionListener(e -> toggleWish(wish));

        JButton store = ShopUi.secondaryButton("판매처에서 보기");
        store.setEnabled(product.getProductUrl() != null && product.getProductUrl().startsWith("http")
                && Desktop.isDesktopSupported());
        store.addActionListener(e -> openStore());

        JButton close = ShopUi.secondaryButton("닫기");
        close.addActionListener(e -> dispose());
        return ShopUi.row(10, cart, wish, store, close);
    }

    /** UC-06 4단계 [장바구니]. 비회원이면 로그인 화면으로 (4a), 품절 옵션이면 안내 (4b). */
    private void addToCart() {
        ProductOption option = (ProductOption) optionBox.getSelectedItem();
        if (option == null || option.stock() <= 0) {
            JOptionPane.showMessageDialog(this, "품절된 옵션입니다. 다른 색상이나 사이즈를 골라 주세요.");
            return;
        }
        int count = (Integer) quantity.getValue();
        if (count > option.stock()) {
            JOptionPane.showMessageDialog(this, "남은 수량은 " + option.stock() + "개입니다.");
            return;
        }
        try {
            context.cart.add(new OrderLine(product.getId(), option.id(), count));
            JOptionPane.showMessageDialog(this, "장바구니에 담았습니다.");
        } catch (RuntimeException e) {
            ShopUi.showError(this, frame, e);
        }
    }

    /** UC-10 1a [찜] */
    private void toggleWish(JButton wish) {
        try {
            boolean wished = context.cart.toggleWish(product.getId());
            wish.setText(wished ? "찜 해제" : "찜하기");
            JOptionPane.showMessageDialog(this, wished ? "찜 목록에 넣었습니다." : "찜 목록에서 뺐습니다.");
        } catch (RuntimeException e) {
            ShopUi.showError(this, frame, e);
        }
    }

    private void openStore() {
        try {
            Desktop.getDesktop().browse(URI.create(product.getProductUrl()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "판매처 페이지를 열지 못했습니다.");
        }
    }

    private static JComponent field(String name, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        JLabel label = ShopUi.label(name, 1f, Font.BOLD, ShopUi.HINT);
        label.setPreferredSize(new Dimension(130, label.getPreferredSize().height));
        label.setVerticalAlignment(JLabel.TOP);
        row.add(label, BorderLayout.WEST);
        row.add(ShopUi.label(wrap(value == null || value.isBlank() ? "-" : value, 330), 1f, Font.PLAIN, ShopUi.TEXT),
                BorderLayout.CENTER);
        return row;
    }

    private static String wrap(String text, int width) {
        return "<html><div style='width:" + width + "px'>" + ShopUi.escape(text) + "</div></html>";
    }
}
