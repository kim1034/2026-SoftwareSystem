package com.sadamda;

import com.sadamda.external.DressUpClients;
import com.sadamda.external.DressUpException;
import com.sadamda.external.DressUpImageClient;
import com.sadamda.external.DressUpSettings;
import com.sadamda.external.GeminiDressUpClient;
import com.sadamda.external.RemoteImages;
import com.sadamda.external.UnconnectedDressUpClient;
import com.sadamda.external.OpenAiDressUpClient;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.model.recommend.RecommendService;
import com.sadamda.model.tryon.CharacterFigure;
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.model.tryon.TryOnImage;
import com.sadamda.model.tryon.TryOnService;
import com.sadamda.common.AppException;
import com.sadamda.controller.AdminController;
import com.sadamda.controller.AttendanceController;
import com.sadamda.controller.CodiBookController;
import com.sadamda.controller.OrderController;
import com.sadamda.model.attendance.AttendanceService;
import com.sadamda.model.attendance.AttendanceWeek;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.recommend.OutfitHelper;
import com.sadamda.model.recommend.OutfitSuggestion;
import com.sadamda.model.tryon.CodiBookService;
import com.sadamda.model.tryon.SavedCodi;
import com.sadamda.controller.ProductController;
import com.sadamda.controller.RecommendController;
import com.sadamda.controller.Session;
import com.sun.net.httpserver.HttpServer;

import java.awt.image.BufferedImage;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 상품·추천·보관함 도메인과 옷 입혀보기 연동을 한 번에 점검하는 실행형 검사.
 * 외부 라이브러리 없이 돌아간다: java -cp target/classes:target/test-classes com.sadamda.DomainSelfCheck
 * (인터넷이나 AI 이용 열쇠가 필요 없다. Gemini 연동은 이 PC 에 띄운 가짜 서버로 요청·응답 형식만 확인한다.)
 */
public class DomainSelfCheck {
    private static final int DEMO_PRODUCTS = 370;
    private static int checks;

    public static void main(String[] args) throws Exception {
        products();
        recommend();
        controllers();
        locker();
        codiBook();
        outfitHelper();
        attendance();
        dressUp();
        openAiRelay();
        character();
        System.out.println("모두 통과: " + checks + "개 확인");
    }

    private static void products() {
        ProductService service = new ProductService();
        check(service.findAll().size() == DEMO_PRODUCTS, "데모 상품 " + DEMO_PRODUCTS + "개");
        check(service.countByCategory(Category.BOTTOM) == 35, "하의 35개");
        check(service.findAll().stream().allMatch(p -> p.mainImageUrl() != null && !p.mainImageUrl().isBlank()),
                "모든 상품에 사진이 있음");
        check(service.countByCategory(Category.SHOES) == 105, "신발 105개");
        check(service.countByCategory(Category.JEWELRY) == 105, "주얼리 105개");
        check(service.findByCategory(Category.TOP, 0).size() == ProductService.PAGE_SIZE, "UC-04 첫 쪽 8개");
        check(service.findByCategory(Category.TOP, 4).size() == 3, "UC-04 다섯째 쪽 3개 (35개 중)");
        check(new ProductService(new java.util.ArrayList<>()).findByCategory(Category.DRESS, 0).isEmpty(),
                "UC-04 2a 상품이 없는 카테고리는 빈 목록");
        check(service.findPopular(8).size() == 8, "UC-04 2a 인기 상품");

        List<Product> loafers = service.search(new SearchFilter("로퍼", null, null, null, null, null, null, null));
        check(!loafers.isEmpty() && loafers.stream().allMatch(p -> p.getCategory() == Category.SHOES), "UC-05 키워드 검색");
        check(service.search(new SearchFilter("신발", null, null, null, null, null, null, null)).size() == 105, "UC-05 카테고리 이름 검색");

        List<Product> cheap = service.search(new SearchFilter(null, null, null, null, null, 100000, null, ProductService.SORT_PRICE_ASC));
        check(!cheap.isEmpty() && cheap.stream().allMatch(p -> p.getPrice() <= 100000), "UC-05 가격대 필터");
        check(cheap.get(0).getPrice() <= cheap.get(cheap.size() - 1).getPrice(), "UC-05 낮은가격순");
        List<Product> newest = service.search(new SearchFilter(null, null, null, null, null, null, null, ProductService.SORT_NEWEST));
        check(newest.get(0).getId() > newest.get(1).getId(), "UC-05 신상품순");
        List<Product> navy = service.search(new SearchFilter(null, null, null, null, null, null, "네이비", null));
        check(!navy.isEmpty(), "UC-05 색상 필터");
        List<Product> size240 = service.search(new SearchFilter(null, null, null, "240", null, null, null, null));
        check(size240.size() == 105, "UC-05 사이즈 필터 (240 은 신발만)");
        List<Product> pear = service.search(new SearchFilter(null, "50대", "하체 발달형", null, null, null, null, null));
        check(!pear.isEmpty() && pear.size() < DEMO_PRODUCTS, "UC-05 연령대·체형 필터");
        check(service.findAll().stream().anyMatch(p -> p.getAgeGroupTags().size() < 3)
                && service.findAll().stream().allMatch(p -> !p.getAgeGroupTags().isEmpty()), "연령대 태그가 상품마다 다름 (빈 태그 없음)");
        int forties = service.search(new SearchFilter(null, "40대", null, null, null, null, null, null)).size();
        int sixties = service.search(new SearchFilter(null, "60대", null, null, null, null, null, null)).size();
        check(forties > 0 && sixties > 0 && forties != sixties && forties < DEMO_PRODUCTS, "UC-05 연령대 필터가 결과를 바꿈");
        // 상품 목록 화면(강준서, ProductListPanel)이 보내는 값과 맞는지
        List<Product> byCode = service.search(new SearchFilter(null, null, null, null, null, null, null, "PRICE_LOW"));
        check(byCode.get(0).getPrice() <= byCode.get(byCode.size() - 1).getPrice(), "화면 정렬 코드 PRICE_LOW");
        List<Product> byCodeHigh = service.search(new SearchFilter(null, null, null, null, null, null, null, "PRICE_HIGH"));
        check(byCodeHigh.get(0).getPrice() >= byCodeHigh.get(byCodeHigh.size() - 1).getPrice(), "화면 정렬 코드 PRICE_HIGH");
        check(service.search(new SearchFilter(null, null, null, null, null, null, null, "NEWEST")).get(0).getId()
                > service.search(new SearchFilter(null, null, null, null, null, null, null, "NEWEST")).get(1).getId(), "화면 정렬 코드 NEWEST");
        int m66 = service.search(new SearchFilter(null, null, null, "M (66)", null, null, null, null)).size();
        check(m66 > 0 && m66 == service.search(new SearchFilter(null, null, null, "66", null, null, null, null)).size(),
                "화면 사이즈 값 M (66)");
        check(service.search(new SearchFilter(null, null, "보통 체형", null, null, null, null, null)).size()
                == service.search(new SearchFilter(null, null, "보통형", null, null, null, null, null)).size(), "체형 예전 값도 받음");
        check(service.search(new SearchFilter(null, "선택 안 함", "선택 안 함", "선택 안 함", null, null, null, null)).size()
                == DEMO_PRODUCTS, "선택 안 함은 조건 없음");
        check(!service.search(new SearchFilter(null, null, null, null, null, null, "화이트·아이보리", null)).isEmpty(), "묶은 색상 화이트·아이보리");
        check(service.search(new SearchFilter(null, null, null, null, 100001, 300000, null, null)).stream()
                .allMatch(p -> p.getPrice() >= 100001 && p.getPrice() <= 300000), "화면 가격대 10~30만원");
        check(service.search(new SearchFilter("없는상품이름", null, null, null, null, null, null, null)).isEmpty(), "UC-05 2a 결과 없음");
        check(service.suggestKeywords("로파 신발").contains("신발"), "UC-05 2a 비슷한 검색어");

        Product first = service.findById(1L);
        check(first.getName() != null && first.mainImageUrl().startsWith("http"), "UC-06 상세");
        expectError(() -> service.findById(99999L), "UC-06 없는 상품");

        expectError(() -> service.save(new Product()), "UC-16 5a 필수 항목 누락");
        int dresses = service.findByCategory(Category.DRESS, 0).size();
        Product created = new Product();
        created.setName("테스트 원피스");
        created.setCategory(Category.DRESS);
        created.setPrice(50000);
        created.setOptions(List.of(new ProductOption(null, "66", "블랙", 2)));
        created.setImageUrls(List.of("https://example.com/a.jpg"));
        service.save(created);
        check(created.getId() == DEMO_PRODUCTS + 1L && created.getOptions().get(0).id() != null, "UC-16 등록 시 id 부여");
        check(service.findByCategory(Category.DRESS, 0).size() == dresses + 1, "UC-16 등록한 상품이 목록에 반영");

        Long optionId = created.getOptions().get(0).id();
        check(!service.decreaseStock(created.getId(), optionId, 3), "UC-11 7a 재고보다 많이는 차감 불가");
        check(service.decreaseStock(created.getId(), optionId, 2) && created.isSoldOut(), "UC-11 재고 차감 → 품절");
        check(service.findPopular(1).get(0) == created, "인기순: 많이 팔린 상품이 먼저");

        Product other = service.findById(2L);
        service.suspend(2L);
        check(other.getStatus() == Product.Status.SUSPENDED && !service.search(null).contains(other)
                && service.findAll().contains(other), "UC-16 3a suspend: 판매 중지 (관리자 목록에는 남음)");
        other.setStatus(Product.Status.ON_SALE);
        expectError(() -> service.suspend(99999L), "없는 상품은 판매 중지 불가");

        service.setActiveOrderChecker(id -> id == created.getId());
        service.delete(created.getId());
        check(created.getStatus() == Product.Status.SUSPENDED, "UC-16 3a 주문이 있으면 판매 중지");
        check(service.findByCategory(Category.DRESS, 0).size() == dresses
                && !service.findByCategory(Category.DRESS, 0).contains(created), "판매 중지 상품은 목록에서 빠짐");
        service.setActiveOrderChecker(null);
        service.delete(created.getId());
        check(service.findAll().size() == DEMO_PRODUCTS, "UC-16 삭제");
    }

