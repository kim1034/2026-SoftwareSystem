package com.sadamda.view.product;

import com.sadamda.model.product.Product;

/**
 * 상품 카드 한 장에 보여줄 정보 (UC-04 2단계: 사진, 브랜드, 상품명, 가격).
 * brand 는 카드에서 짧게 보여주는 이름("구호 플러스 (kuho plus)" → "구호 플러스"), imageUrl 은 Product.imageUrls 의 첫 번째(대표 사진).
 */
record ProductItem(Long id, String brand, String name, int price, String imageUrl) {

    static ProductItem of(Product product) {
        return new ProductItem(product.getId(), ShopProductCard.shortBrand(product.getBrand()), product.getName(),
                product.getPrice(), product.mainImageUrl());
    }
}
