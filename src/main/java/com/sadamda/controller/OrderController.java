package com.sadamda.controller;

import com.sadamda.external.PaymentGateway;
import com.sadamda.model.order.GiftInfo;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.order.OrderStatus;

import java.util.List;

/** UC-11 주문 및 결제, UC-12 선물하기, UC-13 주문·배송 조회 및 취소·교환·반품. 담당: 김대원 */
public class OrderController {
    private final Session session;
    private final OrderService orderService;
    private final PaymentGateway paymentGateway;

    public OrderController(Session session, OrderService orderService, PaymentGateway paymentGateway) {
        this.session = session;
        this.orderService = orderService;
        this.paymentGateway = paymentGateway;
    }

    /**
     * UC-11 4~8단계. giftInfo 가 null 이 아니면 UC-12 선물 주문이다.
     * TODO: 로그인 확인(1a, «include» UC-02) → paymentGateway.approve
     *       → 실패면 주문 만들지 않음(6a) → orderService.placeOrder
     *       → 재고 소진이면 paymentGateway.cancel(7a)
     */
    public Order checkout(Order order, String paymentMethod, GiftInfo giftInfo) {
        throw new UnsupportedOperationException("TODO UC-11, UC-12");
    }

    /** UC-13 1~2단계 */
    public List<Order> myOrders() {
        throw new UnsupportedOperationException("TODO UC-13: session 회원 id 로 orderService.findByMember 호출");
    }

    /** UC-13 3단계 */
    public Order orderDetail(Long orderId) {
        return orderService.findById(orderId);
    }

    /** UC-13 4~5단계: 배송 전 취소는 결제 취소 후 "취소됨" 처리. */
    public Order cancel(Long orderId, String reason) {
        throw new UnsupportedOperationException("TODO UC-13");
    }

    /** UC-13 6단계: 교환·반품 신청을 관리자에게 전달 (UC-18). */
    public Order requestExchangeOrReturn(Long orderId, OrderStatus requested, String reason) {
        return orderService.requestChange(orderId, requested, reason);
    }
}
