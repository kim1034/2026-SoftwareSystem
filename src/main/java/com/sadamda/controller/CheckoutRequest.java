package com.sadamda.controller;

import com.sadamda.model.order.GiftInfo;
import com.sadamda.model.order.OrderLine;

import java.util.List;

/**
 * UC-11 4단계 [결제하기] 입력. giftInfo 가 null 이 아니면 UC-12 선물 주문이다.
 * fromCart 는 OrderSheet.fromCart() 를 그대로 넘긴다 — true 면 결제 후 주문한 상품을 장바구니에서 비운다.
 */
public record CheckoutRequest(List<OrderLine> lines, String shippingAddress, String paymentMethod,
                              GiftInfo giftInfo, boolean fromCart) {
}
