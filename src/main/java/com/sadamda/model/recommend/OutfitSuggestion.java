package com.sadamda.model.recommend;

import com.sadamda.model.product.Product;

import java.util.List;

/**
 * 상황별 코디 도우미(UC-20)가 골라 준 한 벌.
 *
 * @param title    예: "결혼식·행사 코디 1"
 * @param reason   고른 이유. 예: "단정·클래식 · 가을 소재 · 보통 체형에 맞춤"
 * @param products 구성 상품 (모자 → 상의 → 하의 → 주얼리 → 신발 순)
 */
public record OutfitSuggestion(String title, String reason, List<Product> products) {
    public OutfitSuggestion {
        products = List.copyOf(products);
    }

    public int totalPrice() {
        return products.stream().mapToInt(Product::getPrice).sum();
    }
}
