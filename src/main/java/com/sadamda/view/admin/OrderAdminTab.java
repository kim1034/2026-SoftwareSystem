package com.sadamda.view.admin;

import com.sadamda.common.AppException;
import com.sadamda.common.RetryableException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderStatus;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UC-18 주문 관리: 상태별 주문 목록, 배송 상태 변경(배송준비 → 배송중(송장번호) → 배송완료),
 * 교환·반품 승인(반품은 환불, 실패하면 다시 시도 5a)·거절(사유).
 */
final class OrderAdminTab extends JPanel {
    static final Map<OrderStatus, String> STATUS_NAMES = new LinkedHashMap<>();

    static {
        STATUS_NAMES.put(OrderStatus.PAID, "결제완료");
        STATUS_NAMES.put(OrderStatus.PREPARING, "배송준비");
        STATUS_NAMES.put(OrderStatus.SHIPPING, "배송중");
        STATUS_NAMES.put(OrderStatus.DELIVERED, "배송완료");
        STATUS_NAMES.put(OrderStatus.EXCHANGE_REQUESTED, "교환 신청");
        STATUS_NAMES.put(OrderStatus.RETURN_REQUESTED, "반품 신청");
        STATUS_NAMES.put(OrderStatus.RETURNED, "반품 완료");
        STATUS_NAMES.put(OrderStatus.CANCELED, "취소됨");
    }

    private static final String ALL = "전체";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final AppContext context;
    private final JComboBox<String> filter;
    private final JTable table = AdminUi.table("번호", "주문번호", "주문 일시", "회원", "상품", "결제금액", "상태", "송장번호", "사유");
    private final JButton preparing = AdminUi.small(Theme.secondaryButton("배송준비로"));
    private final JButton shipping = AdminUi.small(Theme.secondaryButton("배송중으로 (송장 입력)"));
    private final JButton delivered = AdminUi.small(Theme.secondaryButton("배송완료로"));
    private final JButton approve = AdminUi.small(Theme.primaryButton("교환·반품 승인"));
    private final JButton reject = AdminUi.small(Theme.secondaryButton("거절"));

    OrderAdminTab(AppContext context) {
        super(new BorderLayout(0, 16));
        this.context = context;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        List<String> names = new ArrayList<>(List.of(ALL));
        names.addAll(STATUS_NAMES.values());
        filter = Theme.comboBox(names.toArray(String[]::new));
        filter.addActionListener(e -> refresh());
        JButton reload = AdminUi.small(Theme.secondaryButton("새로 고침"));
        reload.addActionListener(e -> refresh());
        add(AdminUi.row(Theme.label("상태", 1f, java.awt.Font.BOLD, Theme.TEXT), filter, reload), BorderLayout.NORTH);
        add(AdminUi.scroll(table), BorderLayout.CENTER);

        preparing.addActionListener(e -> changeStatus(OrderStatus.PREPARING));
        shipping.addActionListener(e -> changeStatus(OrderStatus.SHIPPING));
        delivered.addActionListener(e -> changeStatus(OrderStatus.DELIVERED));
        approve.addActionListener(e -> approve());
        reject.addActionListener(e -> reject());
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        add(AdminUi.row(preparing, shipping, delivered, approve, reject), BorderLayout.SOUTH);
        updateButtons();
    }

    void refresh() {
        String chosen = (String) filter.getSelectedItem();
        OrderStatus status = STATUS_NAMES.entrySet().stream().filter(e -> e.getValue().equals(chosen))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        List<Object[]> rows = new ArrayList<>();
        for (Order o : context.admin.orders(status)) {
            rows.add(new Object[]{o.getId(), o.getOrderNumber(), o.getOrderedAt().format(TIME),
                    "회원 " + o.getMemberId() + (o.isGift() ? " (선물)" : ""), o.getItems().size() + "건",
                    AdminUi.price(o.getTotalPrice()), STATUS_NAMES.get(o.getStatus()),
                    o.getTrackingNumber() == null ? "" : o.getTrackingNumber(),
                    o.getReason() == null ? "" : o.getReason()});
        }
        AdminUi.fill(table, rows);
        updateButtons();
    }

    private Order selected() {
        Long id = AdminUi.selectedId(table);
        if (id == null) {
            return null;
        }
        return context.admin.orders(null).stream().filter(o -> o.getId().equals(id)).findFirst().orElse(null);
    }

    /** 고른 주문의 상태에서 할 수 있는 일만 누를 수 있게 한다. */
    private void updateButtons() {
        Order order = selected();
        OrderStatus s = order == null ? null : order.getStatus();
        preparing.setEnabled(s == OrderStatus.PAID);
        shipping.setEnabled(s == OrderStatus.PREPARING);
        delivered.setEnabled(s == OrderStatus.SHIPPING);
        boolean request = s == OrderStatus.EXCHANGE_REQUESTED || s == OrderStatus.RETURN_REQUESTED;
        approve.setEnabled(request);
        reject.setEnabled(request);
    }

    /** UC-18 3단계. 배송중으로 바꿀 때는 송장번호를 묻는다. */
    private void changeStatus(OrderStatus status) {
        Order order = selected();
        if (order == null) {
            AdminUi.needSelection(this);
            return;
        }
        String tracking = null;
        if (status == OrderStatus.SHIPPING) {
            tracking = JOptionPane.showInputDialog(this, "송장번호를 입력해 주세요", "배송중으로 바꾸기",
                    JOptionPane.PLAIN_MESSAGE);
            if (tracking == null) {
                return; // 취소
            }
        }
        String number = tracking;
        AdminUi.run(this, "배송 상태 변경", () -> context.admin.updateOrderStatus(order.getId(), status, number));
        refreshKeepingSelection(order.getId());
    }

    /** UC-18 4~5단계. 반품이면 환불하고, 환불이 잠깐 실패하면 다시 시도할지 묻는다 (5a). */
    private void approve() {
        Order order = selected();
        if (order == null) {
            AdminUi.needSelection(this);
            return;
        }
        boolean isReturn = order.getStatus() == OrderStatus.RETURN_REQUESTED;
        String question = isReturn
                ? "반품을 승인할까요? 결제금액 " + AdminUi.price(order.getTotalPrice()) + "을 환불합니다."
                : "교환을 승인할까요? 주문이 다시 배송준비 상태가 됩니다.";
        if (!AdminUi.confirm(this, question, "교환·반품 승인")) {
            return;
        }
        while (true) {
            try {
                context.admin.approveRequest(order.getId());
                break;
            } catch (RetryableException e) {
                if (!AdminUi.confirm(this, e.getMessage() + "\n다시 시도할까요?", "환불 실패")) {
                    break;
                }
            } catch (AppException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "승인하지 못했어요", JOptionPane.WARNING_MESSAGE);
                break;
            }
        }
        refreshKeepingSelection(order.getId());
    }

    private void reject() {
        Order order = selected();
        if (order == null) {
            AdminUi.needSelection(this);
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "거절 사유를 입력해 주세요 (회원에게 보입니다)", "교환·반품 거절",
                JOptionPane.PLAIN_MESSAGE);
        if (reason == null) {
            return;
        }
        AdminUi.run(this, "거절", () -> context.admin.rejectRequest(order.getId(), reason));
        refreshKeepingSelection(order.getId());
    }

    private void refreshKeepingSelection(Long orderId) {
        refresh();
        for (int view = 0; view < table.getRowCount(); view++) {
            Object id = table.getModel().getValueAt(table.convertRowIndexToModel(view), 0);
            if (orderId.equals(((Number) id).longValue())) {
                table.setRowSelectionInterval(view, view);
                break;
            }
        }
    }
}
