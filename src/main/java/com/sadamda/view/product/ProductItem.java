package com.sadamda.view.product;

/**
 * 상품 카드 한 장에 보여줄 정보 (UC-04 2단계: 사진, 상품명, 가격).
 * Product 에 getter 가 생기면 Product 에서 이 값을 꺼내 만든다. imageUrl 은 Product.imageUrls 의 첫 번째(대표 사진).
 */
record ProductItem(Long id, String name, int price, String imageUrl) {
}
