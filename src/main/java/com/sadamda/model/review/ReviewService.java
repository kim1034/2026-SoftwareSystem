package com.sadamda.model.review;

import com.sadamda.common.AppException;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.order.OrderStatus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 리뷰 로직. 담당: 백예원 */
public class ReviewService {
    private final OrderService orderService;
    private final MemberService memberService;
    private final List<Review> reviews = new ArrayList<>();
    private long nextId = 1;

    public ReviewService(OrderService orderService, MemberService memberService) {
        this.orderService = orderService;
        this.memberService = memberService;
    }

    /**
     * UC-14 리뷰 작성 (FR-23). 체형 정보는 회원 정보에서 자동으로 채운다.
     * 별점을 고르지 않았거나(3a) 배송 완료 전 주문이면 AppException.
     */
    public synchronized Review write(Long memberId, Long orderId, Long productId, int rating,
                                     String content, List<String> photoPaths) {
        if (rating < 1 || rating > 5) {
            throw new AppException("별점을 골라 주세요.");
        }
        Order order = orderService.findById(orderId);
        if (!order.getMemberId().equals(memberId)) {
            throw new AppException("본인 주문에만 리뷰를 쓸 수 있습니다.");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException("배송이 끝난 상품만 리뷰를 쓸 수 있어요.");
        }
        if (order.getItems().stream().noneMatch(item -> item.productId().equals(productId))) {
            throw new AppException("이 주문에 없는 상품입니다.");
        }
        if (hasWritten(orderId, productId)) {
            throw new AppException("이미 리뷰를 쓴 상품이에요.");
        }
        Review review = new Review(nextId++, memberId, productId, orderId, rating,
                content == null ? "" : content.trim(),
                photoPaths == null ? List.of() : List.copyOf(photoPaths),
                memberService.findById(memberId).getBodyProfile());
        reviews.add(review);
        return review;
    }

    /** UC-06 상품 상세의 리뷰 목록 (FR-23). */
    public synchronized List<Review> findByProduct(Long productId) {
        return findByProduct(productId, null);
    }

    /** FR-23 체형별 리뷰. bodyType 이 null 이거나 비어 있으면 전체, 새로 쓴 리뷰가 먼저. */
    public synchronized List<Review> findByProduct(Long productId, String bodyType) {
        boolean all = bodyType == null || bodyType.isBlank();
        List<Review> result = new ArrayList<>();
        for (int i = reviews.size() - 1; i >= 0; i--) {
            Review review = reviews.get(i);
            if (review.productId().equals(productId) && (all || sameBodyType(review, bodyType))) {
                result.add(review);
            }
        }
        return result;
    }

    /** FR-23 이 상품 리뷰에 나오는 체형 종류. 체형별 보기 버튼을 만들 때 쓴다. */
    public synchronized List<String> bodyTypes(Long productId) {
        Set<String> types = new LinkedHashSet<>();
        for (Review review : reviews) {
            if (review.productId().equals(productId) && review.bodyProfile() != null
                    && review.bodyProfile().bodyType() != null && !review.bodyProfile().bodyType().isBlank()) {
                types.add(review.bodyProfile().bodyType());
            }
        }
        return List.copyOf(types);
    }

    public synchronized boolean hasWritten(Long orderId, Long productId) {
        return reviews.stream().anyMatch(r -> orderId != null && orderId.equals(r.orderId())
                && r.productId().equals(productId));
    }

    public synchronized Review addDemoReview(Long productId, int rating, String content, List<String> photoUrls,
                                             BodyProfile bodyProfile) {
        Review review = new Review(nextId++, null, productId, null, rating, content,
                photoUrls == null ? List.of() : List.copyOf(photoUrls), bodyProfile);
        reviews.add(review);
        return review;
    }

    private boolean sameBodyType(Review review, String bodyType) {
        return review.bodyProfile() != null && bodyType.equals(review.bodyProfile().bodyType());
    }
}
