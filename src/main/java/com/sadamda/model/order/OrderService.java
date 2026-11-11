package com.sadamda.model.order;

import java.util.List;

/** 주문 저장·재고 차감·상태 변경. 결제 연동은 OrderController 가 맡는다. 담당: 백예원 */
public class OrderService {

    /** UC-11 7단계 주문 저장 및 재고 차감 (FR-19). 재고가 소진됐으면 예외 (7a). */
    public Order placeOrder(Order order) {
        throw new UnsupportedOperationException("TODO UC-11");
    }

    /** UC-13 주문 목록 (FR-21). */
    public List<Order> findByMember(Long memberId) {
        throw new UnsupportedOperationException("TODO UC-13");
    }

    /** UC-13 주문 상세. */
    public Order findById(Long orderId) {
        throw new UnsupportedOperationException("TODO UC-13");
    }

    /** UC-13 취소·교환·반품 신청 (FR-22). 배송 중이면 취소 불가 (4a). */
    public Order requestChange(Long orderId, OrderStatus requested, String reason) {
        throw new UnsupportedOperationException("TODO UC-13");
    }

    /** UC-18 상태별 주문 목록 (FR-27). */
    public List<Order> findByStatus(OrderStatus status) {
        throw new UnsupportedOperationException("TODO UC-18");
    }

    /** UC-18 주문 상태 변경 (배송준비 → 배송중 → 배송완료, 교환·반품 승인·거절). */
    public Order updateStatus(Long orderId, OrderStatus status, String trackingNumber) {
        throw new UnsupportedOperationException("TODO UC-18");
    }
}
