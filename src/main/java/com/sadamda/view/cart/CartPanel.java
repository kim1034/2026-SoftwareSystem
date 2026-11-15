package com.sadamda.view.cart;

import com.sadamda.common.AppException;
import com.sadamda.common.LoginRequiredException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.cart.CartItem;
import com.sadamda.model.member.Member;
import com.sadamda.model.product.ProductOption;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ProductImages;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.WrapLayout;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.order.OrderPanel;
import com.sadamda.view.product.MockProducts;
import com.sadamda.view.product.ProductDetail;
import com.sadamda.view.product.ProductListPanel;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * UC-10 장바구니: 담긴 상품(사진·옵션·수량·가격)과 총액, 수량·옵션 변경, 삭제, [주문하기].
 * 로그인이 필요하다. 찜 목록(1a)은 아직.
 */
public class CartPanel extends ScreenPanel {
    public static final String NAME = "cart";

    /** 가운데 세로 한 줄. 카드와 요약이 여기에 쌓인다. */
    private final JPanel column = new JPanel(new GridBagLayout());
    private final JButton orderButton = Theme.primaryButton("주문하기");
    private final JPanel orderBar = new JPanel(new GridBagLayout());
    /** 장바구니 줄 번호 → 그 줄의 안내 문구(재고 초과 등). 다시 그려도 문구를 남긴다. */
    private final Map<Long, String> messages = new LinkedHashMap<>();

