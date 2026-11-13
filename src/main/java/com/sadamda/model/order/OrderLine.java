package com.sadamda.model.order;

/** 담기·주문할 상품 한 줄 (상품, 옵션, 수량). 장바구니 담기와 주문서 작성에 같이 쓴다. */
public record OrderLine(Long productId, Long optionId, int quantity) {
}
