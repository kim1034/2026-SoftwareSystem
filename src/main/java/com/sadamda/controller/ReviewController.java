package com.sadamda.controller;

import com.sadamda.model.review.Review;
import com.sadamda.model.review.ReviewService;

/** UC-14 리뷰 작성. */
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** UC-14 */
    public Review write(Review review) {
        return reviewService.write(review);
    }
}