    private static void recommend() {
        ProductService productService = new ProductService();
        RecommendService service = new RecommendService(productService);

        check(service.findLookbooks(null, null).size() == 6, "UC-07 데모 코디 6개");
        check(service.findLookbooks("여행", null).size() == 2, "UC-07 상황 필터");
        check(service.findLookbooks("여행", "겨울").size() == 1, "UC-07 상황·계절 필터");
        Lookbook lookbook = service.findLookbooks(null, null).get(0);
        check(service.findLookbookProducts(lookbook.getId()).size() >= 2, "UC-07 구성 상품");
        expectError(() -> service.saveLookbook(new Lookbook("제목만", null, "일상", "봄", List.of())), "UC-17 구성 상품 없음");

        List<Product> trending = service.findTrendingItems();
        check(trending.size() == 28, "UC-08 유행 아이템 28개 (세부 카테고리 14개 × 2)");
        Product target = productService.findById(3L);
        service.setTrending(3L, true);
        check(service.findTrendingItems().contains(target), "UC-17 유행 지정");
        service.setTrending(3L, false);
        check(!service.findTrendingItems().contains(target), "UC-17 유행 해제");

        Member member = new Member();
        member.setId(7L);
        check(service.recommendFor(member).size() == RecommendService.RECOMMEND_LIMIT, "UC-08 3a 체형 정보 없음 → 인기 상품");
        check(!service.hasBodyType(member), "UC-08 3a 체형 입력 권유 대상");
        member.setBodyProfile(new BodyProfile("50대", 158, "66", "하체 발달형"));
        List<Product> forMe = service.recommendFor(member);
        check(forMe.size() == RecommendService.RECOMMEND_LIMIT, "UC-08 맞춤 추천 12개");
        check(forMe.stream().filter(p -> p.getCategory() == Category.TOP || p.getCategory() == Category.BOTTOM)
                .allMatch(p -> p.getBodyTypeTags().contains("하체 발달형")), "UC-08 옷은 체형 태그가 맞는 것만");
        check(forMe.get(0).getCategory() == Category.TOP || forMe.get(0).getCategory() == Category.BOTTOM,
                "UC-08 체형이 맞는 옷이 먼저");
        check(forMe.stream().map(Product::getCategory).distinct().count() >= 4, "UC-08 카테고리가 고르게");
        Member younger = new Member();
        younger.setBodyProfile(new BodyProfile("40대", 158, "66", "하체 발달형"));
        Member older = new Member();
        older.setBodyProfile(new BodyProfile("60대", 158, "66", "하체 발달형"));
        check(!service.recommendFor(younger).equals(service.recommendFor(older)), "UC-08 연령대가 다르면 추천도 다름");
        check(service.recommendFor(older).stream().limit(3).allMatch(p -> p.getAgeGroupTags().contains("60대")),
                "UC-08 연령대가 맞는 상품이 먼저");

        Product suspended = productService.findById(lookbook.getProductIds().get(0));
        suspended.setStatus(Product.Status.SUSPENDED);
        check(service.lookbookWarnings(lookbook).size() == 1, "UC-17 2a 판매 중지 상품 경고");
        expectError(() -> service.saveLookbook(lookbook), "UC-17 2a 판매 중지 상품이 든 코디는 저장 안 됨");
        check(!service.recommendFor(member).contains(suspended), "판매 중지 상품은 추천에서 빠짐");
    }

