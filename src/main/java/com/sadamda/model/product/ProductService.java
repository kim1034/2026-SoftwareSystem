package com.sadamda.model.product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 상품 도메인 로직. 담당: 정우진
 * DB 스키마가 정해지기 전까지 메모리에 보관한다. 시작할 때 데모 데이터 300개를 읽는다.
 */
public class ProductService {
    /** UC-04 [더 보기] 한 번에 보여주는 상품 수. */
    public static final int PAGE_SIZE = 12;

    public static final String SORT_POPULAR = "인기순";
    public static final String SORT_NEWEST = "신상품순";
    public static final String SORT_PRICE_ASC = "낮은가격순";
    public static final String SORT_PRICE_DESC = "높은가격순";
    public static final List<String> SORTS = List.of(SORT_POPULAR, SORT_NEWEST, SORT_PRICE_ASC, SORT_PRICE_DESC);

    private final Map<Long, Product> products = new LinkedHashMap<>();
    private long nextProductId = 1;
    private long nextOptionId = 1;
    private LongPredicate activeOrderChecker = productId -> false;

    public ProductService() {
        this(ProductSeedLoader.load());
    }

    public ProductService(List<Product> initialProducts) {
        initialProducts.forEach(this::save);
    }

    /**
     * UC-04 카테고리별 상품 조회 (FR-05). page 는 0부터, 한 쪽에 PAGE_SIZE 개, 인기순.
     * 상품이 없으면 빈 목록이다. 이때 화면은 "준비 중인 상품입니다"와 findPopular 를 보여준다 (2a).
     */
    public synchronized List<Product> findByCategory(Category category, int page) {
        return onSale()
                .filter(product -> product.getCategory() == category)
                .sorted(comparator(SORT_POPULAR))
                .skip((long) Math.max(page, 0) * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());
    }

    /** 카테고리의 판매 중 상품 전체 (옷 입혀보기 고르기 창 등). */
    public synchronized List<Product> findAllByCategory(Category category) {
        return onSale()
                .filter(product -> product.getCategory() == category)
                .sorted(comparator(SORT_POPULAR))
                .collect(Collectors.toList());
    }

    /** 카테고리의 판매 중 상품 수. [더 보기] 버튼을 보여줄지 정할 때 쓴다. */
    public synchronized int countByCategory(Category category) {
        return (int) onSale().filter(product -> product.getCategory() == category).count();
    }

    /**
     * UC-05 상품 검색 및 필터 (FR-06, FR-07).
     * keyword 는 띄어쓰기로 나눈 모든 단어가 상품명·브랜드·카테고리(세부 카테고리 포함) 중 어딘가에 있어야 한다.
     * sort 는 SORTS 중 하나 ("가격순"은 낮은가격순). null 이면 인기순.
     */
    public synchronized List<Product> search(SearchFilter filter) {
        if (filter == null) {
            return onSale().sorted(comparator(SORT_POPULAR)).collect(Collectors.toList());
        }
        return onSale()
                .filter(product -> matchesKeyword(product, filter.keyword()))
                .filter(product -> isBlank(filter.ageGroup()) || product.getAgeGroupTags().contains(filter.ageGroup()))
                .filter(product -> isBlank(filter.bodyType()) || product.getBodyTypeTags().contains(filter.bodyType()))
                .filter(product -> isBlank(filter.size()) || product.getOptions().stream()
                        .anyMatch(option -> option.stock() > 0 && option.size().equalsIgnoreCase(filter.size().trim())))
                .filter(product -> filter.minPrice() == null || product.getPrice() >= filter.minPrice())
                .filter(product -> filter.maxPrice() == null || product.getPrice() <= filter.maxPrice())
                .filter(product -> matchesColor(product, filter.color()))
                .sorted(comparator(filter.sort()))
                .collect(Collectors.toList());
    }

    /** UC-04 2a, UC-05 2a: 보여줄 상품이 없을 때 대신 보여주는 인기 상품. */
    public synchronized List<Product> findPopular(int limit) {
        return onSale().sorted(comparator(SORT_POPULAR)).limit(limit).collect(Collectors.toList());
    }

    /** UC-05 2a: 검색 결과가 없을 때 제안할 비슷한 검색어 (글자가 겹치는 카테고리·브랜드 이름). */
    public synchronized List<String> suggestKeywords(String keyword) {
        if (isBlank(keyword)) {
            return List.of();
        }
        String key = normalize(keyword);
        Set<String> candidates = new LinkedHashSet<>();
        for (Category category : Category.values()) {
            candidates.add(category.label());
        }
        for (Product product : products.values()) {
            if (product.getSubCategory() != null) {
                // "신발(로퍼/플랫)" → 신발, 로퍼, 플랫
                for (String word : product.getSubCategory().split("[()/]")) {
                    if (!word.isBlank()) {
                        candidates.add(word.trim());
                    }
                }
            }
            if (product.getBrand() != null) {
                candidates.add(product.getBrand().replaceAll("\\s*\\(.*\\)", "").trim());
            }
        }
        return candidates.stream()
                .filter(candidate -> sharedChars(key, normalize(candidate)) >= 2)
                .limit(5)
                .collect(Collectors.toList());
    }

