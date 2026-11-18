package com.sadamda.view.cart;

import com.sadamda.common.AppException;
import com.sadamda.model.cart.CartItem;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.ProductOption;
import com.sadamda.view.product.MockProducts;
import com.sadamda.view.product.ProductDetail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 임시 목업 장바구니. CartService 가 구현되면 이 파일을 지우고 context.cart 의 같은 이름 메서드로 바꾼다.
 * CartController 와 같은 방식으로 동작한다. 앱을 켜 둔 동안만 회원별로 기억한다.
 */
public final class MockCart {
    private static final Map<Long, List<CartItem>> BY_MEMBER = new HashMap<>();
    private static long nextId = 1;

    private MockCart() {
    }

    /** CartController.cart() — 담은 순서대로. */
    public static List<CartItem> cart(Long memberId) {
        return List.copyOf(items(memberId));
    }

    /** CartController.add(line) — 같은 옵션이 이미 있으면 수량을 더한다. 재고를 넘으면 AppException. */
    public static void add(Long memberId, OrderLine line) {
        List<CartItem> items = items(memberId);
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            if (item.optionId().equals(line.optionId())) {
                int quantity = item.quantity() + line.quantity();
                checkStock(line.productId(), line.optionId(), quantity);
                items.set(i, new CartItem(item.id(), memberId, item.productId(), item.optionId(), quantity));
                return;
            }
        }
        checkStock(line.productId(), line.optionId(), line.quantity());
        items.add(new CartItem(nextId++, memberId, line.productId(), line.optionId(), line.quantity()));
    }

    /**
     * CartController.update(cartItemId, optionId, quantity) — 옵션·수량을 바꾼다.
     * 바꾼 옵션이 이미 다른 줄에 있으면 한 줄로 합친다. 재고를 넘으면 AppException (UC-10 3a).
     */
    public static void update(Long memberId, Long cartItemId, Long optionId, int quantity) {
        List<CartItem> items = items(memberId);
        CartItem target = find(items, cartItemId);
        CartItem same = items.stream()
                .filter(item -> !item.id().equals(cartItemId) && item.optionId().equals(optionId))
                .findFirst().orElse(null);
        int total = quantity + (same == null ? 0 : same.quantity());
        checkStock(target.productId(), optionId, total);
        items.set(items.indexOf(target), new CartItem(target.id(), memberId, target.productId(), optionId, total));
        if (same != null) {
            items.remove(same);
        }
    }

    /** CartController.remove(cartItemId) */
    public static void remove(Long memberId, Long cartItemId) {
        items(memberId).removeIf(item -> item.id().equals(cartItemId));
    }

    private static List<CartItem> items(Long memberId) {
        return BY_MEMBER.computeIfAbsent(memberId, id -> new ArrayList<>());
    }

    private static CartItem find(List<CartItem> items, Long cartItemId) {
        return items.stream().filter(item -> item.id().equals(cartItemId)).findFirst()
                .orElseThrow(() -> new AppException("장바구니에 없는 상품입니다."));
    }

    /** UC-10 3a: 재고보다 많으면 주문 가능한 최대 수량을 알려 준다. */
    private static void checkStock(Long productId, Long optionId, int quantity) {
        ProductDetail product = MockProducts.detail(productId);
        ProductOption option = product.options().stream()
                .filter(each -> each.id().equals(optionId)).findFirst()
                .orElseThrow(() -> new AppException("판매하지 않는 옵션입니다."));
        if (option.stock() == 0) {
            throw new AppException("품절된 옵션입니다.");
        }
        if (quantity > option.stock()) {
            throw new AppException("이 옵션은 최대 " + option.stock() + "개까지 주문할 수 있어요.");
        }
    }
}
