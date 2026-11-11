package com.sadamda.model.order;

/** 주문 상태 (UC-13, UC-18). */
public enum OrderStatus {
    PAID, PREPARING, SHIPPING, DELIVERED,
    CANCELED, EXCHANGE_REQUESTED, RETURN_REQUESTED, RETURNED
}
