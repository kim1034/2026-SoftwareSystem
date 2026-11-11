package com.sadamda.model.product;

import java.util.List;

/** 상품 도메인 로직. 담당: 정우진 */
public class ProductService {

    /** UC-04 카테고리별 상품 조회 (FR-05). */
    public List<Product> findByCategory(Category category, int page) {
        throw new UnsupportedOperationException("TODO UC-04");
    }

    /** UC-05 상품 검색 및 필터 (FR-06, FR-07). */
    public List<Product> search(SearchFilter filter) {
        throw new UnsupportedOperationException("TODO UC-05");
    }

    /** UC-06 상품 상세 조회 (FR-08). */
    public Product findById(Long productId) {
        throw new UnsupportedOperationException("TODO UC-06");
    }

    /** UC-16 상품 등록·수정 (FR-25). 필수 항목이 빠지면 예외 (5a). */
    public Product save(Product product) {
        throw new UnsupportedOperationException("TODO UC-16");
    }

    /** UC-16 상품 삭제. 진행 중인 주문이 있으면 판매 중지로 바꾼다 (3a). */
    public void delete(Long productId) {
        throw new UnsupportedOperationException("TODO UC-16");
    }
}