    /** 화면이 쓰는 Controller 연결 (상품·룩북·관리자 코디 삭제). */
    private static void controllers() {
        ProductService productService = new ProductService();
        RecommendService recommendService = new RecommendService(productService);
        Session session = new Session();
        ProductController product = new ProductController(productService, null);
        RecommendController recommend = new RecommendController(session, recommendService);
        AdminController admin = new AdminController(session, productService, recommendService, null, null);

        check(product.countByCategory(Category.HAT) == 35, "카테고리 버튼 상품 수");
        check(product.suggestKeywords("로파 신발").contains("신발"), "검색 결과 없음 → 비슷한 검색어");
        check(recommend.occasions().containsAll(List.of("모임", "여행", "일상")) && recommend.seasons().contains("겨울"),
                "UC-07 상황·계절 필터 값");
        Lookbook first = recommend.lookbooks(null, null).get(0);
        check(recommend.lookbookProducts(first.getId()).size() == first.getProductIds().size(), "UC-07 코디 구성 상품");
        check(recommend.recommendedForMe().isEmpty() && !recommend.hasBodyType(), "UC-08 비회원은 맞춤 추천 없음");

        expectError(() -> admin.deleteLookbook(first.getId()), "UC-17 비회원은 코디 삭제 불가");
        Member user = new Member();
        user.setId(1L);
        session.login(user);
        expectError(() -> admin.deleteLookbook(first.getId()), "UC-17 일반 회원은 코디 삭제 불가");
        check(recommend.recommendedForMe().size() == RecommendService.RECOMMEND_LIMIT && !recommend.hasBodyType(),
                "UC-08 3a 체형 정보 없는 회원");

        Member manager = new Member();
        manager.setId(2L);
        manager.setRole(Member.Role.ADMIN);
        session.login(manager);
        Lookbook posted = admin.saveLookbook(new Lookbook("새 코디", null, "출근", "봄",
                List.of(first.getProductIds().get(0), first.getProductIds().get(1))));
        check(recommend.lookbooks("출근", "봄").size() == 1 && recommend.occasions().contains("출근"), "UC-17 코디 올리기");
        admin.deleteLookbook(posted.getId());
        check(recommend.lookbooks("출근", null).isEmpty(), "UC-17 코디 삭제");
        expectError(() -> admin.deleteLookbook(posted.getId()), "UC-17 없는 코디 삭제");
    }

    /** UC-19 내 코디북: 저장·목록·덮어쓰기·이름 바꾸기·지우기, 다시 켜도 유지, 판매 끝난 상품 빼고 되살리기. */
    private static void codiBook() throws Exception {
        Path dir = Files.createTempDirectory("sadamda-codibook");
        CodiBookService service = new CodiBookService(dir);
        String mom = CodiBookService.ownerKey("Demo@Sadamda.com ");
        String other = CodiBookService.ownerKey("daughter@sadamda.com");
        check(mom.equals(CodiBookService.ownerKey("demo@sadamda.com")) && mom.length() == 16
                && !mom.contains("@"), "UC-19 회원 열쇠: 이메일 대소문자·공백 무시, 폴더 이름에 이메일을 드러내지 않음");

        byte[] png = RemoteImages.toPng(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB));
        CharacterSpec spec = new CharacterSpec(158, 55, "통통한 체형", "50대");
        Map<DressUpSlot, Long> outfit = new java.util.EnumMap<>(DressUpSlot.class);
        outfit.put(DressUpSlot.TOP, 11L);
        outfit.put(DressUpSlot.BOTTOM, 12L);

        SavedCodi first = service.save(mom, "  딸 결혼식 룩 ", CharacterStyle.DISNEY, spec, outfit, png);
        SavedCodi second = service.save(mom, "제주 여행", CharacterStyle.REAL, spec, Map.of(DressUpSlot.HAT, 1L), null);
        check(first.title().equals("딸 결혼식 룩") && first.hasImage() && Files.exists(Path.of(first.imagePath())),
                "UC-19 저장: 이름 앞뒤 공백 정리, 그림 파일 저장");
        check(!second.hasImage(), "UC-19 입혀보기 전에도 옷만으로 저장");
        check(service.findAll(mom).size() == 2 && service.findAll(other).isEmpty(), "UC-19 회원별 코디북");
        check(service.findAll(mom).get(0).id() == second.id(), "UC-19 최근 저장한 코디가 먼저");
        expectError(() -> service.save(mom, " ", CharacterStyle.REAL, spec, outfit, null), "UC-19 이름 없이 저장 불가");
        expectError(() -> service.save(mom, "가".repeat(CodiBookService.MAX_TITLE + 1), CharacterStyle.REAL, spec, outfit, null),
                "UC-19 이름 30자 초과 불가");
        expectError(() -> service.save(mom, "빈 코디", CharacterStyle.REAL, spec, Map.of(), null), "UC-19 옷 없이 저장 불가");
        expectError(() -> service.save(null, "비회원", CharacterStyle.REAL, spec, outfit, null), "UC-19 비회원 저장 불가");
        expectError(() -> service.find(other, first.id()), "UC-19 남의 코디는 열 수 없음");

        CodiBookService reopened = new CodiBookService(dir);
        SavedCodi restored = reopened.find(mom, first.id());
        check(restored.title().equals("딸 결혼식 룩") && restored.style() == CharacterStyle.DISNEY
                        && restored.spec().equals(spec) && restored.productIds().equals(outfit) && restored.hasImage(),
                "UC-19 프로그램을 다시 켜도 이름·그림체·스펙·옷·그림 유지");
        check(java.util.Arrays.equals(reopened.readImage(restored), png), "UC-19 저장한 그림 그대로 읽기");

        SavedCodi changed = reopened.overwrite(mom, first.id(), CharacterStyle.ANIME, spec,
                Map.of(DressUpSlot.TOP, 13L), null);
        check(changed.title().equals("딸 결혼식 룩") && changed.style() == CharacterStyle.ANIME
                        && changed.productIds().equals(Map.of(DressUpSlot.TOP, 13L)) && !changed.hasImage()
                        && !Files.exists(dir.resolve(mom).resolve(first.id() + ".png")),
                "UC-19 4a 덮어쓰기: 이름 유지, 옷 바뀜, 그림 없이 덮으면 예전 그림 삭제");
        check(reopened.rename(mom, second.id(), "제주 둘째 날").title().equals("제주 둘째 날")
                && new CodiBookService(dir).find(mom, second.id()).title().equals("제주 둘째 날"), "UC-19 5a 이름 바꾸기 저장");
        SavedCodi third = reopened.save(mom, "새 코디", CharacterStyle.REAL, spec, outfit, png);
        check(third.id() == 3L, "UC-19 다시 켠 뒤에도 id 가 겹치지 않음");
        reopened.delete(mom, third.id());
        check(reopened.findAll(mom).size() == 2 && !Files.exists(Path.of(third.imagePath()))
                && new CodiBookService(dir).findAll(mom).size() == 2, "UC-19 5b 지우기: 파일도 삭제");
        Files.writeString(dir.resolve(mom).resolve("99.properties"), "망가진 파일");
        check(new CodiBookService(dir).findAll(mom).size() == 2, "UC-19 읽을 수 없는 파일은 건너뜀");