    public CartPanel(AppContext context, MainFrame frame) {
        super(context, frame, "장바구니", "UC-10 장바구니·찜 관리");
        hideTitle();
        setBackground(Theme.PAGE);
        setBorder(BorderFactory.createEmptyBorder());

        column.setOpaque(false);
        JScrollPane page = Theme.centeredPage(column);
        page.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 아래에 늘 붙어 있는 주문 버튼 (UC-10 5단계)
        orderButton.addActionListener(e -> order());
        orderBar.setBackground(Theme.CARD);
        orderBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)));
        orderBar.add(orderButton);

        JPanel screen = new JPanel(new BorderLayout());
        screen.setOpaque(false);
        screen.add(page, BorderLayout.CENTER);
        screen.add(orderBar, BorderLayout.SOUTH);
        setContent(screen);

        // 화면을 열 때마다 장바구니를 다시 불러온다. 비회원이면 로그인 후 이 화면으로 돌아온다.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                messages.clear();
                refresh();
            }
        });
        // [글씨 크기]를 바꾸면 줄 폭이 바뀌므로 다시 그린다.
        column.addPropertyChangeListener("font", e -> SwingUtilities.invokeLater(() -> {
            if (isShowing()) {
                refresh();
            }
        }));
    }

    /** UC-10 2단계, 4단계: 담긴 상품과 총액을 새로 그린다. */
    private void refresh() {
        Member member = context.member.currentMember();
        if (member == null) {
            frame.navigate(LoginPanel.NAME);
            return;
        }
        List<CartItem> items = fetchCart(member.getId());
        column.removeAll();
        int width = columnWidth();
        JLabel title = Theme.label("장바구니", 1.6f, Font.BOLD, Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.addToCard(column, title, 0);

        if (items.isEmpty()) {
            Theme.addToCard(column, emptyCard(), 24);
            orderBar.setVisible(false);
        } else {
            int total = 0;
            for (CartItem item : items) {
                ProductDetail product = MockProducts.detail(item.productId());
                total += product.price() * item.quantity();
                Theme.addToCard(column, itemCard(item, product), 16);
            }
            Theme.addToCard(column, summaryCard(total), 24);
            orderButton.setText(price(total) + " · 총 " + items.size() + "건 주문하기");
            orderBar.setVisible(true);
        }
        // 카드 폭을 글씨 크기에 맞춰 정한다. (길이가 다른 상품명 때문에 카드마다 폭이 달라지지 않게)
        column.setPreferredSize(null);
        Dimension size = column.getPreferredSize();
        column.setPreferredSize(new Dimension(width, size.height));
        orderButton.setPreferredSize(null);
        orderButton.setPreferredSize(new Dimension(width, orderButton.getPreferredSize().height));
        column.revalidate();
        column.repaint();
        orderBar.revalidate();
    }

    // TODO CartService 가 구현되면 context.cart.cart() 로 바꾸고, 상품 정보는 context.product.detail(id) 로 가져온다.
    private List<CartItem> fetchCart(Long memberId) {
        return MockCart.cart(memberId);
    }

    /** UC-10 5단계: 장바구니 전체로 주문서를 만든다 (UC-11). */
    private void order() {
        try {
            context.order.prepareFromCart();
        } catch (LoginRequiredException e) {
            frame.navigate(LoginPanel.NAME);
            return;
        } catch (AppException e) {
            JOptionPane.showMessageDialog(this, e.getMessage()); // 예: 품절 상품이 섞여 있음
            return;
        } catch (UnsupportedOperationException e) {
            JOptionPane.showMessageDialog(this, "주문하기는 아직 연결되지 않았습니다.");
            return;
        }
        frame.navigate(OrderPanel.NAME); // TODO 주문서 화면이 생기면 OrderSheet 를 넘긴다.
    }

    // ── 카드 ──

    /** 상품 한 줄: [사진] 상품명·옵션·수량 버튼 | [삭제]·금액 */
    private JPanel itemCard(CartItem item, ProductDetail product) {
        ProductOption option = option(product, item.optionId());
        JPanel card = whiteCard();
        card.setLayout(new BorderLayout(20, 0));
        card.add(new Thumbnail(product.imageUrls().get(0)), BorderLayout.WEST);

        JPanel info = new JPanel(new GridBagLayout());
        info.setOpaque(false);
        JButton remove = Theme.linkButton("삭제");
        remove.getAccessibleContext().setAccessibleName(product.name() + " 삭제");
        remove.addActionListener(e -> remove(item, product));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        info.add(Theme.label(product.name(), 1.05f, Font.BOLD, Theme.TEXT), c);
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        info.add(remove, c);

        c.gridy = 1;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        info.add(Theme.label(option.size() + " / " + option.color(), 1f, Font.PLAIN, Theme.HINT), c);

        c.gridy = 2;
        c.insets = new Insets(16, 0, 0, 0);
        c.anchor = GridBagConstraints.SOUTHWEST;
        info.add(quantityRow(item, product), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.SOUTHEAST;
        info.add(Theme.label(price(product.price() * item.quantity()), 1.2f, Font.BOLD, Theme.TEXT), c);

        String message = messages.get(item.id());
        if (message != null) {
            JLabel error = Theme.errorLabel();
            error.setText(message);
            c.gridy = 3;
            c.gridx = 0;
            c.gridwidth = 2;
            c.insets = new Insets(8, 0, 0, 0);
            c.anchor = GridBagConstraints.WEST;
            info.add(error, c);
        }
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    /** [−] 2 [+]  [옵션 변경] */
    private JPanel quantityRow(CartItem item, ProductDetail product) {
        JButton minus = smallButton("−", "수량 줄이기");
        JButton plus = smallButton("+", "수량 늘리기");
        JLabel count = Theme.label(item.quantity() + "개", 1.1f, Font.BOLD, Theme.TEXT);
        count.setHorizontalAlignment(SwingConstants.CENTER);
        count.setPreferredSize(new Dimension(count.getFontMetrics(count.getFont()).stringWidth("99개") + 24,
                count.getPreferredSize().height));
        minus.addActionListener(e -> {
            if (item.quantity() == 1) {
                messages.put(item.id(), "1개보다 줄일 수 없어요. 빼려면 [삭제]를 눌러 주세요.");
                refresh();
                return;
            }
            change(item, item.optionId(), item.quantity() - 1);
        });
        plus.addActionListener(e -> change(item, item.optionId(), item.quantity() + 1));
        JButton changeOption = Theme.secondaryButton("옵션 변경");
        changeOption.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        changeOption.addActionListener(e -> openOptionDialog(item, product));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(minus);
        row.add(count);
        row.add(plus);
        row.add(javax.swing.Box.createHorizontalStrut(16));
        row.add(changeOption);
        return row;
    }

    /** UC-10 3~4단계: 수량·옵션을 바꾸고 총액을 다시 계산한다. 재고를 넘으면 최대 수량을 안내한다 (3a). */
    private boolean change(CartItem item, Long optionId, int quantity) {
        try {
            // TODO context.cart.update(item.id(), optionId, quantity) 로 바꾼다.
            MockCart.update(item.memberId(), item.id(), optionId, quantity);
            messages.remove(item.id());
            refresh();
            return true;
        } catch (AppException e) {
            messages.put(item.id(), e.getMessage());
            refresh();
            return false;
        }
    }

    private void remove(CartItem item, ProductDetail product) {
        int answer = JOptionPane.showConfirmDialog(this, "'" + product.name() + "'을(를) 장바구니에서 뺄까요?",
                "삭제", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        // TODO context.cart.remove(item.id()) 로 바꾼다.
        MockCart.remove(item.memberId(), item.id());
        messages.remove(item.id());
        refresh();
    }

    /** "선택 주문정보" 같은 금액 요약. */
    private JPanel summaryCard(int total) {
        JPanel card = whiteCard();
        card.setLayout(new GridBagLayout());
        Theme.addToCard(card, Theme.label("주문 정보", 1.2f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(card, line(), 16);
        Theme.addToCard(card, amountRow("총 상품금액", price(total), false), 16);
        Theme.addToCard(card, line(), 16);
        Theme.addToCard(card, amountRow("총 결제 예정금액", price(total), true), 16);
        return card;
    }

    private JPanel emptyCard() {
        JPanel card = whiteCard();
        card.setLayout(new GridBagLayout());
        JLabel title = Theme.label("장바구니가 비어 있어요", 1.2f, Font.BOLD, Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel hint = Theme.label("마음에 드는 상품을 담아 보세요.", 1f, Font.PLAIN, Theme.HINT);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        JButton browse = Theme.primaryButton("상품 보러 가기");
        browse.addActionListener(e -> frame.navigate(ProductListPanel.NAME));
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(browse);
        Theme.addToCard(card, title, 24);
        Theme.addToCard(card, hint, 8);
        Theme.addToCard(card, buttonRow, 24);
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 48, 24));
        return card;
    }

    // ── 옵션 변경 창 ──

    /** 상세 화면과 같은 사이즈·색상 버튼으로 옵션을 바꾼다. 품절은 막는다. */
    private void openOptionDialog(CartItem item, ProductDetail product) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "옵션 변경",
                Dialog.ModalityType.APPLICATION_MODAL);
        ProductOption current = option(product, item.optionId());
        Map<String, JToggleButton> sizes = new LinkedHashMap<>();
        Map<String, JToggleButton> colors = new LinkedHashMap<>();
        ButtonGroup sizeGroup = new ButtonGroup();
        ButtonGroup colorGroup = new ButtonGroup();
        JLabel error = Theme.errorLabel();

        Runnable updateColors = () -> {
            String size = selected(sizes);
            colors.forEach((color, chip) -> {
                boolean available = product.options().stream().anyMatch(o -> o.color().equals(color)
                        && o.stock() > 0 && (size == null || o.size().equals(size)));
                if (!available && chip.isSelected()) {
                    colorGroup.clearSelection();
                }
                chip.setEnabled(available);
                chip.setText(available ? color : color + " 품절");
            });
        };
        product.options().stream().map(ProductOption::size).distinct().forEach(size -> {
            JToggleButton chip = Theme.chip(size);
            boolean available = product.options().stream().anyMatch(o -> o.size().equals(size) && o.stock() > 0);
            chip.setEnabled(available);
            chip.setText(available ? size : size + " 품절");
            chip.addActionListener(e -> updateColors.run());
            sizeGroup.add(chip);
            sizes.put(size, chip);
        });
        product.options().stream().map(ProductOption::color).distinct().forEach(color -> {
            JToggleButton chip = Theme.chip(color);
            colorGroup.add(chip);
            colors.put(color, chip);
        });
        sizes.get(current.size()).setSelected(true);
        updateColors.run();
        if (colors.get(current.color()).isEnabled()) {
            colors.get(current.color()).setSelected(true);
        }

        JButton apply = Theme.primaryButton("변경하기");
        JButton cancel = Theme.secondaryButton("취소");
        cancel.addActionListener(e -> dialog.dispose());
        apply.addActionListener(e -> {
            String size = selected(sizes);
            String color = selected(colors);
            if (size == null || color == null) {
                error.setText(size == null ? "사이즈를 골라 주세요" : "색상을 골라 주세요");
                return;
            }
            ProductOption chosen = product.options().stream()
                    .filter(o -> o.size().equals(size) && o.color().equals(color)).findFirst().orElseThrow();
            if (change(item, chosen.id(), item.quantity())) {
                dialog.dispose();
            } else {
                error.setText(messages.get(item.id())); // 예: 바꾼 옵션의 재고가 부족함
            }
        });

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Theme.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        Theme.addToCard(content, Theme.label(product.name(), 1.1f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(content, Theme.label("사이즈", 1f, Font.BOLD, Theme.TEXT), 24);
        Theme.addToCard(content, chipRow(sizes), 4);
        Theme.addToCard(content, Theme.label("색상", 1f, Font.BOLD, Theme.TEXT), 16);
        Theme.addToCard(content, chipRow(colors), 4);
        Theme.addToCard(content, error, 16);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(apply);
        Theme.addToCard(content, buttons, 16);

        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(apply);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setSize(columnWidth(), 0);
        dialog.pack();
        dialog.setSize(Math.max(dialog.getWidth(), columnWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── 작은 부품 ──

    /** 가운데 줄 폭. 글씨가 커지면 함께 넓어진다. */
    private static int columnWidth() {
        float fontSize = UIManager.getFont("Label.font").getSize2D();
        return Math.max(640, Math.round(fontSize * 40));
    }

    private static JPanel whiteCard() {
        JPanel card = Theme.card();
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return card;
    }

    private static JButton smallButton(String text, String accessibleName) {
        JButton button = Theme.secondaryButton(text);
        button.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18)); // 48px 이상 (NFR-02)
        button.getAccessibleContext().setAccessibleName(accessibleName);
        return button;
    }

    private static JPanel chipRow(Map<String, JToggleButton> chips) {
        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 8));
        row.setOpaque(false);
        chips.values().forEach(row::add);
        return row;
    }

    private static JPanel amountRow(String name, String amount, boolean emphasize) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(Theme.label(name, emphasize ? 1.1f : 1f, emphasize ? Font.BOLD : Font.PLAIN,
                emphasize ? Theme.TEXT : Theme.HINT), BorderLayout.WEST);
        row.add(Theme.label(amount, emphasize ? 1.3f : 1f, emphasize ? Font.BOLD : Font.PLAIN, Theme.TEXT),
                BorderLayout.EAST);
        return row;
    }

    private static JComponent line() {
        JPanel line = new JPanel();
        line.setBackground(Theme.LINE);
        line.setPreferredSize(new Dimension(1, 1));
        return line;
    }

    private static ProductOption option(ProductDetail product, Long optionId) {
        return product.options().stream().filter(o -> o.id().equals(optionId)).findFirst().orElseThrow();
    }

    private static String selected(Map<String, JToggleButton> chips) {
        return chips.entrySet().stream().filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private static String price(int won) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(won) + "원";
    }

    /** 장바구니 줄의 작은 사진 (3:4, 칸에 꽉 차게). */
    private static final class Thumbnail extends JComponent {
        private final String url;

        Thumbnail(String url) {
            this.url = url;
            ProductImages.request(url, this::repaint);
        }

        @Override
        public Dimension getPreferredSize() {
            int width = Math.round(UIManager.getFont("Label.font").getSize2D() * 6);
            return new Dimension(width, Math.round(width * 4f / 3));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            Dimension size = getPreferredSize();
            RoundRectangle2D box = new RoundRectangle2D.Float(0, 0, size.width, size.height, 12, 12);
            g2.setColor(Theme.PAGE);
            g2.fill(box);
            BufferedImage image = ProductImages.get(url);
            if (image != null) {
                double scale = Math.max(size.width / (double) image.getWidth(), size.height / (double) image.getHeight());
                int w = (int) Math.round(image.getWidth() * scale);
                int h = (int) Math.round(image.getHeight() * scale);
                g2.clip(box);
                g2.drawImage(image, (size.width - w) / 2, (size.height - h) / 2, w, h, null);
            } else {
                String text = ProductImages.failed(url) ? "사진 없음" : "…";
                g2.setColor(Theme.HINT);
                g2.setFont(UIManager.getFont("Label.font"));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (size.width - fm.stringWidth(text)) / 2,
                        (size.height - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }
}
