package com.sadamda.model.product;

import java.util.List;

/** 상품 도메인 로직. 담당: 정우진. 실패는 AppException 으로 알린다. */
public class ProductService {

    /** UC-04 카테고리별 상품 조회 (FR-05). page 는 0부터. */
    public List<Product> findByCategory(Category category, int page) {
        throw new UnsupportedOperationException("TODO UC-04");
    }

    /** UC-04 2a, UC-05 2a 상품이 없을 때 대신 보여줄 인기 상품. */
    public List<Product> findPopular(int limit) {
        throw new UnsupportedOperationException("TODO UC-04 2a");
    }

    /** UC-05 상품 검색 및 필터 (FR-06, FR-07). */
    public List<Product> search(SearchFilter filter) {
        throw new UnsupportedOperationException("TODO UC-05");
    }

    /** UC-06 상품 상세 조회 (FR-08). 없으면 AppException. */
    public Product findById(Long productId) {
        throw new UnsupportedOperationException("TODO UC-06");
    }

    /** UC-16 2단계 관리자용 전체 상품 목록 (판매 중지 포함). */
    public List<Product> findAll() {
        throw new UnsupportedOperationException("TODO UC-16");
    }

    /** UC-16 상품 등록·수정 (FR-25). 필수 항목이 빠지면 AppException (5a). */
    public Product save(Product product) {
        throw new UnsupportedOperationException("TODO UC-16");
    }

    /** UC-16 3a 진행 중인 주문이 있는지. 있으면 삭제 대신 판매 중지를 권한다. */
    public boolean hasActiveOrders(Long productId) {
        throw new UnsupportedOperationException("TODO UC-16 3a");
    }

    /** UC-16 3a 판매 중지. */
    public void suspend(Long productId) {
        throw new UnsupportedOperationException("TODO UC-16 3a");
    }

    /** UC-16 상품 삭제. */
    public void delete(Long productId) {
        throw new UnsupportedOperationException("TODO UC-16");
    }
}
