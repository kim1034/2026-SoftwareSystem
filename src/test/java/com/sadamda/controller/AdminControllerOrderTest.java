package com.sadamda.controller;

import com.sadamda.common.AccessDeniedException;
import com.sadamda.common.AppException;
import com.sadamda.common.RetryableException;
import com.sadamda.external.PaymentGateway;
import com.sadamda.external.PaymentGateway.PaymentResult;
import com.sadamda.model.member.Member;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.order.OrderStatus;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.recommend.RecommendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

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

class AdminControllerOrderTest {
    private final Session session = new Session();
    private final OrderService orderService = mock(OrderService.class);
    private final PaymentGateway gateway = mock(PaymentGateway.class);
    private final AdminController controller = new AdminController(session, mock(ProductService.class),
            mock(RecommendService.class), orderService, gateway);

    @BeforeEach
    void loginAsAdmin() {
        session.login(SessionTest.member(9L, Member.Role.ADMIN));
    }

    private Order order(long id, OrderStatus status) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(id);
        when(order.getStatus()).thenReturn(status);
        when(order.getPaymentId()).thenReturn("pay-" + id);
        when(orderService.findById(id)).thenReturn(order);
        return order;
    }

    @Test
    void shipping_withoutTracking_throws() {
        AppException e = assertThrows(AppException.class,
                () -> controller.updateOrderStatus(5L, OrderStatus.SHIPPING, " "));
        assertEquals("송장번호를 입력해 주세요.", e.getMessage());
        verify(orderService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void shipping_withTracking_updates() {
        Order updated = mock(Order.class);
        when(orderService.updateStatus(5L, OrderStatus.SHIPPING, "1234-5678")).thenReturn(updated);
        assertSame(updated, controller.updateOrderStatus(5L, OrderStatus.SHIPPING, "1234-5678"));
    }

    @Test
    void approveReturn_refundsThenApproves() {
        order(5L, OrderStatus.RETURN_REQUESTED);
        Order approved = mock(Order.class);
        when(gateway.cancel("pay-5")).thenReturn(new PaymentResult(true, "pay-5", "환불"));
        when(orderService.approveChange(5L)).thenReturn(approved);

        assertSame(approved, controller.approveRequest(5L));

        InOrder inOrder = inOrder(gateway, orderService);
        inOrder.verify(gateway).cancel("pay-5");
        inOrder.verify(orderService).approveChange(5L);
    }

    @Test
    void approveReturn_refundFails_retryable_notApproved() {
        order(5L, OrderStatus.RETURN_REQUESTED);
        when(gateway.cancel("pay-5")).thenReturn(new PaymentResult(false, "pay-5", "PG 오류"));
        RetryableException e = assertThrows(RetryableException.class, () -> controller.approveRequest(5L));
        assertEquals("환불 요청에 실패했습니다. 다시 시도해 주세요.", e.getMessage());
        verify(orderService, never()).approveChange(any());
    }

    @Test
    void approveReturn_refundThrows_retryable_notApproved() {
        order(5L, OrderStatus.RETURN_REQUESTED);
        when(gateway.cancel("pay-5")).thenThrow(new RuntimeException("timeout"));
        assertThrows(RetryableException.class, () -> controller.approveRequest(5L));
        verify(orderService, never()).approveChange(any());
    }

    @Test
    void approveExchange_noRefund() {
        order(5L, OrderStatus.EXCHANGE_REQUESTED);
        controller.approveRequest(5L);
        verify(gateway, never()).cancel(anyString());
        verify(orderService).approveChange(5L);
    }

    @Test
    void approveOtherStatus_throws() {
        order(5L, OrderStatus.DELIVERED);
        AppException e = assertThrows(AppException.class, () -> controller.approveRequest(5L));
        assertEquals("처리할 교환·반품 요청이 없습니다.", e.getMessage());
        verify(gateway, never()).cancel(anyString());
    }

    @Test
    void reject_blankReason_throws() {
        AppException e = assertThrows(AppException.class, () -> controller.rejectRequest(5L, ""));
        assertEquals("거절 사유를 입력해 주세요.", e.getMessage());
        verify(orderService, never()).rejectChange(any(), any());
    }

    @Test
    void nonAdmin_accessDenied() {
        session.login(SessionTest.member(1L, Member.Role.MEMBER));
        assertThrows(AccessDeniedException.class, () -> controller.approveRequest(5L));
        assertThrows(AccessDeniedException.class, () -> controller.rejectRequest(5L, "사유"));
        assertThrows(AccessDeniedException.class,
                () -> controller.updateOrderStatus(5L, OrderStatus.DELIVERED, null));
        verify(gateway, never()).cancel(anyString());
    }
}
