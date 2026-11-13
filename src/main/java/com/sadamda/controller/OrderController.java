package com.sadamda.controller;

import com.sadamda.common.AppException;
import com.sadamda.external.PaymentGateway;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.order.OrderStatus;

import java.util.List;

/**
 * UC-11 주문 및 결제, UC-12 선물하기, UC-13 주문·배송 조회 및 취소·교환·반품.
 * 모든 메서드 로그인 필요 (UC-11 «include» 로그인). 담당: 김대원
 */
public class OrderController {
    private final Session session;
    private final OrderService orderService;
    private final CartService cartService;
    private final MemberService memberService;
    private final PaymentGateway paymentGateway;

    public OrderController(Session session, OrderService orderService, CartService cartService,
                           MemberService memberService, PaymentGateway paymentGateway) {
        this.session = session;
        this.orderService = orderService;
        this.cartService = cartService;
        this.memberService = memberService;
        this.paymentGateway = paymentGateway;
    }

    /** UC-10 5단계 [주문하기] → UC-11 2단계 장바구니 전체로 주문서를 만든다. */
    public OrderSheet prepareFromCart() {
        Long memberId = session.requireLogin().getId();
        List<OrderLine> lines = cartService.getCart(memberId).stream()
                .map(item -> new OrderLine(item.productId(), item.optionId(), item.quantity()))
                .toList();
        if (lines.isEmpty()) {
            throw new AppException("장바구니가 비어 있습니다.");
        }
        return sheet(memberId, lines, true);
    }

    /** UC-06 4단계 [바로 구매] → UC-11 2단계 상품 하나로 주문서를 만든다. */
    public OrderSheet prepareDirect(OrderLine line) {
        Long memberId = session.requireLogin().getId();
        if (line.quantity() < 1) {
            throw new AppException("수량을 1개 이상 골라 주세요.");
        }
        return sheet(memberId, List.of(line), false);
    }

    private OrderSheet sheet(Long memberId, List<OrderLine> lines, boolean fromCart) {
        // 세션의 회원 정보는 오래됐을 수 있어 기본 배송지는 새로 읽는다.
        String defaultAddress = memberService.findById(memberId).getDefaultAddress();
        return new OrderSheet(lines, defaultAddress, orderService.calculateTotal(lines), fromCart);
    }

    /**
     * UC-11 4~8단계 [결제하기]. 결제 승인 → 주문 저장·재고 차감 → 주문 반환.
     * request.giftInfo() 가 있으면 UC-12 선물 주문.
     * AppException 메시지: 결제 실패·취소(6a, 주문 안 만듦), 재고 소진(7a, 결제 자동 취소).
     * 결제 시스템 응답을 기다리므로 View 는 별도 스레드에서 호출한다.
     */
    public Order checkout(CheckoutRequest request) {
        throw new UnsupportedOperationException("TODO UC-11, UC-12 (계획 3단계)");
    }

    /** UC-13 1~2단계 내 주문 목록, 최신순. */
    public List<Order> myOrders() {
        throw new UnsupportedOperationException("TODO UC-13 (계획 6단계)");
    }

    /** UC-13 3단계 주문 상세. 본인 주문이 아니면 AccessDeniedException. */
    public Order orderDetail(Long orderId) {
        throw new UnsupportedOperationException("TODO UC-13 (계획 6단계)");
    }

    /** UC-13 4~5단계 배송 전 취소: 결제 취소 후 "취소됨". 배송 중이면 AppException (4a). */
    public Order cancel(Long orderId, String reason) {
        throw new UnsupportedOperationException("TODO UC-13 (계획 6단계)");
    }

    /** UC-13 6단계 교환·반품 신청. requested 는 EXCHANGE_REQUESTED 또는 RETURN_REQUESTED. */
    public Order requestExchangeOrReturn(Long orderId, OrderStatus requested, String reason) {
        throw new UnsupportedOperationException("TODO UC-13 (계획 6단계)");
    }
}
