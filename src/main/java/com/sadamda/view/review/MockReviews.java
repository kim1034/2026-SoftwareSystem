package com.sadamda.view.review;

import com.sadamda.common.AppException;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.order.OrderStatus;
import com.sadamda.model.review.Review;
import com.sadamda.view.order.MockOrders;
import com.sadamda.view.product.MockProducts;

import java.util.ArrayList;
import java.util.List;

/**
 * 임시 목업 리뷰. ReviewService 가 구현되면 이 파일을 지우고 context.review.write / context.product.reviews 로 바꾼다.
 * 앱을 켜 둔 동안만 새로 쓴 리뷰를 기억한다.
 */
public final class MockReviews {
    private static final List<Review> WRITTEN = new ArrayList<>();
    private static long nextId = 1000;

    private MockReviews() {
    }

    /**
     * ReviewController.write(orderId, productId, rating, content, photoPaths) 와 같은 방식.
     * 별점이 없거나(3a) 배송 완료 전 주문이거나 이미 쓴 상품이면 AppException.
     * 체형 정보는 회원 정보에서 자동으로 채운다 (Model 과 같은 방식).
     */
    public static Review write(Long memberId, BodyProfile bodyProfile, Long orderId, Long productId, int rating,
                               String content, List<String> photoPaths) {
        if (rating < 1 || rating > 5) {
            throw new AppException("별점을 골라 주세요.");
        }
        if (MockOrders.orderDetail(memberId, orderId).status() != OrderStatus.DELIVERED) {
            throw new AppException("배송이 끝난 상품만 리뷰를 쓸 수 있어요.");
        }
        if (written(orderId, productId)) {
            throw new AppException("이미 리뷰를 쓴 상품이에요.");
        }
        Review review = new Review(nextId++, memberId, productId, orderId, rating, content, List.copyOf(photoPaths),
                bodyProfile);
        WRITTEN.add(review);
        return review;
    }

    /** 이 주문의 이 상품에 리뷰를 이미 썼는지. */
    public static boolean written(Long orderId, Long productId) {
        return WRITTEN.stream().anyMatch(r -> r.orderId().equals(orderId) && r.productId().equals(productId));
    }

    /** ProductController.reviews(productId) 와 같은 역할. 새로 쓴 리뷰를 먼저, 그 뒤에 데모 리뷰. */
    public static List<Review> reviews(Long productId) {
        List<Review> result = new ArrayList<>();
        for (int i = WRITTEN.size() - 1; i >= 0; i--) {
            if (WRITTEN.get(i).productId().equals(productId)) {
                result.add(WRITTEN.get(i));
            }
        }
        result.addAll(MockProducts.reviews(productId));
        return result;
    }
}
