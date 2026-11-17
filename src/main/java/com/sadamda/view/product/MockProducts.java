package com.sadamda.view.product;

import com.sadamda.common.AppException;
import com.sadamda.controller.ProductController;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.product.Product;
import com.sadamda.model.review.Review;

import java.util.List;

/**
 * 목업 장바구니·주문·리뷰(view.cart.MockCart, view.order.MockOrders, view.review.MockReviews)가 상품을 찾는 통로.
 * 이 목업들은 context 가 없어서, MainFrame 이 시작할 때 connect 로 넘긴 ProductController(데모 데이터 products.tsv)를 쓴다.
 * 상품 목록·상세 화면은 context.product 를 바로 쓴다. 목업 장바구니·주문·리뷰가 Controller 로 바뀌면 이 파일을 지운다.
 */
public final class MockProducts {
    private static ProductController products;

    private MockProducts() {
    }

    /** MainFrame 이 시작할 때 부른다. */
    public static void connect(ProductController controller) {
        products = controller;
    }

    /** ProductController.detail(productId) 를 상세 화면 정보로. 없는 번호면 null. */
    public static ProductDetail detail(Long productId) {
        try {
            return ProductDetail.of(products.detail(productId));
        } catch (AppException e) {
            return null;
        }
    }

    /** 데모 데이터의 상품 코드(예: TOP-KR-01)로 상품 번호를 찾는다. 시연용 주문·리뷰를 같은 상품에 붙일 때 쓴다. */
    public static Long idOf(String code) {
        return products.search(null).stream().filter(product -> code.equals(product.getCode()))
                .map(Product::getId).findFirst()
                .orElseThrow(() -> new IllegalStateException("데모 데이터에 상품 " + code + " 이(가) 없습니다"));
    }

    /** 데모 리뷰. 몇 상품에만 있다. 새로 쓴 리뷰는 view.review.MockReviews 가 합쳐서 돌려준다. */
    public static List<Review> reviews(Long productId) {
        return REVIEWS.stream().filter(demo -> idOf(demo.code()).equals(productId))
                .map(demo -> new Review(demo.id(), demo.memberId(), productId, demo.orderId(), demo.rating(),
                        demo.content(), demo.photoUrls(), demo.bodyProfile()))
                .toList();
    }

    /** 상품 번호는 데이터를 읽을 때 정해지므로 상품 코드로 적어 둔다. */
    private record DemoReview(Long id, Long memberId, String code, Long orderId, int rating, String content,
                              List<String> photoUrls, BodyProfile bodyProfile) {
    }

    private static final List<DemoReview> REVIEWS = List.of(
            new DemoReview(1L, 101L, "TOP-KR-01", 1001L, 5, "따뜻하고 색이 고와요. 평소 66을 입는데 M이 딱 맞아요.",
                    List.of("https://nimg.lfmall.co.kr/file/product/prd/DL/2026/375/DLSW6D994CM_00.jpg?1789449085000"),
                    new BodyProfile("50대", 160, "M (66)", "보통 체형")),
            new DemoReview(2L, 102L, "TOP-KR-01", 1002L, 4, "소매가 조금 길지만 품이 넉넉해서 편하게 입어요.", List.of(),
                    new BodyProfile("60대", 155, "L (77)", "통통한 체형")),
            new DemoReview(3L, 103L, "TOP-KR-01", 1003L, 5, "어머니 선물로 드렸는데 아주 좋아하세요.", List.of(), null),
            new DemoReview(4L, 104L, "TOP-KR-09", 1004L, 5, "가볍고 구김이 적어서 자주 입어요.", List.of(),
                    new BodyProfile("40대", 163, "M (66)", "마른 체형")),
            new DemoReview(5L, 105L, "TOP-KR-09", 1005L, 4, "사진보다 색이 조금 진해요.", List.of(),
                    new BodyProfile("50대", 158, "L (77)", "보통 체형")),
            new DemoReview(6L, 106L, "BOT-KR-07", 1006L, 5, "허리가 편하고 다리가 길어 보여요.", List.of(),
                    new BodyProfile("50대", 162, "M (66)", "하체 발달형")));
}
