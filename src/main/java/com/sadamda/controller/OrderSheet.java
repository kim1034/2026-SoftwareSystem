package com.sadamda.controller;

import com.sadamda.model.order.OrderLine;

import java.util.List;

/**
 * UC-11 2단계 주문서: 주문할 상품, 기본 배송지, 결제 금액.
 * fromCart 는 장바구니에서 온 주문인지 — 결제 후 장바구니에서 비울지 정한다. CheckoutRequest 에 그대로 넘긴다.
 */
public record OrderSheet(List<OrderLine> lines, String defaultAddress, int totalPrice, boolean fromCart) {
}
