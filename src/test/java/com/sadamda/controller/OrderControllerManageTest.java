package com.sadamda.controller;

import com.sadamda.common.AccessDeniedException;
import com.sadamda.common.AppException;
import com.sadamda.common.RetryableException;
import com.sadamda.external.PaymentGateway;
import com.sadamda.external.PaymentGateway.PaymentResult;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.member.Member;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderControllerManageTest {
    private final Session session = new Session();
    private final OrderService orderService = mock(OrderService.class);
    private final PaymentGateway gateway = mock(PaymentGateway.class);
    private final OrderController controller = new OrderController(session, orderService, mock(CartService.class),
            mock(MemberService.class), gateway);

    @BeforeEach
    void login() {
        session.login(SessionTest.member(1L, Member.Role.MEMBER));
    }

    private Order order(long id, long memberId, OrderStatus status) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(id);
        when(order.getMemberId()).thenReturn(memberId);
        when(order.getStatus()).thenReturn(status);
        when(order.getPaymentId()).thenReturn("pay-" + id);
        when(orderService.findById(id)).thenReturn(order);
        return order;
    }

    @Test
    void myOrders_returnsServiceResultForCurrentMember() {
        List<Order> orders = List.of(mock(Order.class));
        when(orderService.findByMember(1L)).thenReturn(orders);
        assertSame(orders, controller.myOrders());
    }

    @Test
    void detail_ownOrder_returned() {
        Order mine = order(5L, 1L, OrderStatus.PAID);
        assertSame(mine, controller.orderDetail(5L));
    }

    @Test
    void detail_otherMembersOrder_accessDenied() {
        order(5L, 2L, OrderStatus.PAID);
        AccessDeniedException e = assertThrows(AccessDeniedException.class, () -> controller.orderDetail(5L));
        assertEquals("본인 주문만 볼 수 있습니다.", e.getMessage());
    }

    @Test
    void cancel_paid_cancelsPaymentThenOrder() {
        order(5L, 1L, OrderStatus.PAID);
        Order canceled = mock(Order.class);
        when(gateway.cancel("pay-5")).thenReturn(new PaymentResult(true, "pay-5", "취소"));
        when(orderService.cancel(5L, "단순 변심")).thenReturn(canceled);

        assertSame(canceled, controller.cancel(5L, "단순 변심"));

        InOrder inOrder = inOrder(gateway, orderService);
        inOrder.verify(gateway).cancel("pay-5");
        inOrder.verify(orderService).cancel(5L, "단순 변심");
    }

    @Test
    void cancel_preparing_allowed() {
        order(5L, 1L, OrderStatus.PREPARING);
        when(gateway.cancel("pay-5")).thenReturn(new PaymentResult(true, "pay-5", "취소"));
        controller.cancel(5L, "단순 변심");
        verify(orderService).cancel(5L, "단순 변심");
    }

    @Test
    void cancel_shipping_throws4aMessage_noPaymentCall() {
        order(5L, 1L, OrderStatus.SHIPPING);
        AppException e = assertThrows(AppException.class, () -> controller.cancel(5L, "단순 변심"));
        assertEquals("이미 배송이 시작되어 취소할 수 없습니다. 배송 완료 후 반품을 신청해 주세요.", e.getMessage());
        verify(gateway, never()).cancel(anyString());
    }

    @Test
    void cancel_alreadyCanceled_throws() {
        order(5L, 1L, OrderStatus.CANCELED);
        AppException e = assertThrows(AppException.class, () -> controller.cancel(5L, "단순 변심"));
        assertEquals("취소할 수 없는 주문입니다.", e.getMessage());
        verify(gateway, never()).cancel(anyString());
    }

    @Test
    void cancel_otherMembersOrder_accessDenied() {
        order(5L, 2L, OrderStatus.PAID);
        assertThrows(AccessDeniedException.class, () -> controller.cancel(5L, "단순 변심"));
        verify(gateway, never()).cancel(anyString());
    }

    @Test
    void cancel_paymentCancelFails_retryable_orderUntouched() {
        order(5L, 1L, OrderStatus.PAID);
        when(gateway.cancel("pay-5")).thenReturn(new PaymentResult(false, "pay-5", "PG 오류"));
        RetryableException e = assertThrows(RetryableException.class, () -> controller.cancel(5L, "단순 변심"));
        assertEquals("결제 취소에 실패했습니다. 잠시 후 다시 시도해 주세요.", e.getMessage());
        verify(orderService, never()).cancel(any(), any());
    }

    @Test
    void cancel_gatewayThrows_retryable() {
        order(5L, 1L, OrderStatus.PAID);
        when(gateway.cancel("pay-5")).thenThrow(new RuntimeException("timeout"));
        assertThrows(RetryableException.class, () -> controller.cancel(5L, "단순 변심"));
        verify(orderService, never()).cancel(any(), any());
    }

    @Test
    void returnRequest_delivered_ok() {
        order(5L, 1L, OrderStatus.DELIVERED);
        Order requested = mock(Order.class);
        when(orderService.requestChange(5L, OrderStatus.RETURN_REQUESTED, "사이즈가 안 맞아요")).thenReturn(requested);
        assertSame(requested,
                controller.requestExchangeOrReturn(5L, OrderStatus.RETURN_REQUESTED, "사이즈가 안 맞아요"));
    }

    @Test
    void returnRequest_notDelivered_throws() {
        order(5L, 1L, OrderStatus.SHIPPING);
        AppException e = assertThrows(AppException.class,
                () -> controller.requestExchangeOrReturn(5L, OrderStatus.EXCHANGE_REQUESTED, "색상 교환"));
        assertEquals("배송 완료된 주문만 교환·반품을 신청할 수 있습니다.", e.getMessage());
    }

    @Test
    void returnRequest_blankReason_throws() {
        order(5L, 1L, OrderStatus.DELIVERED);
        AppException e = assertThrows(AppException.class,
                () -> controller.requestExchangeOrReturn(5L, OrderStatus.RETURN_REQUESTED, " "));
        assertEquals("사유를 골라 주세요.", e.getMessage());
    }

    @Test
    void returnRequest_wrongStatusArgument_illegalArgument() {
        order(5L, 1L, OrderStatus.DELIVERED);
        assertThrows(IllegalArgumentException.class,
                () -> controller.requestExchangeOrReturn(5L, OrderStatus.CANCELED, "사유"));
    }
}
