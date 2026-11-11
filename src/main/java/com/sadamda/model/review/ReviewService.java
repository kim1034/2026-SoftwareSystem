package com.sadamda.model.review;

import java.util.List;

/** 리뷰 로직. 담당: 백예원 */
public class ReviewService {

    /** UC-14 리뷰 작성 (FR-23). 별점을 고르지 않으면 예외 (3a). */
    public Review write(Review review) {
        throw new UnsupportedOperationException("TODO UC-14");
    }

    /** UC-06 상품 상세의 리뷰 목록 (FR-23). */
    public List<Review> findByProduct(Long productId) {
        throw new UnsupportedOperationException("TODO UC-06");
    }
}
