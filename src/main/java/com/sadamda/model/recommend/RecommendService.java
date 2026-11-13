package com.sadamda.model.recommend;

import com.sadamda.common.AppException;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 룩북·유행·맞춤 추천 로직. 담당: 정우진
 * 상품은 ProductService 에서 가져온다. 룩북은 DB 스키마가 정해지기 전까지 메모리에 보관한다.
 */
public class RecommendService {
    /** UC-08 "나에게 어울리는 상품"에 보여주는 상품 수. */
    public static final int RECOMMEND_LIMIT = 12;
    /** 추천 결과가 한 카테고리로 쏠리지 않게 하는 카테고리별 최대 개수. */
    private static final int MAX_PER_CATEGORY = 3;

    private final ProductService productService;
    private final Map<Long, Lookbook> lookbooks = new LinkedHashMap<>();
    private long nextLookbookId = 1;

    public RecommendService(ProductService productService) {
        this.productService = productService;
        seedDemoLookbooks();
    }

    /** UC-07 룩북 목록 (FR-09). occasion·season 이 null 이면 전체. */
    public synchronized List<Lookbook> findLookbooks(String occasion, String season) {
        return lookbooks.values().stream()
                .filter(lookbook -> isBlank(occasion) || occasion.equals(lookbook.getOccasion()))
                .filter(lookbook -> isBlank(season) || season.equals(lookbook.getSeason()))
                .collect(Collectors.toList());
    }

    /** UC-07 2단계 상황 필터에 보여줄 값 (등록된 코디에 있는 것만). */
    public synchronized List<String> occasions() {
        Set<String> values = new LinkedHashSet<>();
        lookbooks.values().forEach(lookbook -> values.add(lookbook.getOccasion()));
        values.remove(null);
        return new ArrayList<>(values);
    }

    /** UC-07 2단계 계절 필터에 보여줄 값 (등록된 코디에 있는 것만). */
    public synchronized List<String> seasons() {
        Set<String> values = new LinkedHashSet<>();
        lookbooks.values().forEach(lookbook -> values.add(lookbook.getSeason()));
        values.remove(null);
        return new ArrayList<>(values);
    }

    /** UC-07 코디 상세와 구성 상품 (FR-10). 없는 코디면 예외. */
    public synchronized Lookbook findLookbook(Long lookbookId) {
        Lookbook lookbook = lookbookId == null ? null : lookbooks.get(lookbookId);
        if (lookbook == null) {
            throw new AppException("코디를 찾을 수 없습니다.");
        }
        return lookbook;
    }

    /** UC-07 4단계 구성 상품 목록. 지워진 상품은 빠진다. 품절 여부는 Product.isSoldOut 으로 본다 (6a). */
    public synchronized List<Product> findLookbookProducts(Long lookbookId) {
        List<Product> result = new ArrayList<>();
        for (Long productId : findLookbook(lookbookId).getProductIds()) {
            Product product = productService.findByIdOrNull(productId);
            if (product != null) {
                result.add(product);
            }
        }
        return result;
    }

    /** UC-08 이번 시즌 유행 아이템 (FR-11). 판매 중지 상품은 빠진다. */
    public List<Product> findTrendingItems() {
        return productService.findAll().stream()
                .filter(Product::isOnSale)
                .filter(Product::isTrending)
                .collect(Collectors.toList());
    }

    /**
     * UC-08 연령대·체형 맞춤 추천 (FR-12).
     * 점수 = 체형 태그 일치 3점 + 연령대 태그 일치 2점 + 유행 아이템 1점. 같은 점수면 많이 팔린 순.
     * 체형 정보가 없으면 연령대 인기 상품을 보여준다 (3a). 이때 화면은 hasBodyType 으로 확인해 체형 입력을 권한다.
     */
    public List<Product> recommendFor(Member member) {
        BodyProfile profile = member == null ? null : member.getBodyProfile();
        String ageGroup = profile == null ? null : profile.ageGroup();
        String bodyType = profile == null ? null : profile.bodyType();

        List<Product> candidates = productService.findAll().stream()
                .filter(Product::isOnSale)
                .filter(product -> !product.isSoldOut())
                .collect(Collectors.toList());

        if (isBlank(bodyType)) {
            // 3a: 연령대 태그가 맞는 상품을 인기순으로. 연령대도 없으면 전체 인기 상품.
            List<Product> byAge = candidates.stream()
                    .filter(product -> isBlank(ageGroup) || product.getAgeGroupTags().contains(ageGroup))
                    .sorted(Comparator.comparingInt(Product::getSalesCount).reversed()
                            .thenComparing(product -> product.isTrending() ? 0 : 1)
                            .thenComparing(Product::getId))
                    .collect(Collectors.toList());
            return spread(byAge.isEmpty() ? productService.findPopular(RECOMMEND_LIMIT * 4) : byAge);
        }

        Map<Product, Integer> scores = new LinkedHashMap<>();
        for (Product product : candidates) {
            int score = 0;
            // 체형을 가리는 옷(상의·하의·원피스·아우터)에서 태그가 맞을 때만 체형 점수를 준다
            if (isClothing(product) && product.getBodyTypeTags().contains(bodyType)) {
                score += 3;
            }
            if (!isBlank(ageGroup) && product.getAgeGroupTags().contains(ageGroup)) {
                score += 2;
            }
            if (product.isTrending()) {
                score += 1;
            }
            // 체형이 맞지 않는 옷은 추천하지 않는다
            if (isClothing(product) && !product.getBodyTypeTags().contains(bodyType)) {
                continue;
            }
            scores.put(product, score);
        }
        List<Product> ranked = scores.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> -entry.getKey().getSalesCount())
                        .thenComparing(entry -> entry.getKey().getId()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return spread(ranked);
    }

