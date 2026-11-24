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
    private int productTotal;
    private int shippingFee;
    private String reason;
    private String paymentMethod;

    public Order(Long id, Long memberId, List<OrderItem> items, String shippingAddress, int productTotal,
                 int shippingFee, String paymentId, String paymentMethod, GiftInfo giftInfo,
                 LocalDateTime orderedAt) {
        this.id = id;
        this.memberId = memberId;
        this.items = List.copyOf(items);
        this.shippingAddress = shippingAddress;
        this.productTotal = productTotal;
        this.shippingFee = shippingFee;
        this.totalPrice = productTotal + shippingFee;
        this.paymentId = paymentId;
        this.paymentMethod = paymentMethod;
        this.giftInfo = giftInfo;
        this.orderedAt = orderedAt;
        this.status = OrderStatus.PAID;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public int getProductTotal() {
        return productTotal;
    }

    public int getShippingFee() {
        return shippingFee;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public GiftInfo getGiftInfo() {
        return giftInfo;
    }

    public boolean isGift() {
        return giftInfo != null;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public String getReason() {
        return reason;
    }

    public String getOrderNumber() {
        return String.format("SD%1$tY%1$tm%1$td-%2$04d", orderedAt, id);
    }

    void changeStatus(OrderStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}