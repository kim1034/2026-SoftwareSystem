package com.sadamda.model.cart;

import com.sadamda.common.AppException;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.product.ProductService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 장바구니·찜 로직. 담당: 백예원. 실패는 AppException 으로 알린다. */
public class CartService {
    private final ProductService productService;
    private final Map<Long, List<CartItem>> carts = new HashMap<>();
    private final Map<Long, Set<Long>> wishes = new HashMap<>();
    private long nextId = 1;

    public CartService(ProductService productService) {
        this.productService = productService;
    }

    /** UC-10 장바구니 조회 (FR-17). */
    public synchronized List<CartItem> getCart(Long memberId) {
        return List.copyOf(items(memberId));
    }

    /** UC-10 4단계 총액. */
    public synchronized int getTotal(Long memberId) {
        int total = 0;
        for (CartItem item : items(memberId)) {
            Product product = productService.findByIdOrNull(item.productId());
            if (product != null) {
                total += product.getPrice() * item.quantity();
            }
        }
        return total;
    }

    /** UC-06·UC-07 장바구니 담기 (FR-17). 품절 옵션이 있으면 AppException (UC-07 6a). */
    public synchronized void add(Long memberId, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new AppException("담을 상품이 없습니다.");
        }
        Map<Long, Integer> wanted = new HashMap<>();
        for (CartItem item : items(memberId)) {
            wanted.put(item.optionId(), item.quantity());
        }
        for (OrderLine line : lines) {
            if (line.quantity() < 1) {
                throw new AppException("수량을 1개 이상 골라 주세요.");
            }
            int quantity = wanted.getOrDefault(line.optionId(), 0) + line.quantity();
            checkStock(line.productId(), line.optionId(), quantity);
            wanted.put(line.optionId(), quantity);
        }
        List<CartItem> items = items(memberId);
        for (OrderLine line : lines) {
            int index = indexOfOption(items, line.optionId());
            if (index >= 0) {
                CartItem same = items.get(index);
                items.set(index, new CartItem(same.id(), memberId, same.productId(), same.optionId(),
                        same.quantity() + line.quantity()));
            } else {
                items.add(new CartItem(nextId++, memberId, line.productId(), line.optionId(), line.quantity()));
            }
        }
    }

    /** UC-10 수량·옵션 변경. 재고보다 많으면 AppException (3a). */
    public synchronized void updateItem(Long memberId, Long cartItemId, Long optionId, int quantity) {
        if (quantity < 1) {
            throw new AppException("수량을 1개 이상 골라 주세요.");
        }
        List<CartItem> items = items(memberId);
        CartItem target = find(items, cartItemId);
        CartItem same = null;
        for (CartItem item : items) {
            if (!item.id().equals(cartItemId) && item.optionId().equals(optionId)) {
                same = item;
            }
        }
        int total = quantity + (same == null ? 0 : same.quantity());
        checkStock(target.productId(), optionId, total);
        items.set(items.indexOf(target), new CartItem(target.id(), memberId, target.productId(), optionId, total));
        if (same != null) {
            items.remove(same);
        }
    }

    /** UC-10 항목 삭제. */
    public synchronized void removeItem(Long memberId, Long cartItemId) {
        items(memberId).removeIf(item -> item.id().equals(cartItemId));
    }

    public synchronized void removeOrdered(Long memberId, List<OrderLine> lines) {
        for (OrderLine line : lines) {
            items(memberId).removeIf(item -> item.optionId().equals(line.optionId()));
        }
    }

    /** UC-10 1a 찜 추가·해제 (FR-18). 결과가 찜 상태이면 true. */
    public synchronized boolean toggleWish(Long memberId, Long productId) {
        productService.findById(productId);
        Set<Long> wished = wishes.computeIfAbsent(memberId, id -> new LinkedHashSet<>());
        if (wished.remove(productId)) {
            return false;
        }
        wished.add(productId);
        return true;
    }

    public synchronized boolean isWished(Long memberId, Long productId) {
        return wishes.getOrDefault(memberId, Set.of()).contains(productId);
    }

    /** UC-10 1a 찜 목록 (FR-18). */
    public synchronized List<Product> getWishList(Long memberId) {
        List<Product> result = new ArrayList<>();
        for (Long productId : wishes.getOrDefault(memberId, Set.of())) {
            Product product = productService.findByIdOrNull(productId);
            if (product != null) {
                result.add(0, product);
            }
        }
        return result;
    }

    private List<CartItem> items(Long memberId) {
        return carts.computeIfAbsent(memberId, id -> new ArrayList<>());
    }

    private int indexOfOption(List<CartItem> items, Long optionId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).optionId().equals(optionId)) {
                return i;
            }
        }
        return -1;
    }

    private CartItem find(List<CartItem> items, Long cartItemId) {
        for (CartItem item : items) {
            if (item.id().equals(cartItemId)) {
                return item;
            }
        }
        throw new AppException("장바구니에 없는 상품입니다.");
    }

    private void checkStock(Long productId, Long optionId, int quantity) {
        Product product = productService.findById(productId);
        if (!product.isOnSale()) {
            throw new AppException("'" + product.getName() + "'은(는) 판매가 중지된 상품입니다.");
        }
        ProductOption option = product.option(optionId);
        if (option == null) {
            throw new AppException("판매하지 않는 옵션입니다.");
        }
        if (option.stock() == 0) {
            throw new AppException("'" + product.getName() + "' " + option.size() + "/" + option.color() + "은(는) 품절입니다.");
        }
        if (quantity > option.stock()) {
            throw new AppException("이 옵션은 최대 " + option.stock() + "개까지 주문할 수 있어요.");
        }
    }
}