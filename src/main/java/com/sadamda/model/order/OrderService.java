package com.sadamda.model.order;

import java.util.List;

/** 주문 저장·재고 차감·상태 변경. 결제 연동은 OrderController 가 맡는다. 담당: 백예원 */
public class OrderService {

    /** UC-11 2단계 결제 금액 계산. 품절·판매 중지 상품이 있으면 AppException. */
    public int calculateTotal(List<OrderLine> lines) {
        throw new UnsupportedOperationException("TODO UC-11");
    }

    /**
     * UC-11 7단계 결제 승인 후 주문 저장 및 재고 차감 (FR-19).
     * 그사이 재고가 소진됐으면 AppException (7a) — Controller 가 결제를 취소한다.
     * giftInfo 가 null 이 아니면 선물 주문 (UC-12, FR-20).
     */
    public Order placeOrder(Long memberId, List<OrderLine> lines, String shippingAddress,
                            GiftInfo giftInfo, String paymentId) {
        throw new UnsupportedOperationException("TODO UC-11");
    }

    /** UC-11 장바구니에서 주문한 상품을 장바구니에서 비운다. */
    public void clearOrderedFromCart(Long memberId, List<OrderLine> lines) {
        throw new UnsupportedOperationException("TODO UC-11");
    }

    /** UC-13 주문 목록 (FR-21), 최신순. */
    public List<Order> findByMember(Long memberId) {
        throw new UnsupportedOperationException("TODO UC-13");
    }

    /** UC-13 주문 상세. 없으면 AppException. */
    public Order findById(Long orderId) {
        throw new UnsupportedOperationException("TODO UC-13");
    }

    /** UC-13 5단계 배송 전 취소. 결제 취소 성공 후 호출된다. 배송 중이면 AppException (4a). */
    public Order cancel(Long orderId, String reason) {
        throw new UnsupportedOperationException("TODO UC-13");
    }

    /** UC-13 6단계 교환·반품 신청 (FR-22). requested 는 EXCHANGE_REQUESTED 또는 RETURN_REQUESTED. */
    public Order requestChange(Long orderId, OrderStatus requested, String reason) {
        throw new UnsupportedOperationException("TODO UC-13");
    }

    /** UC-18 상태별 주문 목록 (FR-27). status 가 null 이면 전체. */
    public List<Order> findByStatus(OrderStatus status) {
        throw new UnsupportedOperationException("TODO UC-18");
    }

    /** UC-18 3단계 배송 상태 변경 (배송준비 → 배송중(송장번호) → 배송완료). 잘못된 전이면 AppException. */
    public Order updateStatus(Long orderId, OrderStatus status, String trackingNumber) {
        throw new UnsupportedOperationException("TODO UC-18");
    }

    /** UC-18 4~5단계 교환·반품 승인 결과 반영. 반품은 환불 성공 후 호출된다. */
    public Order approveChange(Long orderId) {
        throw new UnsupportedOperationException("TODO UC-18");
    }

    /** UC-18 4단계 교환·반품 거절. */
    public Order rejectChange(Long orderId, String reason) {
        throw new UnsupportedOperationException("TODO UC-18");
    }
}