    /** UC-06 상품 상세 조회 (FR-08). 없는 상품이면 예외. */
    public synchronized Product findById(Long productId) {
        Product product = productId == null ? null : products.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다");
        }
        return product;
    }

    /** 상품이 있으면 돌려주고 없으면 null. 룩북 구성 상품처럼 지워졌을 수 있는 상품을 볼 때 쓴다. */
    public synchronized Product findByIdOrNull(Long productId) {
        return productId == null ? null : products.get(productId);
    }

    /** 관리자 상품 목록 (UC-16 2단계). 판매 중지 상품도 포함한다. */
    public synchronized List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    /**
     * UC-16 상품 등록·수정 (FR-25). 필수 항목이 빠지면 빠진 항목을 알려주는 예외 (5a).
     * id 가 없으면 새로 등록하고, 있으면 그 상품을 바꾼다.
     */
    public synchronized Product save(Product product) {
        List<String> missing = new ArrayList<>();
        if (product == null) {
            throw new IllegalArgumentException("상품 정보가 없습니다");
        }
        if (isBlank(product.getName())) {
            missing.add("상품명");
        }
        if (product.getCategory() == null) {
            missing.add("카테고리");
        }
        if (product.getPrice() <= 0) {
            missing.add("가격");
        }
        if (product.getOptions().isEmpty()) {
            missing.add("옵션·재고");
        }
        if (product.getImageUrls().isEmpty()) {
            missing.add("이미지");
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("다음 항목을 입력해 주세요: " + String.join(", ", missing));
        }
        if (product.getId() == null) {
            product.setId(nextProductId++);
        } else {
            nextProductId = Math.max(nextProductId, product.getId() + 1);
        }
        List<ProductOption> options = new ArrayList<>();
        for (ProductOption option : product.getOptions()) {
            if (option.id() == null) {
                options.add(new ProductOption(nextOptionId++, option.size(), option.color(), Math.max(option.stock(), 0)));
            } else {
                nextOptionId = Math.max(nextOptionId, option.id() + 1);
                options.add(option);
            }
        }
        product.setOptions(options);
        products.put(product.getId(), product);
        return product;
    }

    /**
     * UC-16 상품 삭제. 진행 중인 주문이 있으면 지우지 않고 판매 중지로 바꾼다 (3a).
     * 화면에서 먼저 물어보려면 hasActiveOrders 로 확인한다.
     */
    public synchronized void delete(Long productId) {
        Product product = findById(productId);
        if (activeOrderChecker.test(productId)) {
            product.setStatus(Product.Status.SUSPENDED);
        } else {
            products.remove(productId);
        }
    }

    /** UC-16 3a: 진행 중인 주문이 있는 상품인지. */
    public synchronized boolean hasActiveOrders(Long productId) {
        return activeOrderChecker.test(productId);
    }

    /** 주문 도메인(OrderService)이 "이 상품에 진행 중인 주문이 있는가"를 알려주는 연결점. 연결 전에는 항상 없음으로 본다. */
    public synchronized void setActiveOrderChecker(LongPredicate checker) {
        this.activeOrderChecker = checker == null ? productId -> false : checker;
    }

    /**
     * UC-11 7단계 재고 차감. 주문 도메인에서 호출한다.
     * 재고가 모자라면 아무것도 바꾸지 않고 false (7a).
     */
    public synchronized boolean decreaseStock(Long productId, Long optionId, int quantity) {
        Product product = findById(productId);
        ProductOption target = product.option(optionId);
        if (target == null || quantity <= 0 || target.stock() < quantity) {
            return false;
        }
        List<ProductOption> options = new ArrayList<>();
        for (ProductOption option : product.getOptions()) {
            options.add(option == target
                    ? new ProductOption(option.id(), option.size(), option.color(), option.stock() - quantity)
                    : option);
        }
        product.setOptions(options);
        product.setSalesCount(product.getSalesCount() + quantity);
        return true;
    }

    private Stream<Product> onSale() {
        return products.values().stream().filter(Product::isOnSale);
    }

    private static Comparator<Product> comparator(String sort) {
        Comparator<Product> newest = Comparator.comparing(Product::getId, Comparator.reverseOrder());
        if (SORT_NEWEST.equals(sort)) {
            return newest;
        }
        if (SORT_PRICE_ASC.equals(sort) || "가격순".equals(sort)) {
            return Comparator.comparingInt(Product::getPrice).thenComparing(Product::getId);
        }
        if (SORT_PRICE_DESC.equals(sort)) {
            return Comparator.comparingInt(Product::getPrice).reversed().thenComparing(Product::getId);
        }
        // 인기순: 많이 팔린 순 → 유행 아이템 → 등록 순
        return Comparator.comparingInt(Product::getSalesCount).reversed()
                .thenComparing(product -> product.isTrending() ? 0 : 1)
                .thenComparing(Product::getId);
    }

    private static boolean matchesKeyword(Product product, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        String haystack = normalize(String.join(" ",
                nullToEmpty(product.getName()), nullToEmpty(product.getBrand()),
                product.getCategory().label(), nullToEmpty(product.getSubCategory())));
        for (String word : keyword.trim().split("\\s+")) {
            if (!haystack.contains(normalize(word))) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesColor(Product product, String color) {
        if (isBlank(color)) {
            return true;
        }
        String key = normalize(color);
        return product.getOptions().stream().anyMatch(option -> normalize(option.color()).contains(key))
                || normalize(nullToEmpty(product.getMaterial())).contains(key);
    }

    private static int sharedChars(String a, String b) {
        int count = 0;
        for (int i = 0; i < a.length(); i++) {
            if (!Character.isWhitespace(a.charAt(i)) && b.indexOf(a.charAt(i)) >= 0) {
                count++;
            }
        }
        return count;
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
