package com.sadamda.view.order;

import com.sadamda.common.AppException;
import com.sadamda.common.RetryableException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.member.Member;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderItem;
import com.sadamda.model.order.OrderStatus;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.WrapLayout;
import com.sadamda.view.member.LoginPanel;
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
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * UC-13 주문·배송 조회 및 취소·교환·반품: 주문 목록(주문일·상품·금액·상태) → 주문 상세(배송 단계) →
 * [주문 취소] / [교환·반품 신청]. 선물 주문은 "선물 주문"으로 표시한다 (UC-12 5단계).
 */
public class OrderHistoryPanel extends ScreenPanel {
    public static final String NAME = "orderHistory";

    private static final List<String> CANCEL_REASONS = List.of("단순 변심", "주문을 잘못했어요", "다른 상품으로 다시 주문할게요");
    private static final List<String> RETURN_REASONS =
            List.of("사이즈가 맞지 않아요", "상품이 설명과 달라요", "상품에 문제가 있어요", "단순 변심");
    private static final List<OrderStatus> STEPS =
            List.of(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.SHIPPING, OrderStatus.DELIVERED);
    private static final Map<OrderStatus, String> STATUS_NAMES = Map.of(
            OrderStatus.PAID, "결제완료", OrderStatus.PREPARING, "배송준비", OrderStatus.SHIPPING, "배송중",
            OrderStatus.DELIVERED, "배송완료", OrderStatus.CANCELED, "취소됨",
            OrderStatus.EXCHANGE_REQUESTED, "교환 신청됨", OrderStatus.RETURN_REQUESTED, "반품 신청됨",
            OrderStatus.RETURNED, "반품 완료");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final JPanel column = new JPanel(new GridBagLayout()) {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(OrderPanel.columnWidth(), super.getPreferredSize().height);
        }
    };
    private final JScrollPane page;
    /** 상세를 보고 있는 주문. null 이면 목록. */
    private Long detailId;

    public OrderHistoryPanel(AppContext context, MainFrame frame) {
        super(context, frame, "주문 내역", "UC-13 주문·배송 조회 및 취소·교환·반품");
        hideTitle();
        setBackground(Theme.PAGE);
        setBorder(BorderFactory.createEmptyBorder());
        column.setOpaque(false);
        page = Theme.centeredPage(column);
        page.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        setContent(page);

        // 화면을 열 때마다 목록부터 다시 불러온다. 비회원이면 로그인 후 이 화면으로 돌아온다.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (context.member.currentMember() == null) {
                    frame.navigate(LoginPanel.NAME);
                    return;
                }
                detailId = null;
                render();
            }
        });
    }

    private List<Order> fetchOrders() {
        return context.order.myOrders();
    }

    private Order fetchOrder(Long orderId) {
        return context.order.orderDetail(orderId);
    }

    private void render() {
        column.removeAll();
        if (detailId == null) {
            renderList();
        } else {
            renderDetail(fetchOrder(detailId));
        }
        column.revalidate();
        column.repaint();
        page.getViewport().setViewPosition(new Point(0, 0));
    }

    // ── 목록 (1~2단계) ──

    private void renderList() {
        JLabel title = Theme.label("주문 내역", 1.6f, Font.BOLD, Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.addToCard(column, title, 0);
        List<Order> orders = fetchOrders();
        if (orders.isEmpty()) {
            JPanel card = OrderPanel.whiteCard();
            card.setBorder(BorderFactory.createEmptyBorder(24, 24, 48, 24));
            JLabel message = Theme.label("아직 주문한 상품이 없어요", 1.2f, Font.BOLD, Theme.TEXT);
            message.setHorizontalAlignment(SwingConstants.CENTER);
            JButton browse = Theme.primaryButton("상품 보러 가기");
            browse.addActionListener(e -> frame.navigate(ProductListPanel.NAME));
            Theme.addToCard(card, message, 24);
            Theme.addToCard(card, OrderPanel.centered(browse), 24);
            Theme.addToCard(column, card, 24);
            return;
        }
        for (Order order : orders) {
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(Theme.label(order.getOrderedAt().format(DAY), 1.2f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
            JButton detail = Theme.linkButton("주문 상세");
            detail.getAccessibleContext().setAccessibleName(order.getOrderedAt().format(DAY) + " 주문 상세");
            detail.addActionListener(e -> openDetail(order.getId()));
            header.add(detail, BorderLayout.EAST);
            Theme.addToCard(column, header, 32);
            Theme.addToCard(column, orderCard(order), 4);
        }
    }

    /** 주문 한 건: 상태(+선물 주문), 상품 줄, 총 결제금액, 상태별 안내·버튼. 카드를 눌러도 상세로 간다. */
    private JPanel orderCard(Order order) {
        JPanel card = OrderPanel.whiteCard();
        Theme.addToCard(card, statusRow(order), 0);
        for (OrderItem item : order.getItems()) {
            Theme.addToCard(card, OrderPanel.itemRow(context.product, item), 16);
        }
        Theme.addToCard(card, OrderPanel.line(), 20);
        Theme.addToCard(card, OrderPanel.amountRow("총 결제금액", OrderPanel.price(order.getTotalPrice()), true), 16);
        addActions(card, order);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openDetail(order.getId());
            }
        });
        return card;
    }

    private void openDetail(Long orderId) {
        detailId = orderId;
        render();
    }

    // ── 상세 (3단계) ──

    private void renderDetail(Order order) {
        JButton back = Theme.linkButton("← 주문 내역으로");
        back.addActionListener(e -> {
            detailId = null;
            render();
        });
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.setOpaque(false);
        backRow.add(back);
        Theme.addToCard(column, backRow, 0);
        JLabel title = Theme.label("주문 상세", 1.6f, Font.BOLD, Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.addToCard(column, title, 8);

        JPanel status = OrderPanel.whiteCard();
        Theme.addToCard(status, statusRow(order), 0);
        Theme.addToCard(status, steps(order.getStatus()), 20);
        addActions(status, order);
        Theme.addToCard(column, status, 24);

        Theme.addToCard(column, OrderPanel.sectionHeader("주문 정보"), 32);
        JPanel info = OrderPanel.whiteCard();
        Theme.addToCard(info, OrderPanel.amountRow("주문번호", order.getOrderNumber(), false), 0);
        Theme.addToCard(info, OrderPanel.amountRow("주문 일시", order.getOrderedAt().format(TIME), false), 12);
        if (order.getTrackingNumber() != null) {
            Theme.addToCard(info, OrderPanel.amountRow("송장번호", order.getTrackingNumber(), false), 12);
        }
        Theme.addToCard(column, info, 12);

        Theme.addToCard(column, OrderPanel.sectionHeader("주문 상품"), 32);
        JPanel items = OrderPanel.whiteCard();
        boolean first = true;
        for (OrderItem item : order.getItems()) {
            Theme.addToCard(items, OrderPanel.itemRow(context.product, item), first ? 0 : 20);
            first = false;
        }
        Theme.addToCard(column, items, 12);

        Theme.addToCard(column, OrderPanel.sectionHeader(order.getGiftInfo() == null ? "배송지" : "선물 받는 분"), 32);
        JPanel address = OrderPanel.whiteCard();
        if (order.getGiftInfo() != null) {
            Theme.addToCard(address, OrderPanel.amountRow("받는 분", order.getGiftInfo().recipientName(), false), 0);
            Theme.addToCard(address, OrderPanel.amountRow("연락처", order.getGiftInfo().recipientPhone(), false), 12);
            if (order.getGiftInfo().message() != null) {
                Theme.addToCard(address, OrderPanel.amountRow("선물 메시지", order.getGiftInfo().message(), false), 12);
            }
            Theme.addToCard(address, Theme.label(order.getShippingAddress(), 1f, Font.PLAIN, Theme.TEXT), 12);
        } else {
            Theme.addToCard(address, Theme.label(order.getShippingAddress(), 1f, Font.PLAIN, Theme.TEXT), 0);
        }
        Theme.addToCard(column, address, 12);

        Theme.addToCard(column, OrderPanel.sectionHeader("결제 금액"), 32);
        JPanel amount = OrderPanel.whiteCard();
        Theme.addToCard(amount, OrderPanel.amountRow("상품 금액", OrderPanel.price(order.getProductTotal()), false), 0);
        Theme.addToCard(amount, OrderPanel.amountRow("배송비", OrderPanel.price(order.getShippingFee()), false), 12);
        Theme.addToCard(amount, OrderPanel.line(), 16);
        Theme.addToCard(amount, OrderPanel.amountRow("총 결제금액", OrderPanel.price(order.getTotalPrice()), true), 16);
        Theme.addToCard(column, amount, 12);
    }

    /** 결제완료 → 배송준비 → 배송중 → 배송완료. 지난 단계와 지금 단계는 검정, 남은 단계는 회색. */
    private static JPanel steps(OrderStatus status) {
        int current = STEPS.indexOf(status);
        JPanel row = new JPanel(new GridLayout(1, STEPS.size(), 8, 0));
        row.setOpaque(false);
        if (current < 0) { // 취소·교환·반품: 단계 대신 상태만 보여 준다
            row.setLayout(new BorderLayout());
            row.add(Theme.label("이 주문은 " + STATUS_NAMES.get(status) + " 상태예요.", 1f, Font.PLAIN, Theme.HINT));
            return row;
        }
        for (int i = 0; i < STEPS.size(); i++) {
            boolean done = i <= current;
            JLabel step = Theme.label((i == current ? "● " : done ? "✓ " : "○ ") + STATUS_NAMES.get(STEPS.get(i)),
                    1f, i == current ? Font.BOLD : Font.PLAIN, done ? Theme.TEXT : Theme.HINT);
            step.setHorizontalAlignment(SwingConstants.CENTER);
            step.setOpaque(true);
            step.setBackground(i == current ? Theme.SECONDARY : Theme.CARD);
            step.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 3, 0, done ? Theme.TEXT : Theme.LINE),
                    BorderFactory.createEmptyBorder(12, 4, 12, 4)));
            step.getAccessibleContext().setAccessibleName(STATUS_NAMES.get(STEPS.get(i))
                    + (i == current ? ", 지금 단계" : done ? ", 지남" : ", 아직"));
            row.add(step);
        }
        return row;
    }

    // ── 상태 표시와 버튼 (목록·상세 공통) ──

    /** "결제완료" + (선물이면) "선물 주문" */
    private static JPanel statusRow(Order order) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(Theme.label(STATUS_NAMES.get(order.getStatus()), 1.1f, Font.BOLD, Theme.TEXT));
        if (order.getGiftInfo() != null) {
            JLabel gift = Theme.label("선물 주문", 1f, Font.BOLD, Theme.TEXT);
            gift.setOpaque(true);
            gift.setBackground(Theme.SECONDARY);
            gift.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            row.add(javax.swing.Box.createHorizontalStrut(12));
            row.add(gift);
        }
        return row;
    }

    /** 상태에 맞는 안내와 버튼을 카드 아래에 붙인다. */
    private void addActions(JPanel card, Order order) {
        switch (order.getStatus()) {
            case PAID, PREPARING -> {
                JButton cancel = Theme.secondaryButton("주문 취소");
                cancel.addActionListener(e -> openCancelDialog(order));
                Theme.addToCard(card, cancel, 20);
            }
            case SHIPPING -> {
                // 4a: 배송 중에는 취소할 수 없음을 미리 알려 주고, 받은 뒤 반품을 안내한다.
                Theme.addToCard(card, Theme.label("배송 중에는 취소할 수 없어요. 받으신 뒤 반품을 신청해 주세요.",
                        1f, Font.PLAIN, Theme.HINT), 20);
                JButton track = Theme.secondaryButton("배송 조회");
                track.addActionListener(e -> JOptionPane.showMessageDialog(this,
                        "송장번호 " + order.getTrackingNumber() + "\n상품이 배송 중이에요.", "배송 조회",
                        JOptionPane.INFORMATION_MESSAGE));
                Theme.addToCard(card, track, 12);
            }
            case DELIVERED -> {
                JButton review = Theme.primaryButton("리뷰 쓰기");
                review.addActionListener(e -> frame.openReview(order.getId())); // UC-14
                JButton change = Theme.secondaryButton("교환·반품 신청");
                change.addActionListener(e -> openReturnDialog(order));
                JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
                buttons.setOpaque(false);
                buttons.add(change);
                buttons.add(review);
                Theme.addToCard(card, buttons, 20);
            }
            default -> {
                // 취소됨·신청됨·반품 완료: 버튼 없이 상태만
            }
        }
    }

    // ── 취소·교환·반품 (4~6단계) ──

    /** [주문 취소]: 사유를 고르고 [취소하기]. 결제 취소가 잠깐 실패하면 이유와 [다시 시도]를 보여 준다. */
    private void openCancelDialog(Order order) {
        ReasonDialog dialog = new ReasonDialog("주문 취소", "취소 사유를 골라 주세요", null, CANCEL_REASONS, "취소하기");
        dialog.onSubmit(() -> {
            String reason = dialog.reason(); // 화면에서 고른 값은 화면 스레드에서 읽어 둔다
            return () -> context.order.cancel(order.getId(), reason);
        });
        dialog.setVisible(true);
    }

    /** [교환·반품 신청]: 교환/반품과 사유를 고르고 [신청하기] → "신청됨" (UC-18 관리자에게 전달). */
    private void openReturnDialog(Order order) {
        ReasonDialog dialog = new ReasonDialog("교환·반품 신청", "사유를 골라 주세요",
                List.of("교환", "반품"), RETURN_REASONS, "신청하기");
        dialog.onSubmit(() -> {
            OrderStatus requested = "교환".equals(dialog.kind())
                    ? OrderStatus.EXCHANGE_REQUESTED : OrderStatus.RETURN_REQUESTED;
            String reason = dialog.reason();
            return () -> context.order.requestExchangeOrReturn(order.getId(), requested, reason);
        });
        dialog.setVisible(true);
    }

    /**
     * 사유(와 교환/반품)를 고르는 작은 창. 처리는 화면 스레드가 아닌 곳에서 하고(결제 취소 대기),
     * 그동안 버튼을 막는다. 성공하면 닫고 화면을 새로 그린다. 실패 문장은 창 안에 보여 준다.
     */
    private final class ReasonDialog extends JDialog {
        private final ButtonGroup kindGroup = new ButtonGroup();
        private final ButtonGroup reasonGroup = new ButtonGroup();
        private final JLabel error = Theme.errorLabel();
        private final JButton submit;
        private final String submitText;
        private Supplier<Supplier<Order>> action;

        ReasonDialog(String title, String prompt, List<String> kinds, List<String> reasons, String submitText) {
            super(SwingUtilities.getWindowAncestor(OrderHistoryPanel.this), title, Dialog.ModalityType.APPLICATION_MODAL);
            this.submitText = submitText;
            JPanel content = new JPanel(new GridBagLayout());
            content.setBackground(Theme.CARD);
            content.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
            Theme.addToCard(content, Theme.label(title, 1.2f, Font.BOLD, Theme.TEXT), 0);
            if (kinds != null) {
                Theme.addToCard(content, Theme.label("교환할까요, 반품할까요?", 1f, Font.BOLD, Theme.TEXT), 20);
                Theme.addToCard(content, chipRow(kinds, kindGroup), 4);
            }
            Theme.addToCard(content, Theme.label(prompt, 1f, Font.BOLD, Theme.TEXT), 20);
            Theme.addToCard(content, chipRow(reasons, reasonGroup), 4);
            Theme.addToCard(content, error, 16);

            submit = Theme.primaryButton(submitText);
            submit.addActionListener(e -> submit());
            JButton close = Theme.secondaryButton("닫기");
            close.addActionListener(e -> dispose());
            JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
            buttons.setOpaque(false);
            buttons.add(close);
            buttons.add(submit);
            Theme.addToCard(content, buttons, 16);

            setContentPane(content);
            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            fitSize();
            setLocationRelativeTo(OrderHistoryPanel.this);
        }

        /** 폭을 먼저 정한 뒤, 그 폭에서 사유 버튼이 몇 줄로 접히는지에 맞춰 높이를 정한다. */
        private void fitSize() {
            pack();
            int width = Math.max(getWidth(), OrderPanel.columnWidth());
            setSize(width, getHeight());
            validate();
            java.awt.Insets insets = getInsets();
            setSize(width, getContentPane().getPreferredSize().height + insets.top + insets.bottom);
        }

        void onSubmit(Supplier<Supplier<Order>> action) {
            this.action = action;
        }

        String kind() {
            return selected(kindGroup);
        }

        String reason() {
            return selected(reasonGroup);
        }

        private void submit() {
            if (kindGroup.getButtonCount() > 0 && kind() == null) {
                error.setText("교환인지 반품인지 골라 주세요");
                return;
            }
            if (reason() == null) {
                error.setText("사유를 골라 주세요");
                return;
            }
            error.setText("");
            Supplier<Order> work = action.get();
            submit.setEnabled(false);
            submit.setText("처리 중…");
            new SwingWorker<Order, Void>() {
                @Override
                protected Order doInBackground() {
                    return work.get();
                }

                @Override
                protected void done() {
                    submit.setEnabled(true);
                    try {
                        get();
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        // RetryableException: 잠깐의 실패라 같은 버튼을 [다시 시도]로 바꿔 다시 누를 수 있게 한다.
                        submit.setText(cause instanceof RetryableException ? "다시 시도" : submitText);
                        error.setText(cause instanceof AppException
                                ? cause.getMessage() : "처리 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.");
                        fitSize();
                        return;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    dispose();
                    render(); // 상태가 바뀐 주문을 다시 보여 준다
                }
            }.execute();
        }

        private JPanel chipRow(List<String> labels, ButtonGroup group) {
            JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 8));
            row.setOpaque(false);
            for (String label : labels) {
                JToggleButton chip = Theme.chip(label);
                chip.setActionCommand(label);
                chip.addActionListener(e -> error.setText(""));
                group.add(chip);
                row.add(chip);
            }
            return row;
        }

        private String selected(ButtonGroup group) {
            return group.getSelection() == null ? null : group.getSelection().getActionCommand();
        }
    }
}
