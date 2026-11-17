package com.sadamda.view.product;

import com.sadamda.model.product.Product;

/**
 * 상품 카드 한 장에 보여줄 정보 (UC-04 2단계: 사진, 상품명, 가격).
 * imageUrl 은 Product.imageUrls 의 첫 번째(대표 사진).
 */
record ProductItem(Long id, String name, int price, String imageUrl) {

    static ProductItem of(Product product) {
        return new ProductItem(product.getId(), product.getName(), product.getPrice(), product.mainImageUrl());
    }
}
