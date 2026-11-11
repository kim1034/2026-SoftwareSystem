package com.sadamda.model.review;

import com.sadamda.model.member.BodyProfile;

import java.util.List;

/** 상품 리뷰 (UC-14). 작성자의 체형 정보를 함께 보여준다. */
public record Review(Long id, Long memberId, Long productId, Long orderId, int rating,
                     String content, List<String> photoUrls, BodyProfile bodyProfile) {
}
