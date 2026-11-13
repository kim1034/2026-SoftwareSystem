package com.sadamda.model.cart;

import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.Product;

import java.util.List;

/** 장바구니·찜 로직. 담당: 백예원. 실패는 AppException 으로 알린다. */
public class CartService {

    /** UC-10 장바구니 조회 (FR-17). */
    public List<CartItem> getCart(Long memberId) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-10 4단계 총액. */
    public int getTotal(Long memberId) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-06·UC-07 장바구니 담기 (FR-17). 품절 옵션이 있으면 AppException (UC-07 6a). */
    public void add(Long memberId, List<OrderLine> lines) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-10 수량·옵션 변경. 재고보다 많으면 AppException (3a). */
    public void updateItem(Long memberId, Long cartItemId, Long optionId, int quantity) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-10 항목 삭제. */
    public void removeItem(Long memberId, Long cartItemId) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-10 1a 찜 추가·해제 (FR-18). 결과가 찜 상태이면 true. */
    public boolean toggleWish(Long memberId, Long productId) {
        throw new UnsupportedOperationException("TODO UC-10 1a");
    }

    /** UC-10 1a 찜 목록 (FR-18). */
    public List<Product> getWishList(Long memberId) {
        throw new UnsupportedOperationException("TODO UC-10 1a");
    }
}
