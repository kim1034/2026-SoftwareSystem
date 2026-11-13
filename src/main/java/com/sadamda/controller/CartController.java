package com.sadamda.controller;

import com.sadamda.model.cart.CartItem;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.Product;

import java.util.List;

/** UC-10 장바구니·찜 관리. 모든 메서드 로그인 필요. */
public class CartController {
    private final Session session;
    private final CartService cartService;

    public CartController(Session session, CartService cartService) {
        this.session = session;
        this.cartService = cartService;
    }

    /** UC-10 1~2단계 */
    public List<CartItem> cart() {
        return cartService.getCart(memberId());
    }

    /** UC-10 2·4단계 총액 */
    public int total() {
        return cartService.getTotal(memberId());
    }

    /** UC-06 4단계 [장바구니]. 비회원이면 LoginRequiredException → 로그인 후 다시 호출 (UC-06 4a). */
    public void add(OrderLine line) {
        cartService.add(memberId(), List.of(line));
    }

    /** UC-07 5~6단계 [코디 전체 담기]. 품절이 섞여 있으면 AppException (6a). */
    public void addAll(List<OrderLine> lines) {
        cartService.add(memberId(), lines);
    }

    /** UC-10 3단계 수량·옵션 변경 */
    public void update(Long cartItemId, Long optionId, int quantity) {
        cartService.updateItem(memberId(), cartItemId, optionId, quantity);
    }

    /** UC-10 3단계 삭제 */
    public void remove(Long cartItemId) {
        cartService.removeItem(memberId(), cartItemId);
    }

    /** UC-10 1a [찜]. 결과가 찜 상태이면 true. */
    public boolean toggleWish(Long productId) {
        return cartService.toggleWish(memberId(), productId);
    }

    /** UC-10 1a 찜 목록 */
    public List<Product> wishList() {
        return cartService.getWishList(memberId());
    }

    private Long memberId() {
        return session.requireLogin().getId();
    }
}
