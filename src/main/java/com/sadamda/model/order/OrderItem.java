package com.sadamda.model.order;

/** 주문 상품 한 줄. */
public record OrderItem(Long productId, Long optionId, int quantity, int unitPrice) {
}
