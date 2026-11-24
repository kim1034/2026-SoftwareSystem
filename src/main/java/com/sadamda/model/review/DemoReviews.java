package com.sadamda.model.review;

import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;

import java.util.List;
import java.util.Map;

/** 시연용 리뷰. 상품마다 1~4개를 상품 번호로 정해서 넣어, 앱을 켤 때마다 같은 리뷰가 나온다. 담당: 백예원 */
public final class DemoReviews {
    private static final List<BodyProfile> WRITERS = List.of(
            new BodyProfile("50대", 160, "M (66)", "보통 체형"),
            new BodyProfile("60대", 155, "L (77)", "통통한 체형"),
            new BodyProfile("40대", 163, "M (66)", "마른 체형"),
            new BodyProfile("50대", 158, "L (77)", "보통 체형"),
            new BodyProfile("50대", 162, "M (66)", "하체 발달형"),
            new BodyProfile("60대", 152, "S (55)", "마른 체형"),
            new BodyProfile("70대", 150, "M (66)", "보통 체형"),
            new BodyProfile("60대", 165, "XL (88)", "상체 발달형"));

    private static final List<String> COMMON = List.of(
            "생각보다 더 마음에 들어요. 색도 사진이랑 거의 같아요.",
            "어머니 선물로 드렸는데 아주 좋아하세요.",
            "배송이 빠르고 포장도 깔끔했어요.",
            "가격 대비 품질이 좋아요. 다른 색도 사고 싶어요.");

    private static final Map<Category, List<String>> BY_CATEGORY = Map.of(
            Category.TOP, List.of("품이 넉넉해서 편하게 입어요. 평소 사이즈 그대로 샀어요.",
                    "소매가 조금 길지만 접어 입으니 괜찮아요.", "얇아 보였는데 생각보다 따뜻해요."),
            Category.BOTTOM, List.of("허리가 밴딩이라 앉아 있어도 편해요.", "길이가 적당하고 다리가 길어 보여요.",
                    "엉덩이 쪽이 조금 끼어서 한 치수 크게 살 걸 그랬어요."),
            Category.DRESS, List.of("모임에 입고 갔더니 다들 예쁘다고 했어요.", "배 부분을 잘 가려 줘서 좋아요.",
                    "길이가 무릎 아래라 단정해요."),
            Category.OUTER, List.of("가볍고 따뜻해요. 단추도 크게 달려 있어 잠그기 쉬워요.",
                    "어깨가 조금 크게 나왔지만 안에 두껍게 입기 좋아요.", "주머니가 깊어서 편해요."),
            Category.SHOES, List.of("발볼이 넓은데도 편해요. 오래 걸어도 안 아파요.", "굽이 낮아서 안정감 있어요.",
                    "반 치수 크게 나왔어요."),
            Category.BAG, List.of("가볍고 수납이 넉넉해요.", "끈 길이 조절이 돼서 좋아요."),
            Category.HAT, List.of("햇빛을 잘 가려 줘요.", "머리둘레 조절이 돼서 잘 맞아요."),
            Category.SCARF, List.of("촉감이 부드럽고 목이 따끔거리지 않아요.", "색이 고와서 어디에나 잘 어울려요."));

    private DemoReviews() {
    }

    public static void seed(ReviewService reviewService, ProductService productService) {
        for (Product product : productService.findAll()) {
            long id = product.getId();
            int count = 1 + (int) (id % 4);
            List<String> phrases = BY_CATEGORY.getOrDefault(product.getCategory(), COMMON);
            for (int i = 0; i < count; i++) {
                String content = i % 2 == 0
                        ? phrases.get((int) ((id + i) % phrases.size()))
                        : COMMON.get((int) ((id + i) % COMMON.size()));
                int rating = (id + i) % 5 == 0 ? 4 : (id + i) % 7 == 0 ? 3 : 5;
                BodyProfile writer = WRITERS.get((int) ((id * 3 + i) % WRITERS.size()));
                List<String> photos = i == 0 && product.mainImageUrl() != null && id % 3 == 0
                        ? List.of(product.mainImageUrl()) : List.of();
                reviewService.addDemoReview(id, rating, content, photos, writer);
            }
        }
    }
}