        // Controller: 로그인 확인, 판매 끝난 상품은 빼고 되살리기 (6a)
        ProductService products = new ProductService();
        Session session = new Session();
        CodiBookController controller = new CodiBookController(session, new CodiBookService(dir), products);
        expectError(controller::list, "UC-19 비회원은 코디북을 볼 수 없음 (LoginRequiredException)");
        Member member = new Member();
        member.setId(1L);
        member.setEmail("demo@sadamda.com");
        session.login(member);
        check(controller.list().size() == 2, "UC-19 로그인한 회원의 코디북 (이메일로 찾음)");
        Product top = products.findAllByCategory(Category.TOP).get(0);
        Product bottom = products.findAllByCategory(Category.BOTTOM).get(0);
        Outfit wearing = new Outfit();
        wearing.put(DressUpSlot.TOP, top);
        wearing.put(DressUpSlot.BOTTOM, bottom);
        SavedCodi viaController = controller.save("모임 룩", CharacterStyle.REAL, spec, wearing, png);
        CodiBookController.Opened whole = controller.open(viaController.id());
        check(whole.missing() == 0 && whole.outfit().get(DressUpSlot.TOP) == top && whole.image() != null,
                "UC-19 6단계 다시 입혀보기: 옷과 그림 되살림");
        products.suspend(bottom.getId());
        CodiBookController.Opened partial = controller.open(viaController.id());
        check(partial.missing() == 1 && partial.outfit().get(DressUpSlot.BOTTOM) == null && partial.image() == null,
                "UC-19 6a 판매 끝난 상품은 빼고, 지금 옷차림과 다른 예전 그림은 보여주지 않음");
        check(controller.products(viaController).size() == 2, "UC-19 목록의 구성 상품");
    }

    /** UC-20 상황별 코디 도우미: 상황·계절·체형에 맞는 3벌, 같은 조건이면 같은 결과. */
    private static void outfitHelper() {
        ProductService products = new ProductService();
        OutfitHelper helper = new OutfitHelper(products);
        check(OutfitHelper.seasonOf(java.time.LocalDate.of(2026, 11, 20)).equals("가을")
                && OutfitHelper.seasonOf(java.time.LocalDate.of(2026, 1, 5)).equals("겨울")
                && OutfitHelper.seasonOf(java.time.LocalDate.of(2026, 7, 1)).equals("여름"), "UC-20 오늘 날짜로 계절 고르기");

        for (String occasion : OutfitHelper.OCCASIONS) {
            List<OutfitSuggestion> suggestions = helper.suggest(occasion, "가을", "보통 체형", "50대");
            check(suggestions.size() == OutfitHelper.SUGGESTIONS, "UC-20 " + occasion + ": 코디 3벌");
            check(suggestions.stream().allMatch(s -> s.products().stream().anyMatch(p -> p.getCategory() == Category.TOP)
                            && s.products().stream().anyMatch(p -> p.getCategory() == Category.BOTTOM)
                            && s.products().stream().anyMatch(p -> p.getCategory() == Category.SHOES)),
                    "UC-20 " + occasion + ": 모든 코디에 상의·하의·신발");
            java.util.Set<Long> tops = new java.util.HashSet<>();
            suggestions.forEach(s -> s.products().stream().filter(p -> p.getCategory() == Category.TOP)
                    .forEach(p -> tops.add(p.getId())));
            check(tops.size() == 3, "UC-20 " + occasion + ": 세 코디의 상의가 서로 다름");
        }
        List<OutfitSuggestion> wedding = helper.suggest("결혼식·행사", "가을", null, null);
        check(wedding.get(0).products().stream().anyMatch(p -> p.getCategory() == Category.JEWELRY)
                        && wedding.get(0).products().stream().noneMatch(p -> p.getCategory() == Category.HAT),
                "UC-20 결혼식·행사는 주얼리를 더하고 모자는 빼기");
        List<OutfitSuggestion> trip = helper.suggest("여행·나들이", "봄", null, null);
        check(trip.get(0).products().get(0).getCategory() == Category.HAT, "UC-20 여행은 모자를 더하고, 모자 → 신발 순으로 정렬");
        check(helper.suggest("결혼식·행사", "가을", "보통 체형", "50대").equals(helper.suggest("결혼식·행사", "가을", "보통 체형", "50대")),
                "UC-20 같은 조건이면 같은 결과");

        List<OutfitSuggestion> pear = helper.suggest("일상·산책", "가을", "하체 발달형", "60대");
        check(pear.stream().flatMap(s -> s.products().stream())
                        .filter(p -> p.getCategory() == Category.TOP || p.getCategory() == Category.BOTTOM)
                        .allMatch(p -> p.getBodyTypeTags().contains("하체 발달형")),
                "UC-20 체형 태그가 맞는 상의·하의만");
        check(pear.get(0).reason().contains("하체 발달형에 맞춤"), "UC-20 고른 이유에 체형 표시");

        List<OutfitSuggestion> summer = helper.suggest("일상·산책", "여름", null, null);
        check(summer.stream().flatMap(s -> s.products().stream())
                        .noneMatch(p -> (p.getName() + " " + p.getMaterial()).contains("캐시미어")),
                "UC-20 여름에는 캐시미어를 고르지 않음");
        expectError(() -> helper.suggest("등산", "가을", null, null), "UC-20 목록에 없는 상황");
        expectError(() -> helper.suggest("여행·나들이", "장마", null, null), "UC-20 목록에 없는 계절");

        RecommendController controller = new RecommendController(new Session(), new RecommendService(products), helper);
        check(controller.suggestOutfits("모임·식사", controller.currentSeason(), null, null).size() == 3
                && controller.helperOccasions().size() == 4, "UC-20 비회원도 코디 도우미 사용 (Controller)");
        products.findAllByCategory(Category.BOTTOM).forEach(p -> products.suspend(p.getId()));
        check(helper.suggest("모임·식사", "가을", null, null).isEmpty(), "UC-20 4a 하의를 고를 수 없으면 빈 목록");
    }

    /** UC-21 옷장 출석: 하루 한 번, 주 단위 7칸, 5칸이면 무료배송 쿠폰, 그날의 코디, 결제 때 쿠폰 사용. */
    private static void attendance() throws Exception {
        Path dir = Files.createTempDirectory("sadamda-attendance");
        AttendanceService service = new AttendanceService(dir);
        String mom = com.sadamda.common.OwnerKeys.of("demo@sadamda.com");
        java.time.LocalDate monday = java.time.LocalDate.of(2026, 11, 16);
        check(AttendanceService.mondayOf(java.time.LocalDate.of(2026, 11, 22)).equals(monday)
                && AttendanceService.mondayOf(monday).equals(monday), "UC-21 한 주는 월요일부터 일요일까지");

        AttendanceWeek first = service.checkIn(mom, monday);
        check(first.checkedToday() && first.stampedCount() == 1 && first.coupons() == 0, "UC-21 첫 출석 도장");
        AttendanceWeek again = service.checkIn(mom, monday);
        check(again.stampedCount() == 1 && !again.couponJustEarned(), "UC-21 2a 같은 날 두 번 출석해도 한 칸");
        service.checkIn(mom, monday.plusDays(1));
        service.checkIn(mom, monday.plusDays(3)); // 수요일은 쉼
        service.checkIn(mom, monday.plusDays(4));
        AttendanceWeek fifth = service.checkIn(mom, monday.plusDays(5));
        check(fifth.stampedCount() == 5 && fifth.couponJustEarned() && fifth.coupons() == 1
                        && !fifth.isStamped(monday.plusDays(2)),
                "UC-21 4단계 하루 쉬어도 이어서, 한 주 5칸이면 무료배송 쿠폰 1장");
        AttendanceWeek sixth = service.checkIn(mom, monday.plusDays(6));
        check(!sixth.couponJustEarned() && sixth.coupons() == 1 && sixth.stampedCount() == 6,
                "UC-21 쿠폰은 한 주에 한 번만");
        AttendanceWeek nextWeek = service.checkIn(mom, monday.plusDays(7));
        check(nextWeek.stampedCount() == 1 && nextWeek.monday().equals(monday.plusDays(7))
                && nextWeek.totalDays() == 7, "UC-21 다음 주는 새 7칸, 지금까지 출석 수는 이어짐");
        check(new AttendanceService(dir).week(mom, monday.plusDays(6)).stampedCount() == 6
                && new AttendanceService(dir).coupons(mom) == 1, "UC-21 프로그램을 다시 켜도 출석·쿠폰 유지");
        check(service.week(com.sadamda.common.OwnerKeys.of("other@sadamda.com"), monday).stampedCount() == 0,
                "UC-21 회원별 출석");
        expectError(() -> service.checkIn(null, monday), "UC-21 비회원은 출석 불가");

        // 그날의 코디: 날마다 다르고, 같은 날이면 같다
        ProductService products = new ProductService();
        OutfitHelper helper = new OutfitHelper(products);
        OutfitSuggestion day1 = helper.dailyOutfit(monday, "보통 체형", "50대");
        java.util.Set<String> occasions = new java.util.HashSet<>();
        java.util.Set<java.util.List<Long>> outfits = new java.util.HashSet<>();
        java.util.Set<Long> weekTops = new java.util.HashSet<>();
        for (int i = 0; i < 7; i++) {
            OutfitSuggestion daily = helper.dailyOutfit(monday.plusDays(i), "보통 체형", "50대");
            occasions.add(daily.title().substring(daily.title().indexOf("·") + 2));
            outfits.add(daily.products().stream().map(Product::getId).toList());
            daily.products().stream().filter(p -> p.getCategory() == Category.TOP).forEach(p -> weekTops.add(p.getId()));
        }
        check(weekTops.size() == 7, "UC-21 일주일 동안 날마다 다른 상의");
        check(day1.title().startsWith("11월 16일 오늘의 코디")
                        && helper.dailyOutfit(monday, "보통 체형", "50대").equals(day1),
                "UC-21 3단계 그날의 코디: 날짜가 제목, 같은 날이면 같은 코디");
        check(occasions.size() == 4 && outfits.size() == 7, "UC-21 일주일 동안 상황 4가지를 돌며 코디 7벌이 모두 다름");

        // Controller: 출석한 날만 코디, 결제 때 쿠폰 쓰기
        Session session = new Session();
        AttendanceController controller = new AttendanceController(session, new AttendanceService(dir), helper,
                () -> monday.plusDays(6));
        expectError(controller::week, "UC-21 비회원은 출석 칸을 볼 수 없음 (LoginRequiredException)");
        Member member = new Member();
        member.setId(1L);
        member.setEmail("demo@sadamda.com");
        member.setBodyProfile(new BodyProfile("50대", 160, "M (66)", "보통 체형"));
        session.login(member);
        check(controller.week().stampedCount() == 6 && controller.outfitOf(monday).equals(day1),
                "UC-21 5단계 출석한 날 칸을 누르면 그날 코디 (회원 체형 기준)");
        expectError(() -> controller.outfitOf(monday.plusDays(2)), "UC-21 5a 쉰 날의 코디는 볼 수 없음");

        CartService cart = new CartService(products);
        OrderService orders = new OrderService(products, cart);
        AttendanceService wallet = new AttendanceService(dir);
        OrderController order = new OrderController(session, orders, cart, new MemberService(),
                new com.sadamda.external.MockPaymentGateway(), wallet);
        Product top = products.findAllByCategory(Category.TOP).get(0);
        List<com.sadamda.model.order.OrderLine> lines =
                List.of(new com.sadamda.model.order.OrderLine(top.getId(), top.getOptions().get(0).id(), 1));
        check(order.freeShippingCoupons() == 1, "UC-21 주문서에 가진 쿠폰 수");
        com.sadamda.model.order.Order free = order.checkout(new com.sadamda.controller.CheckoutRequest(
                lines, "서울시 종로구 1", "카드 결제", null, false, true));
        check(free.getShippingFee() == 0 && free.getTotalPrice() == top.getPrice() && order.freeShippingCoupons() == 0,
                "UC-21 무료배송 쿠폰으로 결제: 배송비 0원, 쿠폰 1장 사용");
        expectError(() -> order.checkout(new com.sadamda.controller.CheckoutRequest(
                lines, "서울시 종로구 1", "카드 결제", null, false, true)), "UC-21 쿠폰이 없으면 쿠폰 결제 불가");
        com.sadamda.model.order.Order paid = order.checkout(new com.sadamda.controller.CheckoutRequest(
                lines, "서울시 종로구 1", "카드 결제", null, false));
        check(paid.getShippingFee() == OrderService.SHIPPING_FEE, "UC-21 쿠폰을 안 쓰면 예전처럼 배송비 3,000원");
    }

    private static void locker() throws Exception {
        Path dir = Files.createTempDirectory("sadamda-check");
        TryOnService service = new TryOnService(dir);
        byte[] png = RemoteImages.toPng(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB));

        TryOnImage saved = service.save(7L, 12L, png);
        service.save(7L, null, png);
        service.save(8L, 12L, png);
        check(Files.exists(Path.of(saved.imagePath())), "FR-16 보관함 파일 저장");
        check(service.findByMember(7L).size() == 2 && service.findByMember(8L).size() == 1, "FR-16 회원별 목록");
        expectError(() -> service.save(null, 1L, png), "비회원은 보관함 저장 불가");
        expectError(() -> service.delete(8L, saved.id()), "남의 이미지는 삭제 불가");

        TryOnService reopened = new TryOnService(dir);
        check(reopened.findByMember(7L).size() == 2, "프로그램을 다시 켜도 보관함 유지");
        check(reopened.save(7L, 1L, png).id() == 4L, "다시 켠 뒤에도 id 가 겹치지 않음");

        List<String> templates = service.findBackgroundTemplates();
        check(templates.size() == 4 && templates.contains("흰 스튜디오"), "UC-09 4단계 기본 배경 템플릿 목록");
        BufferedImage background = RemoteImages.decode(service.loadBackgroundTemplate(templates.get(0)));
        check(background != null && background.getWidth() == 1024 && background.getHeight() == 1536, "UC-09 배경 템플릿 이미지");
        expectError(() -> service.loadBackgroundTemplate("없는 배경"), "없는 배경 템플릿");

        service.delete(7L, saved.id());
        check(!Files.exists(Path.of(saved.imagePath())) && service.findByMember(7L).size() == 1, "NFR-08 보관함 삭제");

        String upload = service.storeUpload(7L, new byte[]{1, 2, 3});
        service.storeUpload(7L, new byte[]{4});
        service.deleteUpload(upload);
        check(!Files.exists(Path.of(upload)), "NFR-08 합성 뒤 업로드 사진 삭제");
        Path outside = Files.createTempFile("outside", ".img");
        service.deleteUpload(outside.toString());
        check(Files.exists(outside), "업로드 폴더 밖 파일은 지우지 않음");
        check(service.deleteUploads(7L) == 1, "NFR-08 남은 업로드 사진 모두 삭제");

        String leftover = service.storeUpload(9L, new byte[]{5});
        String guestLeftover = service.storeUpload(null, new byte[]{6});
        TryOnService restarted = new TryOnService(dir); // 합성 중에 프로그램이 꺼진 뒤 다시 켠 경우
        check(!Files.exists(Path.of(leftover)) && !Files.exists(Path.of(guestLeftover)),
                "NFR-08 다시 켜면 남아 있던 업로드 사진을 지움");
        check(restarted.findByMember(7L).size() == 2, "업로드 정리는 보관함 이미지를 건드리지 않음");
    }

    private static void dressUp() throws Exception {
        ProductService productService = new ProductService();
        Outfit outfit = new Outfit();
        outfit.put(DressUpSlot.TOP, productService.findAllByCategory(Category.TOP).get(0));
        outfit.put(DressUpSlot.SHOES, productService.findAllByCategory(Category.SHOES).get(0));
        expectError(() -> outfit.put(DressUpSlot.HAT, productService.findAllByCategory(Category.TOP).get(0)), "자리와 다른 카테고리");
        check(outfit.totalPrice() > 0 && outfit.products().size() == 2, "한 벌 합계");

        // 가짜 AI 서버: 요청을 기록하고 1x1 PNG 를 Interactions API 응답 모양으로 돌려준다
        byte[] pixel = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        String big = Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(pixel, 300));
        AtomicReference<String> seenBody = new AtomicReference<>();
        AtomicReference<String> seenKey = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            seenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            seenKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            byte[] body = ("{\"id\":\"x\",\"steps\":[{\"type\":\"model_output\",\"content\":[{\"type\":\"image\","
                    + "\"mime_type\":\"image/png\",\"data\":\"" + big + "\"}]}]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/denied", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"error\":{\"code\":403,\"message\":\"API key not valid.\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        try {
            byte[] garment = new byte[]{(byte) 0xFF, (byte) 0xD8, 1, 2, 3};
            DressUpImageClient.Request request = new DressUpImageClient.Request(CharacterStyle.DISNEY,
                    new CharacterSpec(165, 80, "통통한 체형", "50대"), outfit, Map.of(DressUpSlot.TOP, garment), pixel);

            byte[] result = new GeminiDressUpClient("test-key", null, base + "/ok", 5).dressUp(request);
            check(result.length == 300, "Gemini 응답에서 이미지 꺼내기");
            check("test-key".equals(seenKey.get()), "이용 열쇠는 헤더로 전달");
            String body = seenBody.get();
            check(body.contains("\"model\":\"gemini-3.1-flash-image\""), "기본 모델 이름");
            check(body.contains("Edit Image 1") && body.contains("Image 2 is the product photo for the top")
                    && body.contains("top: the exact top shown in Image 2"), "지시문의 이미지 번호가 보내는 순서와 일치 (1=캐릭터, 2~=상품)");
            check(body.contains("shoes: [") && body.split("no photo; draw it from this description", -1).length == 2
                    && body.contains("ONLY reference for that product"),
                    "사진을 보낸 상품은 사진만 따르고, 사진이 없는 상품(신발)만 글로 설명");
            check(body.indexOf("image/png") < body.indexOf("image/jpeg"), "기본 캐릭터(PNG) 다음에 상품 사진(JPEG)");
            check(body.contains("165 cm tall and weighs 80 kg") && body.contains("BMI 29.4")
                    && body.contains("clearly overweight"), "키·몸무게·체질량지수 반영");
            check(body.contains("top of her head is about 15%"), "화면 속 머리 높이를 숫자로 전달");
            check(body.contains("hat: none") && body.contains("bottom: keep the plain white shorts"),
                    "고르지 않은 자리는 기본 캐릭터 차림 그대로");
            check(body.contains("use ONLY that one product") && body.contains("Do NOT paste, overlay or collage"),
                    "상품만 골라내고 사진 붙이기 금지");
            check(body.contains("same art style as the character") && body.contains("recognizes the same product"),
                    "그림체 통일 + 제품 디자인 유지");

            expectAiError(() -> new GeminiDressUpClient("bad", null, base + "/denied", 5).dressUp(request),
                    "이용 열쇠", "UC-09 7b 오류 응답 → 쉬운 한국어 안내");
            expectAiError(() -> new GeminiDressUpClient("k", null, base + "/slow", 1).dressUp(request),
                    "초 안에", "UC-09 7b 시간 초과");
        } finally {
            server.stop(0);
        }
    }

    /** OpenAI 호환 중계 서비스 형식: 가짜 서버로 요청·응답만 확인한다. */
    private static void openAiRelay() throws Exception {
        ProductService productService = new ProductService();
        Outfit outfit = new Outfit();
        outfit.put(DressUpSlot.TOP, productService.findAllByCategory(Category.TOP).get(0));
        outfit.put(DressUpSlot.BOTTOM, productService.findAllByCategory(Category.BOTTOM).get(0));

        byte[] pixel = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        String b64 = Base64.getEncoder().encodeToString(pixel);
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> type = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String[] base = new String[1];
        server.createContext("/v1/images/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            type.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            byte[] reply = ("{\"created\":1,\"data\":[{\"b64_json\":\"" + b64 + "\"}]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        // url 로 돌려주는 중계 서비스
        server.createContext("/url/images/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] reply = ("{\"data\":[{\"url\":\"" + base[0] + "/file.png\"}]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.createContext("/file.png", exchange -> {
            exchange.sendResponseHeaders(200, pixel.length);
            exchange.getResponseBody().write(pixel);
            exchange.close();
        });
        // chat/completions 방식 중계 서비스
        server.createContext("/relay/chat/completions", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] reply = ("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                    + "\"images\":[{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:image/png;base64,"
                    + b64.replace("/", "\\/") + "\"}}]}}]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        // 타임리 SDK API 흉내: 연동 키 → 토큰, edit_image 도구 → file-server 이름, 이름 → 내려받을 주소
        int[] tokenCount = {0};
        server.createContext("/timely-api/sdk-auth/authenticate", exchange -> {
            boolean ok = "tgpt_s-abc123".equals(exchange.getRequestHeaders().getFirst("X-Timely-API"));
            byte[] reply = (ok
                    ? "{\"success\":true,\"status\":200,\"data\":{\"access_token\":\"jwt-" + (++tokenCount[0]) + "\"}}"
                    : "{\"success\":false,\"status\":401,\"message\":\"Invalid API key\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(ok ? 200 : 401, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.createContext("/timely-api/built-in-tool-node/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            // 토큰은 가장 최근에 받은 것만 통한다 (만료 흉내)
            boolean fresh = ("Bearer jwt-" + tokenCount[0]).equals(auth.get());
            byte[] reply = (fresh
                    ? "{\"success\":true,\"name\":\"OpenaiTool\",\"output\":\"file-server:p_OPENAI_IMAGE_TEST.png\","
                    + "\"metadata\":{\"status\":\"SUCCEEDED\",\"type\":\"IMAGE_URL\"}}"
                    : "{\"success\":false,\"status\":401,\"message\":\"Access token expired\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(fresh ? 201 : 401, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.createContext("/timely-file/files/by/name/", exchange -> {
            boolean ok = exchange.getRequestURI().getPath().equals("/timely-file/files/by/name/p_OPENAI_IMAGE_TEST.png/get-url")
                    && exchange.getRequestHeaders().getFirst("Authorization").startsWith("Bearer jwt-");
            byte[] reply = ("{\"downloadUrl\":\"" + base[0] + "/blob/p_OPENAI_IMAGE_TEST.png?sv=1\\u0026sig=x\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(ok ? 200 : 403, ok ? reply.length : -1);
            if (ok) {
                exchange.getResponseBody().write(reply);
            }
            exchange.close();
        });
        server.createContext("/blob/", exchange -> {
            boolean ok = "sv=1&sig=x".equals(exchange.getRequestURI().getQuery());
            exchange.sendResponseHeaders(ok ? 200 : 403, ok ? pixel.length : -1);
            if (ok) {
                exchange.getResponseBody().write(pixel);
            }
            exchange.close();
        });
        server.createContext("/bad/images/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] reply = "{\"error\":{\"message\":\"Incorrect API key provided\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
        base[0] = "http://127.0.0.1:" + server.getAddress().getPort();
        try {
            byte[] garment = new byte[]{(byte) 0xFF, (byte) 0xD8, 9, 9};
            DressUpImageClient.Request withPhotos = new DressUpImageClient.Request(CharacterStyle.GAME_3D,
                    CharacterSpec.defaults(), outfit, Map.of(DressUpSlot.TOP, garment, DressUpSlot.BOTTOM, garment), pixel);
            OpenAiDressUpClient client = new OpenAiDressUpClient("relay-key", base[0] + "/v1/", null, "medium", 5);

            byte[] edited = client.dressUp(withPhotos);
            check(java.util.Arrays.equals(edited, pixel), "OpenAI 형식: b64_json 응답 읽기");
            check("/v1/images/edits".equals(path.get()), "images 방식은 images/edits");
            check("Bearer relay-key".equals(auth.get()), "이용 열쇠는 Bearer 헤더로 전달");
            check(type.get().startsWith("multipart/form-data; boundary="), "multipart 형식");
            check(body.get().split("name=\"image\\[\\]\"", -1).length - 1 == 3, "기본 캐릭터 1장 + 상품 사진 2장 첨부");
            check(body.get().contains("gpt-image-2") && body.get().contains("1024x1536") && body.get().contains("medium"),
                    "모델·크기·품질 전달");

            byte[] fromUrl = new OpenAiDressUpClient("k", base[0] + "/url", "my-model", null, 5).dressUp(withPhotos);
            check(java.util.Arrays.equals(fromUrl, pixel), "url 로 주는 중계 서비스도 처리");

            OpenAiDressUpClient chat = new OpenAiDressUpClient("k", base[0] + "/relay", null, null, null, 5);
            check(chat.name().contains("google/gemini-3.1-flash-image"), "중계 주소면 chat 방식·기본 모델");
            byte[] chatImage = chat.dressUp(withPhotos);
            check(java.util.Arrays.equals(chatImage, pixel), "chat 방식: message.images 의 data URL 읽기");
            check("/relay/chat/completions".equals(path.get()) && body.get().contains("\"modalities\":[\"image\",\"text\"]")
                    && body.get().split("data:image/jpeg;base64,", -1).length - 1 == 2
                    && body.get().split("data:image/png;base64,", -1).length - 1 == 1,
                    "chat 방식: 기본 캐릭터 1장 + 상품 사진 2장을 image_url 로 첨부");

            expectAiError(() -> new UnconnectedDressUpClient().dressUp(withPhotos), "AI 연결 설정",
                    "열쇠가 없으면 가짜 합성 대신 연결 안내");

            expectAiError(() -> new OpenAiDressUpClient("bad", base[0] + "/bad", null, null, 5).dressUp(withPhotos),
                    "이용 열쇠", "중계 서비스 키 오류 → 쉬운 한국어 안내");

            timelyKey(base[0], withPhotos, pixel, path, auth, body, tokenCount);
        } finally {
            server.stop(0);
        }
    }

    /** 타임리 AI 연동 키: 붙여넣은 열쇠 다듬기, edit_image 도구로 그리기, 토큰 다시 받기, 열쇠 고르는 순서. */
    private static void timelyKey(String base, DressUpImageClient.Request withPhotos, byte[] pixel,
                                  AtomicReference<String> path, AtomicReference<String> auth,
                                  AtomicReference<String> body, int[] tokenCount) throws Exception {
        // 복사·붙여넣기로 섞여 들어오는 따옴표·공백·줄바꿈·보이지 않는 문자·"Bearer ", 예전에 저장한 타임리 OpenAI 호환 주소
        DressUpSettings pasted = new DressUpSettings(" \"Bearer tgpt_s-abc" + (char) 0x200B + "123\n\" ",
                " 'https://hello.timelyai.io/api/v2/chat/bridge/openai/chat/completions/' ", "  ");
        check("tgpt_s-abc123".equals(pasted.apiKey()), "붙여넣은 열쇠 다듬기");
        check(pasted.baseUrl() == null && pasted.model() == null, "종료된 타임리 OpenAI 호환 주소는 저장하지 않음");
        DressUpImageClient chosen = DressUpClients.create(pasted);
        check(chosen instanceof com.sadamda.external.TimelyDressUpClient && chosen.connected()
                && chosen.name().startsWith("타임리 AI"), "타임리 키는 타임리 이미지 도구로 연결");
        check(DressUpClients.create(new DressUpSettings("tgpt_s-x", "https://relay.example/v1", "m")).name()
                .startsWith("타임리 AI"), "타임리 키는 서비스 주소·모델 이름을 적어도 타임리 이미지 도구");
        check(DressUpClients.create(new DressUpSettings("sk-x", null, null)).name().startsWith("OpenAI"),
                "타임리 키가 아니면 그대로 OpenAI 공식");

        DressUpImageClient timely = new com.sadamda.external.TimelyDressUpClient(pasted.apiKey(),
                base + "/timely-api/", base + "/timely-file", 5);
        check(java.util.Arrays.equals(timely.dressUp(withPhotos), pixel), "타임리: 토큰 → edit_image → 내려받기");
        check("/timely-api/built-in-tool-node/edit_image/invoke".equals(path.get()) && "Bearer jwt-1".equals(auth.get()),
                "타임리: 연동 키로 받은 토큰을 Bearer 로 전달");
        check(body.get().contains("\"aspectRatio\":\"portrait\"")
                        && body.get().split("data:image/png;base64,", -1).length - 1 == 1
                        && body.get().split("data:image/jpeg;base64,", -1).length - 1 == 2,
                "타임리: 기본 캐릭터 1장 + 상품 사진 2장을 referenceImageUrls 로 첨부");
        timely.dressUp(withPhotos);
        check(tokenCount[0] == 1, "타임리: 받아 둔 토큰을 다시 씀");
        tokenCount[0]++; // 서버 쪽에서 토큰이 만료됨
        check(java.util.Arrays.equals(timely.dressUp(withPhotos), pixel) && tokenCount[0] == 3
                && "Bearer jwt-3".equals(auth.get()), "타임리: 토큰이 만료되면 한 번 새로 받아 다시 요청");

        expectAiError(() -> new com.sadamda.external.TimelyDressUpClient("tgpt_s-wrong", base + "/timely-api",
                base + "/timely-file", 5).dressUp(withPhotos), "연동 키", "타임리 키 오류 → 다시 복사 안내");

        // 다른 프로그램용 OPENAI_API_KEY 가 있는 PC: 화면에 넣은 열쇠가 먼저
        System.setProperty("OPENAI_API_KEY", "sk-other-program");
        try {
            check(DressUpClients.keyFromEnvironment(), "환경 변수 열쇠 알아보기");
            check(DressUpClients.create(pasted).name().startsWith("타임리 AI"), "환경 변수 열쇠가 있어도 화면에 넣은 열쇠를 씀");
            check(DressUpClients.create(new DressUpSettings(null, null, null)).name().startsWith("OpenAI"),
                    "화면에 열쇠가 없으면 환경 변수 열쇠");
        } finally {
            System.clearProperty("OPENAI_API_KEY");
        }
    }

    /** 기본 캐릭터와 신체 스펙(키·몸무게) 반영. */
    private static void character() {
        CharacterSpec standard = CharacterSpec.defaults();
        check(standard.heightCm() == 160 && standard.weightKg() == 56, "기본 스펙 160cm 56kg");
        check(Math.abs(new CharacterSpec(160, 64, null, null).bmi() - 25.0) < 0.01, "체질량지수 계산");
        check(CharacterSpec.standardWeight(170) == 64, "키에 맞는 보통 몸무게");
        check(new CharacterSpec(300, 5, "?", "?").equals(new CharacterSpec(185, 35, "보통 체형", "40대")), "범위를 벗어난 값은 고쳐 담음");

        for (CharacterStyle style : CharacterStyle.values()) {
            BufferedImage base = CharacterFigure.base(style);
            check(base.getWidth() == 896 && base.getHeight() == 1200, style.label() + " 기본 캐릭터 이미지");
            for (int height : new int[]{185, 160, 140}) {
                CharacterSpec spec = new CharacterSpec(height, CharacterSpec.standardWeight(height), "보통 체형", "40대");
                double[] extent = CharacterFigure.figureExtent(CharacterFigure.shaped(style, spec));
                check(Math.abs(extent[0] - CharacterFigure.headTop(spec)) < 0.006 && Math.abs(extent[1] - 0.97) < 0.006,
                        style.label() + " " + height + "cm: 머리끝 " + Math.round(extent[0] * 1000) / 10.0 + "%, 발끝 97%");
            }
        }
        // 키 25cm 차이 = 화면 속 키 25/185 차이
        double tall = CharacterFigure.headTop(new CharacterSpec(185, 70, "보통 체형", "40대"));
        double small = CharacterFigure.headTop(new CharacterSpec(160, 56, "보통 체형", "40대"));
        check(Math.abs((small - tall) - 0.92 * 25 / 185) < 0.001, "화면 속 키가 실제 키에 비례");

        BufferedImage light = CharacterFigure.shaped(CharacterStyle.ANIME, new CharacterSpec(160, 50, "보통 체형", "40대"));
        BufferedImage heavy = CharacterFigure.shaped(CharacterStyle.ANIME, new CharacterSpec(160, 85, "보통 체형", "40대"));
        int lightWidth = widthAt(light, 0.58);
        int heavyWidth = widthAt(heavy, 0.58);
        check(heavyWidth > lightWidth * 1.25, "같은 키에서 85kg 이 50kg 보다 넓음 (" + lightWidth + " → " + heavyWidth + "px)");

        // AI 가 캐릭터를 더 크게 그려 온 경우: 185cm 모습을 150cm 스펙으로 바로잡는다
        CharacterSpec target = new CharacterSpec(150, 50, "보통 체형", "40대");
        BufferedImage tooTall = CharacterFigure.shaped(CharacterStyle.REAL, new CharacterSpec(185, 75, "보통 체형", "40대"));
        double[] fixed = CharacterFigure.figureExtent(CharacterFigure.fitHeight(tooTall, target, false));
        check(Math.abs(fixed[0] - CharacterFigure.headTop(target)) < 0.01 && Math.abs(fixed[1] - 0.97) < 0.01,
                "AI 결과의 화면 속 키 바로잡기");
        BufferedImage right = CharacterFigure.shaped(CharacterStyle.REAL, target);
        check(CharacterFigure.fitHeight(right, target, false) == right, "이미 맞는 그림은 건드리지 않음");
    }

    /** 그 높이에서 배경이 아닌 부분의 너비(px). 가운데에서 바깥으로 훑는다. */
    private static int widthAt(BufferedImage image, double at) {
        int y = (int) (image.getHeight() * at);
        int background = image.getRGB(2, y);
        int left = image.getWidth();
        int right = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            int rgb = image.getRGB(x, y);
            int diff = Math.abs(((rgb >> 16) & 0xFF) - ((background >> 16) & 0xFF))
                    + Math.abs(((rgb >> 8) & 0xFF) - ((background >> 8) & 0xFF))
                    + Math.abs((rgb & 0xFF) - (background & 0xFF));
            if (diff > 60) {
                left = Math.min(left, x);
                right = Math.max(right, x);
            }
        }
        return Math.max(0, right - left);
    }

    private interface AiCall {
        void run() throws DressUpException;
    }

    private static void expectAiError(AiCall call, String expectedWord, String name) {
        try {
            call.run();
            check(false, name);
        } catch (DressUpException e) {
            check(e.getMessage().contains(expectedWord), name + " [" + e.getMessage() + "]");
        }
    }

    private static void expectError(Runnable call, String name) {
        try {
            call.run();
            check(false, name);
        } catch (AppException e) { // Controller 명세: Model 의 실패는 사용자에게 보여줄 AppException
            check(true, name);
        }
    }

    private static void check(boolean condition, String name) {
        checks++;
        if (!condition) {
            throw new AssertionError("실패: " + name);
        }
        System.out.println("  통과  " + name);
    }
}
