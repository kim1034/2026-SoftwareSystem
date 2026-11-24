package com.sadamda.model.recommend;

import com.sadamda.common.AppException;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.product.ProductTags;
import com.sadamda.model.tryon.DressUpSlot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 상황별 코디 도우미 (UC-20): "어디 가세요?"에 답하면 상황·계절·체형에 맞는 한 벌을 3가지 골라 준다. 담당: 정우진
 *
 * 데모 데이터에는 상황·계절 정보가 없어서 상품명·세부 카테고리·소재·코디 포인트 문구의 낱말로 점수를 매긴다
 * (ProductTags 의 연령대·체형 추정과 같은 임시 규칙). 점수:
 * - 상황 낱말 1개당 3점 (최대 6점)
 * - 계절 낱말 2점, 반대 계절의 두꺼운·얇은 소재는 -4점
 * - 연령대 태그 일치 1점, 유행 아이템 1점
 * - 체형을 가리는 옷(상의·하의)은 체형 태그가 맞지 않으면 뺀다 (맞춤 추천 UC-08 과 같은 규칙)
 * 같은 점수면 많이 팔린 순, 그다음 상품 번호 순이라 같은 조건이면 늘 같은 결과가 나온다.
 */
public class OutfitHelper {
    public static final List<String> OCCASIONS = List.of("결혼식·행사", "모임·식사", "여행·나들이", "일상·산책");
    public static final List<String> SEASONS = List.of("봄", "여름", "가을", "겨울");
    /** 한 번에 보여주는 코디 수. */
    public static final int SUGGESTIONS = 3;

    private static final Map<String, List<String>> OCCASION_WORDS = Map.of(
            "결혼식·행사", List.of("클래식", "우아", "단정", "포멀", "격식", "테일러", "블라우스", "실크", "새틴",
                    "트위드", "진주", "펄", "펌프스", "슬랙스", "정장", "셋업", "드레시", "자켓"),
            "모임·식사", List.of("단정", "우아", "모임", "캐시미어", "니트", "블라우스", "로퍼", "미니멀", "클래식",
                    "세련", "귀걸이", "목걸이", "브로치", "스커트"),
            "여행·나들이", List.of("편안", "편한", "데일리", "캐주얼", "스니커즈", "와이드", "밴딩", "경쾌", "버킷",
                    "볼캡", "캡", "활동", "쿠션", "가벼", "생활 방수", "워킹"),
            "일상·산책", List.of("데일리", "캐주얼", "편안", "편한", "니트", "스니커즈", "기본", "베이직", "쿠션",
                    "밴딩", "미니멀", "티셔츠", "맨투맨", "워킹"));

    private static final Map<String, List<String>> SEASON_WORDS = Map.of(
            "봄", List.of("코튼", "면 ", "가디건", "트렌치", "셔츠", "블라우스", "가벼", "린넨"),
            "여름", List.of("린넨", "시어서커", "반팔", "쿨", "샌들", "슬리브리스", "썸머", "밀짚", "라피아", "숏"),
            "가을", List.of("니트", "울", "트렌치", "가디건", "스웨이드", "코듀로이", "트위드", "부츠", "로퍼"),
            "겨울", List.of("울", "캐시미어", "기모", "패딩", "앙고라", "알파카", "플리스", "퍼", "무스탕", "부츠", "비니", "니트"));

    /** 여름에 피할 두꺼운 소재 / 겨울에 피할 얇은 소재. */
    private static final List<String> HEAVY = List.of("캐시미어", "기모", "패딩", "앙고라", "알파카", "플리스", "퍼", "무스탕", "비니", "울");
    private static final List<String> LIGHT = List.of("린넨", "시어서커", "반팔", "슬리브리스", "샌들", "밀짚", "라피아");

    /** 상황마다 입히는 자리. 결혼식·모임은 주얼리, 여행·일상은 모자를 더한다. */
    private static final Map<String, List<Category>> PARTS = Map.of(
            "결혼식·행사", List.of(Category.TOP, Category.BOTTOM, Category.JEWELRY, Category.SHOES),
            "모임·식사", List.of(Category.TOP, Category.BOTTOM, Category.JEWELRY, Category.SHOES),
            "여행·나들이", List.of(Category.HAT, Category.TOP, Category.BOTTOM, Category.SHOES),
            "일상·산책", List.of(Category.HAT, Category.TOP, Category.BOTTOM, Category.SHOES));

