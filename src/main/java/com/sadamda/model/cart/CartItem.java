package com.sadamda.model.cart;

/** 장바구니 항목 (UC-10). */
public record CartItem(Long id, Long memberId, Long productId, Long optionId, int quantity) {
}
