package com.sadamda.model.order;

import java.time.LocalDateTime;
import java.util.List;

/** 주문 (UC-11~13, UC-18). giftInfo 가 있으면 선물 주문이다. */
public class Order {
    private Long id;
    private Long memberId;
    private List<OrderItem> items;
    private String shippingAddress;
    private int totalPrice;
    private String paymentId;      // 결제 시스템 승인 번호
    private String trackingNumber; // UC-18 송장번호
    private GiftInfo giftInfo;
    private OrderStatus status;
    private LocalDateTime orderedAt;

    // Controller 명세에서 쓰는 getter. 나머지 접근자는 Model 담당이 추가한다.
    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
