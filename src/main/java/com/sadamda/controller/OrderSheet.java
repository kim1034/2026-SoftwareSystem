package com.sadamda.controller;

import com.sadamda.model.order.OrderLine;

import java.util.List;

/** UC-11 2단계 주문서: 주문할 상품, 기본 배송지, 결제 금액. */
public record OrderSheet(List<OrderLine> lines, String defaultAddress, int totalPrice) {
}
