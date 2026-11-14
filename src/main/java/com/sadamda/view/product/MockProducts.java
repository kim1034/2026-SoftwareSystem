package com.sadamda.view.product;

import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.model.review.Review;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 임시 목업 데이터. Product 에 getter 가 생기면 이 파일을 지우고
 * ProductListPanel·ProductDetailPanel 의 fetch… 메서드를 context.product 의 같은 이름 메서드로 바꾼다 (docs/product-view.md 4번).
 * 사진 주소는 팀 데모 데이터(products.tsv)에서 빌려 왔다. 원피스·아우터는 일부러 비워 UC-04 2a 를 확인한다.
 */
final class MockProducts {
    /** 한 번에 보여주는 상품 수. [더 보기]를 누를 때마다 이만큼 더 가져온다. */
    static final int PAGE_SIZE = 8;

    private static final Map<Category, List<ProductItem>> BY_CATEGORY = Map.of(
            Category.TOP, List.of(
                    new ProductItem(1L, "카멜 하우스체크 울 100% 가디건", 450000,
                            "https://nimg.lfmall.co.kr/file/product/prd/DL/2026/375/DLSW6D994CM_00.jpg?1789449085000"),
                    new ProductItem(2L, "캐시미어 블렌드 7부 니트 가디건", 398000,
                            "https://nimg.lfmall.co.kr/file/product/prd/DL/2025/375/DLSW5D923E2_00.jpg?1764758441000"),
                    new ProductItem(3L, "아이보리 스카프넥 실크 블렌드 가디건", 473000,
                            "https://nimg.lfmall.co.kr/file/product/prd/DL/2026/375/DLSW6D809IV_00.jpg?1784009623000"),
                    new ProductItem(4L, "다크브라운 브로치 장식 울혼방 블라우스", 382500,
                            "https://nimg.lfmall.co.kr/file/product/prd/DL/2026/375/DLBL6BI26BK_00.jpg?1777425517000"),
                    new ProductItem(5L, "라운드넥 트위드 가디건 베이지", 259000,
                            "https://nimg.lfmall.co.kr/file/product/prd/WS/2026/375/WSSW5DC84V2_A9.jpg?1790934445000"),
                    new ProductItem(6L, "캐시미어 블렌드 케이블 가디건 앙상블 세트", 319000,
                            "https://nimg.lfmall.co.kr/file/product/prd/0S/2026/375/0SSW6DC84U2_A1.jpg?1789626099000"),
                    new ProductItem(7L, "글리터 트위드 라운드넥 재킷형 가디건", 404100,
                            "https://image.babathe.com/goods/GC60627604/GC60627604_1.jpg/babathe/optimize"),
                    new ProductItem(8L, "울 캐시미어 벨티드 하프 코트형 가디건", 534600,
                            "https://image.babathe.com/goods/GC58617792/GC58617792_1.jpg/babathe/optimize"),
                    new ProductItem(9L, "코튼 울 반소매 풀오버 네이비", 199000,
                            "https://img.ssfshop.com/cmd/LB_750x1000/src/https://img.ssfshop.com/goods/BPBR/26/01/07/GM0026010710753_0_THNAIL_ORGINL_20260204172759400.jpg"),
                    new ProductItem(10L, "슬리브리스 허리 밴딩 페플럼 탑 화이트", 179000,
                            "https://img.ssfshop.com/cmd/LB_750x1000/src/https://img.ssfshop.com/goods/BPBR/25/04/11/GM0025041126492_0_THNAIL_ORGINL_20250422173633066.jpg")),
            Category.BOTTOM, List.of(
                    new ProductItem(11L, "울 캐시미어 스트레이트 팬츠 아이보리", 740000,
                            "https://img.ssfshop.com/cmd/LB_500x660/src/https://img.ssfshop.com/goods/WMBR/26/09/28/GM0026092835992_3_THNAIL_ORGINL_20261023114502841.jpg"),
                    new ProductItem(12L, "H라인 밴딩 스커트 블랙", 458000,
                            "https://img.ssfshop.com/cmd/LB_750x/src/https://img.ssfshop.com/goods/WMBR/26/07/31/GM0026073163660_0_THNAIL_ORGINL_20260814183308522.jpg"),
                    new ProductItem(13L, "변형 플리츠 A라인 미디 스커트 아이보리", 239000,
                            "https://img.ssfshop.com/cmd/LB_750x/src/https://img.ssfshop.com/goods/BPBR/25/04/10/GM0025041011759_0_THNAIL_ORGINL_20250422171527886.jpg"),
                    new ProductItem(14L, "클래식 플리츠 스커트 네이비", 259000,
                            "https://img.ssfshop.com/cmd/LB_750x/src/https://img.ssfshop.com/goods/BPBR/25/08/08/GM0025080802131_0_THNAIL_ORGINL_20250829173827299.jpg"),
                    new ProductItem(15L, "모던 도트 플레어 스커트 블랙", 279000,
                            "https://img.ssfshop.com/cmd/LB_750x/src/https://img.ssfshop.com/goods/BPBR/26/07/20/GM0026072051206_0_THNAIL_ORGINL_20260723131011948.jpg"),
                    new ProductItem(16L, "와이드 핏 울 블렌드 테일러드 팬츠", 380000,
                            "https://nimg.lfmall.co.kr/file/product/prd/DL/2026/375/DLPA6D917W2_00.jpg?1788942570000"),
                    new ProductItem(17L, "블랙 스트레치 일자 슬랙스", 340000,
                            "https://nimg.lfmall.co.kr/file/product/prd/DL/2025/375/DLPA5D803BK_00.jpg?1756281339000"),
                    new ProductItem(18L, "인디고 블루 일자핏 스판 데님 팬츠", 285000,
                            "https://nimg.lfmall.co.kr/file/product/prd/DL/2025/375/DLPA5CE71B2_00.jpg?1789461235000"),
                    new ProductItem(19L, "그레이 세미 와이드 울 슬랙스", 219000,
                            "https://nimg.lfmall.co.kr/file/product/prd/HS/2026/375/HSPA6D780G2_00.jpg?1789974628000"),
                    new ProductItem(20L, "네이비 스트레치 테이퍼드 슬랙스", 219000,
                            "https://nimg.lfmall.co.kr/file/product/prd/HS/2026/375/HSPA6D780N2_00.jpg?1789974596000")),
            Category.DRESS, List.of(),
            Category.OUTER, List.of());

