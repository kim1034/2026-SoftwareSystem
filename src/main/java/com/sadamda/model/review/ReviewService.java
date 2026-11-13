package com.sadamda.model.review;

import java.util.List;

/** 리뷰 로직. 담당: 백예원 */
public class ReviewService {

    /**
     * UC-14 리뷰 작성 (FR-23). 체형 정보는 회원 정보에서 자동으로 채운다.
     * 별점을 고르지 않았거나(3a) 배송 완료 전 주문이면 AppException.
     */
    public Review write(Long memberId, Long orderId, Long productId, int rating,
                        String content, List<String> photoPaths) {
        throw new UnsupportedOperationException("TODO UC-14");
    }

    /** UC-06 상품 상세의 리뷰 목록 (FR-23). */
    public List<Review> findByProduct(Long productId) {
        throw new UnsupportedOperationException("TODO UC-06");
    }
}