    /** UC-08 3a: 체형 정보를 등록한 회원인지. 아니면 화면에서 체형 정보 입력(UC-03)을 권한다. */
    public boolean hasBodyType(Member member) {
        return member != null && member.getBodyProfile() != null && !isBlank(member.getBodyProfile().bodyType());
    }

    /**
     * UC-17 코디 등록·수정 (FR-26). 실패하면 AppException:
     * 제목이나 구성 상품이 없을 때, 없는 상품이 들어 있을 때, 판매 중지 상품이 들어 있을 때 (2a, 메시지가 곧 경고 문구).
     */
    public synchronized Lookbook saveLookbook(Lookbook lookbook) {
        if (lookbook == null || isBlank(lookbook.getTitle())) {
            throw new AppException("코디 제목을 입력해 주세요.");
        }
        if (lookbook.getProductIds().isEmpty()) {
            throw new AppException("구성 상품을 1개 이상 골라 주세요.");
        }
        for (Long productId : lookbook.getProductIds()) {
            if (productService.findByIdOrNull(productId) == null) {
                throw new AppException("구성 상품 중 없는 상품이 있습니다.");
            }
        }
        List<String> warnings = lookbookWarnings(lookbook);
        if (!warnings.isEmpty()) {
            throw new AppException(String.join("\n", warnings) + "\n판매 중지된 상품을 빼고 다시 저장해 주세요.");
        }
        if (lookbook.getId() == null) {
            lookbook.setId(nextLookbookId++);
        } else {
            nextLookbookId = Math.max(nextLookbookId, lookbook.getId() + 1);
        }
        lookbooks.put(lookbook.getId(), lookbook);
        return lookbook;
    }

    /** UC-17 2a: 구성 상품 중 판매 중지된 상품에 대한 경고 문구. 없으면 빈 목록. */
    public List<String> lookbookWarnings(Lookbook lookbook) {
        List<String> warnings = new ArrayList<>();
        for (Long productId : lookbook.getProductIds()) {
            Product product = productService.findByIdOrNull(productId);
            if (product != null && !product.isOnSale()) {
                warnings.add("판매 중지된 상품입니다: " + product.getName());
            }
        }
        return warnings;
    }

    /** UC-17 코디 삭제 (FR-26). */
    public synchronized void deleteLookbook(Long lookbookId) {
        findLookbook(lookbookId);
        lookbooks.remove(lookbookId);
    }

    /** UC-17 유행 아이템 지정·해제 (FR-26). */
    public void setTrending(Long productId, boolean trending) {
        productService.findById(productId).setTrending(trending);
    }

    /** 순위는 지키면서 카테고리별 최대 개수를 넘지 않게 RECOMMEND_LIMIT 개를 고른다. */
    private static List<Product> spread(List<Product> ranked) {
        Map<Category, Integer> counts = new EnumMap<>(Category.class);
        List<Product> result = new ArrayList<>();
        for (Product product : ranked) {
            int count = counts.getOrDefault(product.getCategory(), 0);
            if (count < MAX_PER_CATEGORY) {
                counts.put(product.getCategory(), count + 1);
                result.add(product);
                if (result.size() == RECOMMEND_LIMIT) {
                    break;
                }
            }
        }
        return result;
    }

    private static boolean isClothing(Product product) {
        Category category = product.getCategory();
        return category == Category.TOP || category == Category.BOTTOM
                || category == Category.DRESS || category == Category.OUTER;
    }

    /**
     * 데모용 코디. 데모 데이터의 국가별 트렌드 요약을 제목으로 쓰고,
     * 그 나라 브랜드의 상의·하의·신발·모자·주얼리 첫 상품을 묶는다. 관리자가 UC-17 로 바꿀 수 있다.
     */
    private void seedDemoLookbooks() {
        String[][] demos = {
                {"한국", "올드머니 미니멀 모임 코디", "모임", "가을"},
                {"미국", "포근한 캐시미어 일상 코디", "일상", "겨울"},
                {"영국", "브리티시 헤리티지 여행 코디", "여행", "가을"},
                {"프랑스", "프렌치 시크 일상 코디", "일상", "가을"},
                {"이탈리아", "콰이어트 럭셔리 모임 코디", "모임", "겨울"},
                {"일본", "오토나 캐주얼 여행 코디", "여행", "겨울"},
        };
        List<Product> all = productService.findAll();
        Category[] parts = {Category.TOP, Category.BOTTOM, Category.SHOES, Category.HAT, Category.JEWELRY};
        for (String[] demo : demos) {
            List<Long> productIds = new ArrayList<>();
            String imageUrl = null;
            for (Category part : parts) {
                Product pick = all.stream()
                        .filter(product -> demo[0].equals(product.getCountry()) && product.getCategory() == part)
                        .findFirst().orElse(null);
                if (pick != null) {
                    productIds.add(pick.getId());
                    if (imageUrl == null) {
                        imageUrl = pick.mainImageUrl();
                    }
                }
            }
            if (productIds.size() >= 2) {
                saveLookbook(new Lookbook(demo[1], imageUrl, demo[2], demo[3], productIds));
            }
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