    /** 필터·정렬에 쓰는 임시 속성. 실제로는 Product 의 태그·옵션·판매 수에서 온다. 값은 임의로 붙였다. */
    private record Attributes(Set<String> ageGroups, Set<String> bodyTypes, Set<String> sizes,
                              String color, int sales, int newness) {
    }

    private static final String SMALL = "S (55)/M (66)/L (77)";
    private static final String WIDE = "S (55)/M (66)/L (77)/XL (88)/XXL (99)";

    private static final Map<Long, Attributes> ATTRIBUTES = Map.ofEntries(
            attributes(1L, "50대/60대", "보통 체형/통통한 체형", WIDE, "베이지·브라운", 820, 3),
            attributes(2L, "40대/50대", "마른 체형/보통 체형", SMALL, "베이지·브라운", 640, 12),
            attributes(3L, "50대/60대", "마른 체형/상체 발달형", SMALL, "화이트·아이보리", 510, 18),
            attributes(4L, "40대/50대", "보통 체형/상체 발달형", WIDE, "베이지·브라운", 300, 20),
            attributes(5L, "40대", "마른 체형/보통 체형", SMALL, "베이지·브라운", 950, 9),
            attributes(6L, "50대/60대", "통통한 체형/하체 발달형", WIDE, "네이비", 270, 6),
            attributes(7L, "40대/50대", "보통 체형", SMALL, "화이트·아이보리", 430, 15),
            attributes(8L, "50대/60대", "통통한 체형/하체 발달형", WIDE, "베이지·브라운", 150, 19),
            attributes(9L, "40대/50대/60대", "보통 체형/통통한 체형", WIDE, "네이비", 1200, 2),
            attributes(10L, "40대", "마른 체형", SMALL, "화이트·아이보리", 380, 14),
            attributes(11L, "50대/60대", "하체 발달형/보통 체형", WIDE, "화이트·아이보리", 210, 17),
            attributes(12L, "40대/50대", "마른 체형/보통 체형", SMALL, "블랙", 560, 8),
            attributes(13L, "50대/60대", "하체 발달형/통통한 체형", WIDE, "화이트·아이보리", 690, 5),
            attributes(14L, "40대/50대", "보통 체형/하체 발달형", WIDE, "네이비", 720, 11),
            attributes(15L, "40대", "마른 체형/보통 체형", SMALL, "블랙", 330, 16),
            attributes(16L, "50대/60대", "통통한 체형/하체 발달형", WIDE, "그레이", 480, 13),
            attributes(17L, "40대/50대/60대", "보통 체형/하체 발달형", WIDE, "블랙", 1100, 1),
            attributes(18L, "40대/50대", "마른 체형/보통 체형", SMALL, "네이비", 600, 10),
            attributes(19L, "50대/60대", "보통 체형/통통한 체형", WIDE, "그레이", 870, 7),
            attributes(20L, "50대/60대", "통통한 체형/하체 발달형", WIDE, "네이비", 400, 4));

