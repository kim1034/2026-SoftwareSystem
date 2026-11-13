package com.sadamda.controller;

import com.sadamda.common.AppException;
import com.sadamda.common.LoginRequiredException;
import com.sadamda.external.PaymentGateway;
import com.sadamda.model.cart.CartItem;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.member.Member;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderControllerPrepareTest {
    private final Session session = new Session();
    private final OrderService orderService = mock(OrderService.class);
    private final CartService cartService = mock(CartService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final OrderController controller = new OrderController(session, orderService, cartService,
            memberService, mock(PaymentGateway.class));

    @BeforeEach
    void loginAndStubAddress() {
        session.login(SessionTest.member(1L, Member.Role.MEMBER));
        Member fresh = mock(Member.class);
        when(fresh.getDefaultAddress()).thenReturn("서울시 중구 1");
        when(memberService.findById(1L)).thenReturn(fresh);
    }

    @Test
    void fromCart_buildsSheetWithCartLinesTotalAndDefaultAddress() {
        when(cartService.getCart(1L)).thenReturn(List.of(new CartItem(1L, 1L, 10L, 100L, 2)));
        List<OrderLine> expected = List.of(new OrderLine(10L, 100L, 2));
        when(orderService.calculateTotal(expected)).thenReturn(59000);

        OrderSheet sheet = controller.prepareFromCart();

        assertEquals(expected, sheet.lines());
        assertEquals(59000, sheet.totalPrice());
        assertEquals("서울시 중구 1", sheet.defaultAddress());
        assertTrue(sheet.fromCart());
    }

    @Test
    void fromCart_emptyCart_throws() {
        when(cartService.getCart(1L)).thenReturn(List.of());
        AppException e = assertThrows(AppException.class, controller::prepareFromCart);
        assertEquals("장바구니가 비어 있습니다.", e.getMessage());
    }

    @Test
    void direct_buildsSheetWithOneLine() {
        OrderLine line = new OrderLine(10L, 100L, 1);
        when(orderService.calculateTotal(List.of(line))).thenReturn(29500);

        OrderSheet sheet = controller.prepareDirect(line);

        assertEquals(List.of(line), sheet.lines());
        assertEquals(29500, sheet.totalPrice());
        assertEquals("서울시 중구 1", sheet.defaultAddress());
        assertFalse(sheet.fromCart());
    }

    @Test
    void direct_quantityBelowOne_throws() {
        AppException e = assertThrows(AppException.class,
                () -> controller.prepareDirect(new OrderLine(10L, 100L, 0)));
        assertEquals("수량을 1개 이상 골라 주세요.", e.getMessage());
    }

    @Test
    void guest_throwsLoginRequired() {
        session.logout();
        assertThrows(LoginRequiredException.class, controller::prepareFromCart);
    }
}
