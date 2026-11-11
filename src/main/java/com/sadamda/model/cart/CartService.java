package com.sadamda.model.cart;

import java.util.List;

/** 장바구니·찜 로직. 담당: 백예원 */
public class CartService {

    /** UC-10 장바구니 조회 (FR-17). */
    public List<CartItem> getCart(Long memberId) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-06·UC-07 장바구니 담기 (FR-17). */
    public void addToCart(Long memberId, Long productId, Long optionId, int quantity) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-10 수량·옵션 변경. 재고보다 많으면 예외 (3a). */
    public void updateItem(Long cartItemId, Long optionId, int quantity) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-10 항목 삭제. */
    public void removeItem(Long cartItemId) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-10 1a 찜 추가·해제 (FR-18). */
    public void toggleWish(Long memberId, Long productId) {
        throw new UnsupportedOperationException("TODO UC-10 1a");
    }

    /** UC-10 1a 찜 목록 (FR-18). */
    public List<Long> getWishList(Long memberId) {
        throw new UnsupportedOperationException("TODO UC-10 1a");
    }
}
