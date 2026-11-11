package com.sadamda.controller;

import com.sadamda.model.cart.CartItem;
import com.sadamda.model.cart.CartService;

import java.util.List;

/** UC-10 장바구니·찜 관리. */
public class CartController {
    private final Session session;
    private final CartService cartService;

    public CartController(Session session, CartService cartService) {
        this.session = session;
        this.cartService = cartService;
    }

    /** UC-10 1~2단계 */
    public List<CartItem> cart() {
        throw new UnsupportedOperationException("TODO UC-10: session 회원 id 로 cartService.getCart 호출");
    }

    /** UC-06·UC-07 장바구니 담기. 비회원이면 로그인(UC-02) 후 이어간다. */
    public void add(Long productId, Long optionId, int quantity) {
        throw new UnsupportedOperationException("TODO UC-10");
    }

    /** UC-10 3단계 */
    public void update(Long cartItemId, Long optionId, int quantity) {
        cartService.updateItem(cartItemId, optionId, quantity);
    }

    /** UC-10 3단계 */
    public void remove(Long cartItemId) {
        cartService.removeItem(cartItemId);
    }

    /** UC-10 1a 찜 */
    public void toggleWish(Long productId) {
        throw new UnsupportedOperationException("TODO UC-10 1a");
    }
}