    private final ProductService productService;

    public OutfitHelper(ProductService productService) {
        this.productService = productService;
    }

    /** 오늘 날짜의 계절 (3~5월 봄, 6~8월 여름, 9~11월 가을, 12~2월 겨울). 도우미가 처음 고르는 계절이다. */
    public static String seasonOf(LocalDate date) {
        int month = date.getMonthValue();
        if (month >= 3 && month <= 5) {
            return "봄";
        }
        if (month >= 6 && month <= 8) {
            return "여름";
        }
        if (month >= 9 && month <= 11) {
            return "가을";
        }
        return "겨울";
    }

    /**
     * UC-20 3~4단계: 상황·계절·체형·연령대에 맞는 코디를 최대 3가지 고른다.
     * bodyType·ageGroup 은 비어 있으면 조건 없이 고른다 (비회원·체형 미등록, 3a).
     * 상의나 하의를 하나도 고를 수 없으면 빈 목록 (4a).
     */
    public List<OutfitSuggestion> suggest(String occasion, String season, String bodyType, String ageGroup) {
        if (!OCCASIONS.contains(occasion)) {
            throw new AppException("상황을 골라 주세요.");
        }
        if (!SEASONS.contains(season)) {
            throw new AppException("계절을 골라 주세요.");
        }
        String body = ProductTags.bodyType(bodyType);
        List<Category> parts = PARTS.get(occasion);
        Map<Category, List<Product>> ranked = rankParts(occasion, season, bodyType, ageGroup);
        if (ranked.get(Category.TOP).isEmpty() || ranked.get(Category.BOTTOM).isEmpty()) {
            return List.of();
        }

        List<OutfitSuggestion> result = new ArrayList<>();
        for (int index = 0; index < SUGGESTIONS; index++) {
            List<Product> products = new ArrayList<>();
            for (Category part : parts) {
                List<Product> candidates = ranked.get(part);
                if (index < candidates.size()) {
                    products.add(candidates.get(index));
                } else if (part == Category.TOP || part == Category.BOTTOM) {
                    products.clear(); // 상의·하의가 모자라면 이 코디는 만들지 않는다
                    break;
                }
            }
            if (products.isEmpty()) {
                break;
            }
            result.add(new OutfitSuggestion(occasion + " 코디 " + (index + 1),
                    reason(products, occasion, season, body), sortBySlot(products)));
        }
        return result;
    }

    /**
     * UC-21 그날의 코디: 날짜마다 상황을 돌아가며 바꾸고(결혼식 → 모임 → 여행 → 일상), 같은 상황이 다시 오면
     * 다른 순번의 코디를 고른다. 계절은 그 날짜의 계절. 같은 날짜·체형이면 늘 같은 코디라 따로 저장하지 않는다.
     * 고를 상품이 없으면 null.
     */
    public OutfitSuggestion dailyOutfit(LocalDate date, String bodyType, String ageGroup) {
        long day = date.toEpochDay();
        String occasion = OCCASIONS.get((int) Math.floorMod(day, OCCASIONS.size()));
        String season = seasonOf(date);
        // 요일마다 순위의 다른 자리(1~7등)를 골라, 여러 상황에서 점수가 높은 옷이 며칠 연달아 나오지 않게 한다
        int position = (int) Math.floorMod(day, 7);
        Map<Category, List<Product>> ranked = rankParts(occasion, season, bodyType, ageGroup);
        List<Product> products = new ArrayList<>();
        for (Map.Entry<Category, List<Product>> entry : ranked.entrySet()) {
            List<Product> candidates = entry.getValue();
            if (!candidates.isEmpty()) {
                products.add(candidates.get(Math.min(position, candidates.size() - 1)));
            } else if (entry.getKey() == Category.TOP || entry.getKey() == Category.BOTTOM) {
                return null;
            }
        }
        String title = date.getMonthValue() + "월 " + date.getDayOfMonth() + "일 오늘의 코디 · " + occasion;
        return new OutfitSuggestion(title, reason(products, occasion, season, ProductTags.bodyType(bodyType)),
                sortBySlot(products));
    }

