package com.sadamda.view.order;

import com.sadamda.common.AppException;
import com.sadamda.controller.AppContext;
import com.sadamda.controller.CheckoutRequest;
import com.sadamda.controller.OrderSheet;
import com.sadamda.model.member.Member;
import com.sadamda.model.order.GiftInfo;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.ProductOption;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ProductThumbnail;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.cart.CartPanel;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.member.SignUpPanel;
import com.sadamda.view.product.MockProducts;
import com.sadamda.view.product.ProductDetail;
import com.sadamda.view.product.ProductListPanel;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * UC-11 주문 및 결제 + UC-12 선물하기: 배송 주소(내가 받기 / 선물로 보내기), 주문 상품, 결제 방법,
 * 최종 주문정보, 아래 고정 [결제하기]. 결제가 끝나면 같은 화면이 주문 완료로 바뀐다 (UC-11 8단계).
 * 열 때는 MainFrame.openOrder(sheet) 를 쓴다. 장바구니에서 왔는지는 sheet.fromCart() 가 알려 준다.
 */
public class OrderPanel extends ScreenPanel {
    public static final String NAME = "order";
    /** 배송비 (팀 결정: 3,000원 고정). 장바구니 금액 요약도 이 값을 쓴다. */
    public static final int SHIPPING_FEE = 3000;
    private static final String[] PAYMENT_METHODS = {"카드 결제", "계좌이체", "휴대폰 결제"};