    private static Map.Entry<Long, Attributes> attributes(Long id, String ages, String bodyTypes, String sizes,
                                                         String color, int sales, int newness) {
        return Map.entry(id, new Attributes(Set.of(ages.split("/")), Set.of(bodyTypes.split("/")),
                Set.of(sizes.split("/")), color, sales, newness));
    }

    /** 상세 화면용 소재 (데모 데이터의 소재 칸). */
    private static final Map<Long, String> MATERIALS = Map.ofEntries(
            Map.entry(1L, "모(울) 100%"), Map.entry(2L, "캐시미어·울 혼방"), Map.entry(3L, "실크·면·울 혼방"),
            Map.entry(4L, "울 혼방 소프트 우븐"), Map.entry(5L, "코튼·울 혼방 트위드 짜임"), Map.entry(6L, "캐시미어·울 혼방"),
            Map.entry(7L, "메탈사 믹스 트위드"), Map.entry(8L, "모 83%, 캐시미어 17%"), Map.entry(9L, "면 50%, 모 50%"),
            Map.entry(10L, "면 54%, 폴리에스터 46%"), Map.entry(11L, "모 91%, 캐시미어 9%"),
            Map.entry(12L, "폴리에스터 82%, 면 13%, 마 5%"), Map.entry(13L, "면 74%, 나일론 26%"),
            Map.entry(14L, "폴리에스터 65%, 레이온 31%, 폴리우레탄 4%"), Map.entry(15L, "폴리에스터 100%"),
            Map.entry(16L, "울 블렌드 스트레치"), Map.entry(17L, "울·폴리에스터 스트레치"), Map.entry(18L, "프리미엄 스판 데님"),
            Map.entry(19L, "울 혼방 트윌"), Map.entry(20L, "울 혼방 스트레치"));

    /** 상세 화면용 색상 옵션. 몇 상품은 두 가지 색. */
    private static final Map<Long, List<String>> OPTION_COLORS = Map.ofEntries(
            Map.entry(1L, List.of("카멜", "네이비")), Map.entry(2L, List.of("베이지")), Map.entry(3L, List.of("아이보리")),
            Map.entry(4L, List.of("다크 브라운")), Map.entry(5L, List.of("베이지")), Map.entry(6L, List.of("크림 아이보리")),
            Map.entry(7L, List.of("베이지")), Map.entry(8L, List.of("카멜")), Map.entry(9L, List.of("네이비", "화이트")),
            Map.entry(10L, List.of("화이트")), Map.entry(11L, List.of("아이보리")), Map.entry(12L, List.of("블랙")),
            Map.entry(13L, List.of("아이보리")), Map.entry(14L, List.of("네이비")), Map.entry(15L, List.of("블랙 도트")),
            Map.entry(16L, List.of("웜 베이지")), Map.entry(17L, List.of("블랙", "네이비")), Map.entry(18L, List.of("인디고 블루")),
            Map.entry(19L, List.of("차콜 그레이")), Map.entry(20L, List.of("네이비")));

    private static final List<String> SIZE_ORDER = List.of("S (55)", "M (66)", "L (77)", "XL (88)", "XXL (99)");

    private MockProducts() {
    }

    /** ProductController.detail(productId) 와 같은 역할. 없는 번호면 null. */
    static ProductDetail detail(Long productId) {
        ProductItem item = BY_CATEGORY.values().stream().flatMap(List::stream)
                .filter(each -> each.id().equals(productId)).findFirst().orElse(null);
        if (item == null) {
            return null;
        }
        // 사이즈 × 색상 옵션. 품절(UC-06 4b)을 확인할 수 있게 일부 조합은 재고를 0으로 둔다.
        List<ProductOption> options = new ArrayList<>();
        List<String> colors = OPTION_COLORS.get(productId);
        long optionId = productId * 100;
        for (int s = 0; s < SIZE_ORDER.size(); s++) {
            if (!ATTRIBUTES.get(productId).sizes().contains(SIZE_ORDER.get(s))) {
                continue;
            }
            for (int c = 0; c < colors.size(); c++) {
                int stock = (productId + s + c) % 5 == 0 ? 0 : 10;
                options.add(new ProductOption(optionId++, SIZE_ORDER.get(s), colors.get(c), stock));
            }
        }
        return new ProductDetail(item.id(), item.name(), item.price(), MATERIALS.get(productId),
                List.of(item.imageUrl()), options);
    }