    /** 상황의 자리마다 점수순으로 줄 세운 상품. */
    private Map<Category, List<Product>> rankParts(String occasion, String season, String bodyType, String ageGroup) {
        String body = ProductTags.bodyType(bodyType);
        String age = ProductTags.ageGroup(ageGroup);
        Map<Category, List<Product>> ranked = new LinkedHashMap<>();
        for (Category part : PARTS.get(occasion)) {
            ranked.put(part, rank(part, occasion, season, body, age));
        }
        return ranked;
    }

    private List<Product> rank(Category part, String occasion, String season, String body, String age) {
        List<Product> candidates = productService.findAllByCategory(part).stream()
                .filter(Product::isOnSale)
                .filter(product -> !product.isSoldOut())
                .filter(product -> body == null || !coversBody(part) || product.getBodyTypeTags().contains(body))
                // 옷 입혀보기 칸에 들어갈 수 있는 것만 (종류를 알 수 없는 주얼리는 입힐 칸이 없다)
                .filter(product -> DressUpSlot.slotFor(product) != null)
                .collect(Collectors.toList());
        Map<Product, Integer> scores = new LinkedHashMap<>();
        for (Product product : candidates) {
            scores.put(product, score(product, occasion, season, age));
        }
        return candidates.stream()
                .sorted(Comparator.<Product>comparingInt(scores::get).reversed()
                        .thenComparing(Comparator.comparingInt(Product::getSalesCount).reversed())
                        .thenComparing(Product::getId))
                .collect(Collectors.toList());
    }

    static int score(Product product, String occasion, String season, String age) {
        String text = text(product);
        int score = Math.min(6, 3 * count(text, OCCASION_WORDS.get(occasion)));
        if (count(text, SEASON_WORDS.get(season)) > 0) {
            score += 2;
        }
        if ("여름".equals(season) && count(text, HEAVY) > 0) {
            score -= 4;
        }
        if ("겨울".equals(season) && count(text, LIGHT) > 0) {
            score -= 4;
        }
        if (age != null && product.getAgeGroupTags().contains(age)) {
            score += 1;
        }
        if (product.isTrending()) {
            score += 1;
        }
        return score;
    }

    /** 고른 이유: 이 코디에서 실제로 맞은 상황·계절 낱말 몇 개와 체형. */
    private static String reason(List<Product> products, String occasion, String season, String body) {
        Set<String> words = new LinkedHashSet<>();
        for (Product product : products) {
            String text = text(product);
            for (String word : OCCASION_WORDS.get(occasion)) {
                if (text.contains(word) && words.size() < 3) {
                    words.add(word.trim());
                }
            }
        }
        List<String> parts = new ArrayList<>();
        if (!words.isEmpty()) {
            parts.add(String.join("·", words));
        }
        boolean seasonal = products.stream().anyMatch(product -> count(text(product), SEASON_WORDS.get(season)) > 0);
        if (seasonal) {
            parts.add(season + "에 맞는 소재");
        }
        if (body != null) {
            parts.add(body + "에 맞춤");
        }
        return parts.isEmpty() ? "많이 찾는 상품으로 골랐어요" : String.join(" · ", parts);
    }

    private static List<Product> sortBySlot(List<Product> products) {
        List<Category> order = List.of(Category.HAT, Category.TOP, Category.BOTTOM, Category.JEWELRY, Category.SHOES);
        return products.stream()
                .sorted(Comparator.comparingInt(product -> order.indexOf(product.getCategory())))
                .collect(Collectors.toList());
    }

    private static boolean coversBody(Category category) {
        return category == Category.TOP || category == Category.BOTTOM;
    }

    private static String text(Product product) {
        return String.join(" ", nullToEmpty(product.getName()), nullToEmpty(product.getSubCategory()),
                nullToEmpty(product.getMaterial()), nullToEmpty(product.getDescription())) + " ";
    }

    private static int count(String text, List<String> words) {
        int count = 0;
        for (String word : words) {
            if (text.contains(word)) {
                count++;
            }
        }
        return count;
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
