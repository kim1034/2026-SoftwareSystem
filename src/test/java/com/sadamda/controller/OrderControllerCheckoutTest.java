package com.sadamda.controller;

import com.sadamda.common.AppException;
import com.sadamda.external.PaymentGateway;
import com.sadamda.external.PaymentGateway.PaymentResult;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.member.Member;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.GiftInfo;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderControllerCheckoutTest {
    private static final List<OrderLine> LINES = List.of(new OrderLine(10L, 100L, 2));
    private static final String ADDRESS = "서울시 중구 1";

    private final Session session = new Session();
    private final OrderService orderService = mock(OrderService.class);
    private final CartService cartService = mock(CartService.class);
    private final PaymentGateway gateway = mock(PaymentGateway.class);
    private final OrderController controller = new OrderController(session, orderService, cartService,
            mock(MemberService.class), gateway);
    private final Order order = mock(Order.class);

    @BeforeEach
    void setUp() {
        session.login(SessionTest.member(1L, Member.Role.MEMBER));
        when(orderService.calculateTotal(LINES)).thenReturn(59000);
        when(gateway.approve(59000L, "카드")).thenReturn(new PaymentResult(true, "pay-1", "승인"));
        when(gateway.cancel("pay-1")).thenReturn(new PaymentResult(true, "pay-1", "취소"));
        when(orderService.placeOrder(eq(1L), eq(LINES), anyString(), any(), eq("pay-1"))).thenReturn(order);
    }

    private static CheckoutRequest request(GiftInfo gift, boolean fromCart) {
        return new CheckoutRequest(LINES, ADDRESS, "카드", gift, fromCart);
    }

    @Test
    void success_returnsOrder_andClearsCartWhenFromCart() {
        assertSame(order, controller.checkout(request(null, true)));
        verify(gateway).approve(59000L, "카드");
        verify(orderService).placeOrder(1L, LINES, ADDRESS, null, "pay-1");
        verify(orderService).clearOrderedFromCart(1L, LINES);
    }

    @Test
    void success_direct_doesNotClearCart() {
        controller.checkout(request(null, false));
        verify(orderService, never()).clearOrderedFromCart(anyLong(), any());
    }

    @Test
    void gift_shipsToRecipientAddress() {
        GiftInfo gift = new GiftInfo("김엄마", "010-1234-5678", "부산시 해운대구 2", "생신 축하해요");
        controller.checkout(request(gift, true));
        verify(orderService).placeOrder(1L, LINES, "부산시 해운대구 2", gift, "pay-1");
    }

    @Test
    void gift_badPhone_throwsBeforePayment() {
        GiftInfo gift = new GiftInfo("김엄마", "010-12", "부산시 해운대구 2", "");
        AppException e = assertThrows(AppException.class, () -> controller.checkout(request(gift, true)));
        assertEquals("받는 분 연락처를 숫자로 정확히 입력해 주세요.", e.getMessage());
        verify(gateway, never()).approve(anyLong(), anyString());
    }

    @Test
    void gift_missingRecipientName_throws() {
        GiftInfo gift = new GiftInfo(" ", "010-1234-5678", "부산시 해운대구 2", "");
        AppException e = assertThrows(AppException.class, () -> controller.checkout(request(gift, true)));
        assertEquals("받는 분의 이름, 연락처, 주소를 모두 입력해 주세요.", e.getMessage());
    }

    @Test
    void blankAddress_throwsBeforePayment() {
        CheckoutRequest req = new CheckoutRequest(LINES, " ", "카드", null, true);
        AppException e = assertThrows(AppException.class, () -> controller.checkout(req));
        assertEquals("배송지를 입력해 주세요.", e.getMessage());
        verify(gateway, never()).approve(anyLong(), anyString());
    }

    @Test
    void missingPaymentMethod_throws() {
        CheckoutRequest req = new CheckoutRequest(LINES, ADDRESS, null, null, true);
        AppException e = assertThrows(AppException.class, () -> controller.checkout(req));
        assertEquals("결제 수단을 골라 주세요.", e.getMessage());
    }

    @Test
    void noLines_throws() {
        CheckoutRequest req = new CheckoutRequest(List.of(), ADDRESS, "카드", null, true);
        AppException e = assertThrows(AppException.class, () -> controller.checkout(req));
        assertEquals("주문할 상품이 없습니다.", e.getMessage());
    }

    @Test
    void declined_throwsWithReason_noOrderCreated() {
        when(gateway.approve(59000L, "카드")).thenReturn(new PaymentResult(false, null, "한도 초과"));
        AppException e = assertThrows(AppException.class, () -> controller.checkout(request(null, true)));
        assertEquals("결제가 완료되지 않았습니다. 한도 초과", e.getMessage());
        verify(orderService, never()).placeOrder(anyLong(), any(), anyString(), any(), anyString());
    }

    @Test
    void gatewayThrows_noOrderCreated() {
        when(gateway.approve(59000L, "카드")).thenThrow(new RuntimeException("connection reset"));
        AppException e = assertThrows(AppException.class, () -> controller.checkout(request(null, true)));
        assertEquals("결제 시스템에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.", e.getMessage());
        verify(orderService, never()).placeOrder(anyLong(), any(), anyString(), any(), anyString());
    }

    @Test
    void soldOutDuringPayment_cancelsPayment() {
        when(orderService.placeOrder(eq(1L), eq(LINES), anyString(), any(), eq("pay-1")))
                .thenThrow(new AppException("주문하신 상품이 품절되었습니다."));
        AppException e = assertThrows(AppException.class, () -> controller.checkout(request(null, true)));
        assertEquals("주문하신 상품이 품절되었습니다. 결제는 취소되었습니다.", e.getMessage());
        verify(gateway).cancel("pay-1");
        verify(orderService, never()).clearOrderedFromCart(anyLong(), any());
    }

    @Test
    void placeOrderFailsWithAnyException_cancelsPayment() {
        when(orderService.placeOrder(eq(1L), eq(LINES), anyString(), any(), eq("pay-1")))
                .thenThrow(new IllegalStateException("db down"));
        AppException e = assertThrows(AppException.class, () -> controller.checkout(request(null, true)));
        assertEquals("주문을 저장하지 못해 결제를 취소했습니다.", e.getMessage());
        verify(gateway).cancel("pay-1");
    }

    @Test
    void cancelAlsoFails_tellsUserToContactUs() {
        when(orderService.placeOrder(eq(1L), eq(LINES), anyString(), any(), eq("pay-1")))
                .thenThrow(new AppException("품절"));
        when(gateway.cancel("pay-1")).thenReturn(new PaymentResult(false, "pay-1", "취소 불가"));
        AppException e = assertThrows(AppException.class, () -> controller.checkout(request(null, true)));
        assertEquals("결제 취소에 실패했습니다. 고객센터로 연락해 주세요.", e.getMessage());
    }

    @Test
    void cancelThrows_tellsUserToContactUs() {
        when(orderService.placeOrder(eq(1L), eq(LINES), anyString(), any(), eq("pay-1")))
                .thenThrow(new AppException("품절"));
        when(gateway.cancel("pay-1")).thenThrow(new RuntimeException("timeout"));
        AppException e = assertThrows(AppException.class, () -> controller.checkout(request(null, true)));
        assertEquals("결제 취소에 실패했습니다. 고객센터로 연락해 주세요.", e.getMessage());
    }

    @Test
    void secondCheckoutWhileInProgress_isRejected() {
        AtomicReference<String> nested = new AtomicReference<>();
        when(gateway.approve(59000L, "카드")).thenAnswer(inv -> {
            try {
                controller.checkout(request(null, true));
            } catch (AppException e) {
                nested.set(e.getMessage());
            }
            return new PaymentResult(true, "pay-1", "승인");
        });

        controller.checkout(request(null, true));

        assertEquals("결제가 진행 중입니다. 잠시만 기다려 주세요.", nested.get());
        verify(gateway, times(1)).approve(59000L, "카드");
    }

    @Test
    void afterFailure_canCheckoutAgain() {
        when(gateway.approve(59000L, "카드"))
                .thenReturn(new PaymentResult(false, null, "한도 초과"))
                .thenReturn(new PaymentResult(true, "pay-1", "승인"));
        assertThrows(AppException.class, () -> controller.checkout(request(null, true)));
        assertSame(order, controller.checkout(request(null, true)));
        verify(gateway, times(2)).approve(59000L, "카드");
    }

    @Test
    void cartClearFails_stillReturnsOrder_noRefund() {
        org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
                .when(orderService).clearOrderedFromCart(1L, LINES);
        assertSame(order, controller.checkout(request(null, true)));
        verify(gateway, never()).cancel(anyString());
    }

    @Test
    void guest_throwsLoginRequired_beforePayment() {
        session.logout();
        assertThrows(com.sadamda.common.LoginRequiredException.class,
                () -> controller.checkout(request(null, true)));
        verify(gateway, never()).approve(anyLong(), anyString());
    }
}