    /** ProductController.reviews(productId) 와 같은 역할. 몇 상품에만 리뷰가 있다. */
    static List<Review> reviews(Long productId) {
        return REVIEWS.stream().filter(review -> review.productId().equals(productId)).toList();
    }

    private static final List<Review> REVIEWS = List.of(
            new Review(1L, 101L, 1L, 1001L, 5, "따뜻하고 색이 고와요. 평소 66을 입는데 M이 딱 맞아요.",
                    List.of("https://nimg.lfmall.co.kr/file/product/prd/DL/2026/375/DLSW6D994CM_00.jpg?1789449085000"),
                    new BodyProfile("50대", 160, "M (66)", "보통 체형")),
            new Review(2L, 102L, 1L, 1002L, 4, "소매가 조금 길지만 품이 넉넉해서 편하게 입어요.", List.of(),
                    new BodyProfile("60대", 155, "L (77)", "통통한 체형")),
            new Review(3L, 103L, 1L, 1003L, 5, "어머니 선물로 드렸는데 아주 좋아하세요.", List.of(), null),
            new Review(4L, 104L, 9L, 1004L, 5, "가볍고 구김이 적어서 자주 입어요.", List.of(),
                    new BodyProfile("40대", 163, "M (66)", "마른 체형")),
            new Review(5L, 105L, 9L, 1005L, 4, "사진보다 색이 조금 진해요.", List.of(),
                    new BodyProfile("50대", 158, "L (77)", "보통 체형")),
            new Review(6L, 106L, 17L, 1006L, 5, "허리가 편하고 다리가 길어 보여요.", List.of(),
                    new BodyProfile("50대", 162, "M (66)", "하체 발달형")));

    /** ProductController.listByCategory(category, page) 와 같은 방식. page 는 0부터. */
    static List<ProductItem> listByCategory(Category category, int page) {
        List<ProductItem> all = BY_CATEGORY.get(category);
        int from = page * PAGE_SIZE;
        if (from >= all.size()) {
            return List.of();
        }
        return all.subList(from, Math.min(from + PAGE_SIZE, all.size()));
    }

    /**
     * ProductController.search(filter) 를 흉내 낸다 (FR-06, FR-07). null 인 조건은 적용하지 않는다.
     * 검색어: 띄어 쓴 낱말이 모두 상품명이나 카테고리 이름에 들어 있으면 찾은 것으로 본다.
     * 가격: minPrice 이상, maxPrice 이하. 정렬: ProductListPanel 의 SORT_* 값.
     */
    static List<ProductItem> search(SearchFilter filter) {
        List<ProductItem> result = new ArrayList<>();
        for (Category category : Category.values()) {
            for (ProductItem item : BY_CATEGORY.get(category)) {
                if (matches(item, category, filter)) {
                    result.add(item);
                }
            }
        }
        Comparator<ProductItem> order = switch (filter.sort() == null ? "" : filter.sort()) {
            case ProductListPanel.SORT_NEWEST -> Comparator.comparingInt(item -> -ATTRIBUTES.get(item.id()).newness());
            case ProductListPanel.SORT_PRICE_LOW -> Comparator.comparingInt(ProductItem::price);
            case ProductListPanel.SORT_PRICE_HIGH -> Comparator.comparingInt(item -> -item.price());
            default -> Comparator.comparingInt(item -> -ATTRIBUTES.get(item.id()).sales()); // 인기순
        };
        result.sort(order);
        return result;
    }

    private static boolean matches(ProductItem item, Category category, SearchFilter filter) {
        if (filter.keyword() != null) {
            for (String word : filter.keyword().trim().split("\\s+")) {
                if (!item.name().contains(word) && !category.label().contains(word)) {
                    return false;
                }
            }
        }
        Attributes attributes = ATTRIBUTES.get(item.id());
        return (filter.ageGroup() == null || attributes.ageGroups().contains(filter.ageGroup()))
                && (filter.bodyType() == null || attributes.bodyTypes().contains(filter.bodyType()))
                && (filter.size() == null || attributes.sizes().contains(filter.size()))
                && (filter.color() == null || attributes.color().equals(filter.color()))
                && (filter.minPrice() == null || item.price() >= filter.minPrice())
                && (filter.maxPrice() == null || item.price() <= filter.maxPrice());
    }

    /** ProductController.popular() 와 같은 역할. 상의·하의 앞쪽 상품을 섞어 보여준다. */
    static List<ProductItem> popular() {
        List<ProductItem> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            result.add(BY_CATEGORY.get(Category.TOP).get(i));
            result.add(BY_CATEGORY.get(Category.BOTTOM).get(i));
        }
        return result;
    }
}