    /** 가운데 세로 한 줄. 폭은 글씨 크기에 맞춰 정하고, 높이는 내용(내가 받기·선물 카드 등)에 따라 늘어난다. */
    private final JPanel column = new JPanel(new GridBagLayout()) {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(columnWidth(), super.getPreferredSize().height);
        }
    };
    private final JScrollPane page;
    private final JButton payButton = Theme.primaryButton("결제하기");
    private final JPanel payBar = new JPanel(new GridBagLayout());

    /** 지금 작성 중인 주문서. 결제가 끝나면 null (같은 주문을 두 번 결제하지 않게). */
    private OrderSheet sheet;
    private MockOrders.MockOrder completed;

    // 배송 주소
    /** 내가 받기 / 선물로 보내기 중 고른 카드만 담는다. (겹쳐 두면 짧은 카드도 긴 카드 높이가 된다) */
    private final JPanel addressArea = new JPanel(new BorderLayout());
    private JPanel selfCard;
    private JPanel giftCard;
    private final JToggleButton selfChip = Theme.chip("내가 받기");
    private final JToggleButton giftChip = Theme.chip("선물로 보내기");
    private final JTextField addressField = Theme.textField("예: 서울시 종로구 세종대로 1, 101동 101호");
    private final JLabel addressError = Theme.errorLabel();
    private final JLabel receiverName = Theme.label(" ", 1f, Font.PLAIN, Theme.TEXT);
    private final JLabel receiverPhone = Theme.label(" ", 1f, Font.PLAIN, Theme.TEXT);
    // 선물하기 (UC-12)
    private final JTextField giftNameField = Theme.textField("받는 분 이름");
    private final JTextField giftPhoneField = Theme.textField("예: 010-1234-5678");
    private final JTextField giftAddressField = Theme.textField("예: 서울시 종로구 세종대로 1, 101동 101호");
    private final JTextField giftMessageField = Theme.textField("예: 생일 축하해요! (선택)");
    private final JLabel giftNameError = Theme.errorLabel();
    private final JLabel giftPhoneError = Theme.errorLabel();
    private final JLabel giftAddressError = Theme.errorLabel();
    // 결제 방법
    private final ButtonGroup paymentGroup = new ButtonGroup();
    private final List<JToggleButton> paymentChips = new ArrayList<>();

    public OrderPanel(AppContext context, MainFrame frame) {
        super(context, frame, "주문서", "UC-11 주문 및 결제, UC-12 선물하기");
        hideTitle();
        setBackground(Theme.PAGE);
        setBorder(BorderFactory.createEmptyBorder());

        column.setOpaque(false);
        page = Theme.centeredPage(column);
        page.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        payButton.addActionListener(e -> pay());
        payBar.setBackground(Theme.CARD);
        payBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)));
        payBar.add(payButton);

        JPanel screen = new JPanel(new BorderLayout());
        screen.setOpaque(false);
        screen.add(page, BorderLayout.CENTER);
        screen.add(payBar, BorderLayout.SOUTH);
        setContent(screen);

        ButtonGroup receiverGroup = new ButtonGroup();
        receiverGroup.add(selfChip);
        receiverGroup.add(giftChip);
        selfChip.addActionListener(e -> showAddressCard(selfCard));
        giftChip.addActionListener(e -> {
            showAddressCard(giftCard);
            giftNameField.requestFocusInWindow();
        });
        for (String method : PAYMENT_METHODS) {
            JToggleButton chip = Theme.chip(method);
            paymentGroup.add(chip);
            paymentChips.add(chip);
        }
        addressArea.setOpaque(false);
        selfCard = selfAddressCard();
        giftCard = giftCard();
        showAddressCard(selfCard);

        // [글씨 크기]를 바꾸면 가운데 줄 폭이 바뀌므로 아래 버튼 폭도 다시 맞춘다.
        column.addPropertyChangeListener("font", e -> SwingUtilities.invokeLater(this::fitPayButton));

        // UC-11 1a: 비회원이면 로그인 화면으로 보내고, 로그인하면 이 주문서로 돌아온다.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (context.member.currentMember() == null) {
                    frame.navigate(LoginPanel.NAME);
                } else if (completed == null) {
                    render();
                }
            }
        });
    }

    /** UC-11 2단계: 주문서를 연다. 장바구니에서 왔으면(sheet.fromCart()) 결제 후 주문한 상품을 장바구니에서 뺀다. */
    public void showSheet(OrderSheet newSheet) {
        sheet = newSheet;
        completed = null;
        Member member = context.member.currentMember();
        selfChip.setSelected(true);
        showAddressCard(selfCard);
        receiverName.setText(member == null ? "" : member.getName());
        receiverPhone.setText(member == null || member.getPhone() == null ? "" : member.getPhone());
        addressField.setText(sheet.defaultAddress() == null ? "" : sheet.defaultAddress());
        for (JTextField field : List.of(giftNameField, giftPhoneField, giftAddressField, giftMessageField)) {
            field.setText("");
        }
        clearErrors();
        paymentChips.get(0).setSelected(true);
        render();
    }

    private void render() {
        column.removeAll();
        JLabel title = Theme.label(completed == null ? "주문서" : "주문 완료", 1.6f, Font.BOLD, Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.addToCard(column, title, 0);
        if (completed != null) {
            renderCompleted();
        } else if (sheet == null) {
            renderEmpty();
        } else {
            renderSheet();
        }
        fitPayButton();
        column.revalidate();
        column.repaint();
        page.getViewport().setViewPosition(new Point(0, 0));
    }

    /** 아래 [결제하기] 버튼을 가운데 줄과 같은 폭으로 맞춘다. */
    private void fitPayButton() {
        payButton.setPreferredSize(null);
        payButton.setPreferredSize(new Dimension(columnWidth(), payButton.getPreferredSize().height));
        payBar.revalidate();
    }

    // ── 주문서 ──

    private void showAddressCard(JPanel card) {
        addressArea.removeAll();
        addressArea.add(card, BorderLayout.CENTER);
        addressArea.revalidate();
        addressArea.repaint();
    }

    private void renderSheet() {
        // 다시 쓰는 부품(주소 카드·선택 버튼)은 화면에서 떨어져 있는 동안 [글씨 크기] 변경을 놓쳤을 수 있어 새로 맞춘다.
        for (JComponent reused : List.of(selfCard, giftCard, selfChip, giftChip)) {
            SwingUtilities.updateComponentTreeUI(reused);
        }
        paymentChips.forEach(SwingUtilities::updateComponentTreeUI);
        int productTotal = sheet.lines().stream()
                .mapToInt(line -> MockProducts.detail(line.productId()).price() * line.quantity()).sum();

        JPanel addressHeader = sectionHeader("배송 주소");
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        chips.setOpaque(false);
        chips.add(selfChip);
        chips.add(giftChip);
        addressHeader.add(chips, BorderLayout.EAST);
        Theme.addToCard(column, addressHeader, 32);
        Theme.addToCard(column, addressArea, 12);

        JPanel itemsHeader = sectionHeader("주문 상품");
        itemsHeader.add(Theme.label("총 " + sheet.lines().size() + "건", 1.1f, Font.BOLD, Theme.TEXT), BorderLayout.EAST);
        Theme.addToCard(column, itemsHeader, 40);
        JPanel itemsCard = whiteCard();
        boolean first = true;
        for (OrderLine line : sheet.lines()) {
            Theme.addToCard(itemsCard, itemRow(line), first ? 0 : 20);
            first = false;
        }
        Theme.addToCard(column, itemsCard, 12);

        Theme.addToCard(column, sectionHeader("결제 방법"), 40);
        JPanel paymentCard = whiteCard();
        JPanel paymentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        paymentRow.setOpaque(false);
        paymentChips.forEach(paymentRow::add);
        Theme.addToCard(paymentCard, paymentRow, 0);
        Theme.addToCard(column, paymentCard, 12);

        Theme.addToCard(column, sectionHeader("최종 주문정보"), 40);
        JPanel summary = whiteCard();
        Theme.addToCard(summary, amountRow("상품 금액", price(productTotal), false), 0);
        Theme.addToCard(summary, amountRow("배송비", price(SHIPPING_FEE), false), 12);
        Theme.addToCard(summary, line(), 16);
        Theme.addToCard(summary, amountRow("총 결제금액", price(productTotal + SHIPPING_FEE), true), 16);
        Theme.addToCard(column, summary, 12);

        payButton.setText(price(productTotal + SHIPPING_FEE) + " · 결제하기");
        payBar.setVisible(true);
    }

    /** 내가 받기: 받는 분·연락처는 회원 정보, 주소는 기본 배송지를 채워 두고 바로 고칠 수 있게 한다 (3단계). */
    private JPanel selfAddressCard() {
        JPanel card = whiteCard();
        JPanel table = new JPanel(new GridBagLayout());
        table.setOpaque(false);
        infoRow(table, 0, "받는 분", receiverName);
        infoRow(table, 1, "연락처", receiverPhone);
        Theme.addToCard(card, table, 0);
        Theme.addToCard(card, Theme.field("배송 주소", addressError, addressField), 20);
        return card;
    }

    /** UC-12 2단계: 받는 분 이름·연락처·주소와 선물 메시지. */
    private JPanel giftCard() {
        JPanel card = whiteCard();
        Theme.addToCard(card, Theme.label("선물 받을 분의 정보를 입력해 주세요", 1f, Font.PLAIN, Theme.HINT), 0);
        Theme.addToCard(card, Theme.field("받는 분 이름", giftNameError, giftNameField), 16);
        Theme.addToCard(card, Theme.field("받는 분 연락처", giftPhoneError, giftPhoneField), 16);
        Theme.addToCard(card, Theme.field("받는 분 주소", giftAddressError, giftAddressField), 16);
        Theme.addToCard(card, Theme.field("선물 메시지 (선택)", null, giftMessageField), 16);
        return card;
    }

    /** [사진] 상품명 / 옵션 · 수량 ... 금액 */
    private JPanel itemRow(OrderLine line) {
        ProductDetail product = MockProducts.detail(line.productId());
        ProductOption option = product.options().stream()
                .filter(o -> o.id().equals(line.optionId())).findFirst().orElseThrow();
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.add(new ProductThumbnail(product.imageUrls().get(0)), BorderLayout.WEST);
        JPanel text = new JPanel(new GridBagLayout());
        text.setOpaque(false);
        Theme.addToCard(text, Theme.label(product.name(), 1f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(text, Theme.label(option.size() + " / " + option.color() + " · " + line.quantity() + "개",
                1f, Font.PLAIN, Theme.HINT), 6);
        row.add(text, BorderLayout.CENTER);
        row.add(Theme.label(price(product.price() * line.quantity()), 1.1f, Font.BOLD, Theme.TEXT), BorderLayout.EAST);
        return row;
    }

    private void renderEmpty() {
        JPanel card = whiteCard();
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 48, 24));
        JLabel message = Theme.label("주문할 상품이 없어요", 1.2f, Font.BOLD, Theme.TEXT);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        JButton toCart = Theme.primaryButton("장바구니로 가기");
        toCart.addActionListener(e -> frame.navigate(CartPanel.NAME));
        Theme.addToCard(card, message, 24);
        Theme.addToCard(card, centered(toCart), 24);
        Theme.addToCard(column, card, 24);
        payBar.setVisible(false);
    }

    // ── 결제 (UC-11 4~8단계, UC-12 4단계) ──

    private void pay() {
        if (sheet == null) {
            return;
        }
        GiftInfo gift = giftChip.isSelected() ? checkGift() : null;
        boolean valid = giftChip.isSelected() ? gift != null : checkAddress();
        if (!valid) {
            return;
        }
        String address = gift == null ? addressField.getText().trim() : gift.recipientAddress();
        String method = paymentChips.stream().filter(JToggleButton::isSelected)
                .map(JToggleButton::getText).findFirst().orElse(PAYMENT_METHODS[0]);
        CheckoutRequest request = new CheckoutRequest(sheet.lines(), address, method, gift, sheet.fromCart());
        Long memberId = context.member.currentMember().getId();

        // 결제 시스템 응답을 기다리므로 화면 스레드가 아닌 곳에서 부른다 (docs/controller-spec.md).
        // 기다리는 동안 버튼을 막아 두 번 결제하지 않게 하고, 무엇을 하는지 알려 준다.
        String payText = payButton.getText();
        payButton.setEnabled(false);
        payButton.setText("결제 중…");
        new SwingWorker<MockOrders.MockOrder, Void>() {
            @Override
            protected MockOrders.MockOrder doInBackground() {
                return fetchCheckout(memberId, request);
            }

            @Override
            protected void done() {
                payButton.setEnabled(true);
                payButton.setText(payText);
                try {
                    completed = get();
                } catch (ExecutionException e) {
                    // 6a 결제 실패, 7a 재고 소진 등: 주문을 만들지 않고 이유를 알린 뒤 주문서에 남는다.
                    String message = e.getCause() instanceof AppException
                            ? e.getCause().getMessage()
                            : "결제 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.";
                    JOptionPane.showMessageDialog(OrderPanel.this, message, "결제하지 못했어요",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                sheet = null;
                render();
            }
        }.execute();
    }

    // TODO OrderService·CartService 가 구현되면 context.order.checkout(request) 로 바꾼다.
    //  (OrderController.checkout 은 구현됐지만 안에서 부르는 Model 이 아직 TODO)
    private static MockOrders.MockOrder fetchCheckout(Long memberId, CheckoutRequest request) {
        return MockOrders.checkout(memberId, request, SHIPPING_FEE);
    }

    private boolean checkAddress() {
        boolean empty = addressField.getText().isBlank();
        addressError.setText(empty ? "배송 받을 주소를 입력해 주세요" : "");
        Theme.setInvalid(addressField, empty);
        if (empty) {
            focus(addressField);
        }
        return !empty;
    }

    /** UC-12 4단계: 수령인 정보를 검증한다. 틀리면 그 칸 옆에 이유를 쓰고 null. */
    private GiftInfo checkGift() {
        String name = giftNameField.getText().trim();
        String phone = giftPhoneField.getText().trim();
        String address = giftAddressField.getText().trim();
        boolean ok = show(giftNameError, giftNameField, name.isEmpty() ? "받는 분 이름을 입력해 주세요" : null)
                & show(giftPhoneError, giftPhoneField, SignUpPanel.phoneMessage(phone))
                & show(giftAddressError, giftAddressField, address.isEmpty() ? "받는 분 주소를 입력해 주세요" : null);
        if (!ok) {
            JTextField first = !giftNameError.getText().isEmpty() ? giftNameField
                    : !giftPhoneError.getText().isEmpty() ? giftPhoneField : giftAddressField;
            focus(first);
            return null;
        }
        String message = giftMessageField.getText().trim();
        return new GiftInfo(name, phone, address, message.isEmpty() ? null : message);
    }

    private void clearErrors() {
        show(addressError, addressField, null);
        show(giftNameError, giftNameField, null);
        show(giftPhoneError, giftPhoneField, null);
        show(giftAddressError, giftAddressField, null);
    }

    private static boolean show(JLabel error, JComponent input, String message) {
        error.setText(message == null ? "" : message);
        Theme.setInvalid(input, message != null);
        return message == null;
    }

    /** 틀린 칸이 화면 밖이면 보이게 내리고 커서를 둔다. */
    private void focus(JComponent field) {
        field.scrollRectToVisible(new Rectangle(0, -field.getHeight() * 2, field.getWidth(), field.getHeight() * 3));
        field.requestFocusInWindow();
    }

    // ── 주문 완료 (UC-11 8단계, UC-12 5단계) ──

    private void renderCompleted() {
        JPanel card = whiteCard();
        JLabel headline = Theme.label(completed.giftInfo() == null ? "주문이 완료되었어요" : "선물 주문이 완료되었어요",
                1.3f, Font.BOLD, Theme.TEXT);
        headline.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.addToCard(card, headline, 8);
        if (completed.giftInfo() != null) {
            JLabel badge = Theme.label("선물 주문", 1f, Font.BOLD, Theme.TEXT);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            Theme.addToCard(card, badge, 8);
        }
        Theme.addToCard(card, line(), 24);
        Theme.addToCard(card, amountRow("주문번호", completed.orderNumber(), false), 20);
        Theme.addToCard(card, amountRow("주문 일시",
                completed.orderedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")), false), 12);
        Theme.addToCard(card, amountRow("결제 방법", completed.paymentMethod(), false), 12);
        Theme.addToCard(card, amountRow("결제 금액", price(completed.totalPrice()), true), 12);
        Theme.addToCard(card, line(), 20);
        if (completed.giftInfo() != null) {
            Theme.addToCard(card, amountRow("받는 분", completed.giftInfo().recipientName(), false), 20);
            if (completed.giftInfo().message() != null) {
                Theme.addToCard(card, amountRow("선물 메시지", completed.giftInfo().message(), false), 12);
            }
        }
        Theme.addToCard(card, Theme.label("배송지", 1f, Font.PLAIN, Theme.HINT), 20);
        Theme.addToCard(card, Theme.label(completed.shippingAddress(), 1f, Font.PLAIN, Theme.TEXT), 4);

        JButton history = Theme.primaryButton("주문 내역 보기");
        history.addActionListener(e -> frame.navigate(OrderHistoryPanel.NAME));
        JButton shopping = Theme.secondaryButton("쇼핑 계속하기");
        shopping.addActionListener(e -> frame.navigate(ProductListPanel.NAME));
        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.add(shopping);
        buttons.add(history);
        Theme.addToCard(card, buttons, 32);
        Theme.addToCard(column, card, 24);
        payBar.setVisible(false);
    }

    // ── 작은 부품 ──

    private static int columnWidth() {
        float fontSize = UIManager.getFont("Label.font").getSize2D();
        return Math.max(640, Math.round(fontSize * 40));
    }

    private static JPanel whiteCard() {
        JPanel card = Theme.card();
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return card;
    }

    private static JPanel sectionHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label(title, 1.2f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        return header;
    }

    /** 이름 | 값 두 칸 표의 한 줄. 이름 칸 폭이 같아 값이 가지런하다. */
    private static void infoRow(JPanel table, int row, String name, JLabel value) {
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.gridy = row;
        c.anchor = java.awt.GridBagConstraints.WEST;
        c.insets = new java.awt.Insets(row == 0 ? 0 : 12, 0, 0, 24);
        table.add(Theme.label(name, 1f, Font.PLAIN, Theme.HINT), c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new java.awt.Insets(row == 0 ? 0 : 12, 0, 0, 0);
        table.add(value, c);
    }

    private static JPanel amountRow(String name, String value, boolean emphasize) {
        JPanel row = new JPanel(new BorderLayout(24, 0));
        row.setOpaque(false);
        row.add(Theme.label(name, emphasize ? 1.1f : 1f, emphasize ? Font.BOLD : Font.PLAIN,
                emphasize ? Theme.TEXT : Theme.HINT), BorderLayout.WEST);
        row.add(Theme.label(value, emphasize ? 1.3f : 1f, emphasize ? Font.BOLD : Font.PLAIN, Theme.TEXT),
                BorderLayout.EAST);
        return row;
    }

    private static JComponent line() {
        JPanel line = new JPanel();
        line.setBackground(Theme.LINE);
        line.setPreferredSize(new Dimension(1, 1));
        return line;
    }

    private static JPanel centered(JComponent component) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.add(component);
        return row;
    }

    private static String price(int won) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(won) + "원";
    }
}
