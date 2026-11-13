package com.sadamda.controller;

import com.sadamda.model.review.Review;
import com.sadamda.model.review.ReviewService;

import java.util.List;

/** UC-14 리뷰 작성. 로그인 필요. */
public class ReviewController {
    private final Session session;
    private final ReviewService reviewService;

    public ReviewController(Session session, ReviewService reviewService) {
        this.session = session;
        this.reviewService = reviewService;
    }

    /** UC-14 3~4단계. rating 은 1~5, 고르지 않았으면 0 → AppException (3a). */
    public Review write(Long orderId, Long productId, int rating, String content, List<String> photoPaths) {
        return reviewService.write(session.requireLogin().getId(), orderId, productId, rating, content, photoPaths);
    }
}
