package com.sadamda.model.review;

import com.sadamda.common.AppException;
import com.sadamda.model.TestProducts;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.order.OrderStatus;
import com.sadamda.model.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.sadamda.model.TestProducts.COAT;
import static com.sadamda.model.TestProducts.COAT_M;
import static com.sadamda.model.TestProducts.PANTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewServiceTest {
    private static final BodyProfile BODY = new BodyProfile("50대", 160, "M (66)", "보통 체형");

    private final ProductService productService = TestProducts.service();
    private final OrderService orderService = new OrderService(productService, new CartService(productService));
    private final MemberService memberService = new MemberService();
    private final ReviewService reviews = new ReviewService(orderService, memberService);
    private Long member;
    private Order order;

    @BeforeEach
    void setUp() {
        member = memberService.signUp("yewon@example.com", "password1", "백예원", "010-1234-5678", BODY).getId();
        order = orderService.placeOrder(member, List.of(new OrderLine(COAT, COAT_M, 1)), "광주", null, "pay-1",
                "카드 결제");
    }

    private void deliver() {
        orderService.updateStatus(order.getId(), OrderStatus.PREPARING, null);
        orderService.updateStatus(order.getId(), OrderStatus.SHIPPING, "1234");
        orderService.updateStatus(order.getId(), OrderStatus.DELIVERED, null);
    }

    @Test
    void write_onDeliveredOrder_fillsBodyProfile() {
        deliver();

        Review review = reviews.write(member, order.getId(), COAT, 5, "  따뜻해요  ", null);

        assertEquals("따뜻해요", review.content());
        assertEquals(BODY, review.bodyProfile());
        assertTrue(review.photoUrls().isEmpty());
        assertEquals(1, reviews.findByProduct(COAT).size());
        assertTrue(reviews.hasWritten(order.getId(), COAT));
    }

    @Test
    void writeBeforeDelivery_throws() {
        assertThrows(AppException.class, () -> reviews.write(member, order.getId(), COAT, 5, "좋아요", List.of()));
    }

    @Test
    void writeWithoutRating_throws() {
        deliver();

        AppException e = assertThrows(AppException.class,
                () -> reviews.write(member, order.getId(), COAT, 0, "좋아요", List.of()));
        assertEquals("별점을 골라 주세요.", e.getMessage());
    }

    @Test
    void writeOnSomeoneElsesOrder_throws() {
        deliver();

        assertThrows(AppException.class, () -> reviews.write(999L, order.getId(), COAT, 5, "좋아요", List.of()));
    }

    @Test
    void writeForProductNotInOrder_throws() {
        deliver();

        assertThrows(AppException.class, () -> reviews.write(member, order.getId(), PANTS, 5, "좋아요", List.of()));
    }

    @Test
    void writeTwice_throws() {
        deliver();
        reviews.write(member, order.getId(), COAT, 5, "좋아요", List.of());

        assertThrows(AppException.class, () -> reviews.write(member, order.getId(), COAT, 4, "또", List.of()));
    }

    @Test
    void findByProduct_filtersByBodyType_newestFirst() {
        reviews.addDemoReview(COAT, 5, "마른 체형 리뷰", List.of(), new BodyProfile("40대", 163, "M (66)", "마른 체형"));
        reviews.addDemoReview(COAT, 4, "보통 체형 리뷰", List.of(), BODY);
        reviews.addDemoReview(COAT, 3, "체형 정보 없음", List.of(), null);
        reviews.addDemoReview(PANTS, 5, "다른 상품", List.of(), BODY);

        assertEquals(3, reviews.findByProduct(COAT).size());
        assertEquals("체형 정보 없음", reviews.findByProduct(COAT).get(0).content());
        assertEquals(List.of("보통 체형 리뷰"),
                reviews.findByProduct(COAT, "보통 체형").stream().map(Review::content).toList());
        assertEquals(3, reviews.findByProduct(COAT, "").size());
        assertEquals(List.of("마른 체형", "보통 체형"), reviews.bodyTypes(COAT));
    }

    @Test
    void demoReviews_giveEveryProductAtLeastOneReview() {
        DemoReviews.seed(reviews, productService);

        for (var product : productService.findAll()) {
            assertFalse(reviews.findByProduct(product.getId()).isEmpty());
        }
        assertFalse(reviews.hasWritten(order.getId(), COAT));
    }
}
