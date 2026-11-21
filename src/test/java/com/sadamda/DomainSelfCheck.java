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
import com.sadamda.model.tryon.Background;
import com.sadamda.model.tryon.BackgroundCatalog;
import com.sadamda.model.tryon.CharacterFigure;
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpPrompt;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.model.tryon.TryOnImage;
import com.sadamda.model.tryon.TryOnService;
import com.sadamda.model.tryon.UserPhoto;
import com.sadamda.controller.DressUpController;
import com.sadamda.common.AppException;
import com.sadamda.controller.AdminController;
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
        dressUp();
        openAiRelay();
        character();
        photoTryOn();
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

    /** 옷 칸 14개, 원피스 규칙, 배경 목록, 내 사진 확인, 내 사진 지시문 (2026-11-20 추가). */
    private static void photoTryOn() throws Exception {
        ProductService productService = new ProductService();
        check(DressUpSlot.values().length == 14, "옷 칸 14개");
        check(productService.findAll().stream().allMatch(p -> DressUpSlot.slotFor(p) != null),
                "모든 데모 상품을 입힐 칸이 있음 (원피스·아우터·양말·가방·스카프 포함)");
        Map<DressUpSlot, Long> jewelry = new java.util.EnumMap<>(DressUpSlot.class);
        for (Product product : productService.findAllByCategory(Category.JEWELRY)) {
            jewelry.merge(DressUpSlot.slotFor(product), 1L, Long::sum);
        }
        check(jewelry.getOrDefault(DressUpSlot.NECKLACE, 0L) == 35 && jewelry.getOrDefault(DressUpSlot.BRACELET, 0L) == 35
                        && jewelry.getOrDefault(DressUpSlot.EARRINGS, 0L) == 24 && jewelry.getOrDefault(DressUpSlot.BROOCH, 0L) == 9
                        && jewelry.getOrDefault(DressUpSlot.RING, 0L) == 2,
                "주얼리 105개를 귀걸이 24·목걸이 35·팔찌 35·반지 2·브로치 9 로 나눔 " + jewelry);
        Product earring = productService.findAllByCategory(Category.JEWELRY).stream()
                .filter(DressUpSlot.EARRINGS::accepts).findFirst().orElseThrow();
        Outfit outfit = new Outfit();
        expectError(() -> outfit.put(DressUpSlot.NECKLACE, earring), "귀걸이를 목걸이 칸에 넣으면 거절");

        Product top = productService.findAllByCategory(Category.TOP).get(0);
        Product bottom = productService.findAllByCategory(Category.BOTTOM).get(0);
        Product dress = productService.findAllByCategory(Category.DRESS).get(0);
        outfit.put(DressUpSlot.TOP, top);
        outfit.put(DressUpSlot.BOTTOM, bottom);
        List<DressUpSlot> removed = outfit.put(DressUpSlot.DRESS, dress);
        check(removed.equals(List.of(DressUpSlot.TOP, DressUpSlot.BOTTOM)) && outfit.size() == 1,
                "원피스를 입히면 상의·하의를 벗김");
        check(outfit.put(DressUpSlot.TOP, top).equals(List.of(DressUpSlot.DRESS)) && outfit.get(DressUpSlot.DRESS) == null,
                "상의를 입히면 원피스를 벗김");

        // 캐릭터 지시문: 원피스는 흰 티·반바지를 대신하고, 양말만 신으면 맨발이 아님
        Outfit dressed = new Outfit();
        dressed.put(DressUpSlot.DRESS, dress);
        dressed.put(DressUpSlot.SOCKS, productService.findAllByCategory(Category.SOCKS).get(0));
        String characterPrompt = DressUpPrompt.build(CharacterStyle.REAL, CharacterSpec.defaults(), dressed,
                List.of(DressUpSlot.DRESS));
        check(characterPrompt.contains("the dress replaces her white t-shirt and shorts")
                && characterPrompt.contains("she wears only the socks")
                && !characterPrompt.contains("keep the plain white t-shirt"), "캐릭터: 원피스·양말 지시");

        // 아우터: 아우터 사진 속 안쪽 옷 대신 고른 상의를 안에 입힌다 (2026-11-20 수정)
        Product outer = productService.findAllByCategory(Category.OUTER).get(0);
        Outfit layered = new Outfit();
        layered.put(DressUpSlot.TOP, top);
        layered.put(DressUpSlot.OUTER, outer);
        byte[] photoBytes = {9};
        String layeredCharacter = new DressUpImageClient.Request(CharacterStyle.REAL, CharacterSpec.defaults(), layered,
                Map.of(DressUpSlot.TOP, photoBytes, DressUpSlot.OUTER, photoBytes), photoBytes).prompt();
        check(layeredCharacter.contains("ONLY the coat or jacket itself")
                        && layeredCharacter.contains("She wears the top from Image 2 UNDER the outerwear")
                        && layeredCharacter.contains("In Image 3 the model wears other clothes")
                        && layeredCharacter.contains("knit cuffs")
                        && layeredCharacter.contains("wear it open at the front"),
                "캐릭터: 아우터 사진(Image 3)의 안쪽 옷 대신 고른 상의(Image 2)를 안에 입힘");
        String layeredPhoto = DressUpImageClient.Request.photo(layered,
                Map.of(DressUpSlot.TOP, photoBytes, DressUpSlot.OUTER, photoBytes), photoBytes, photoBytes).prompt();
        check(layeredPhoto.contains("She wears the top from Image 3 UNDER the outerwear")
                        && layeredPhoto.contains("In Image 4 the model wears other clothes"),
                "내 사진: 배경이 Image 2 라 상의 Image 3, 아우터 Image 4");
        Outfit outerOnly = new Outfit();
        outerOnly.put(DressUpSlot.OUTER, outer);
        String outerCharacter = DressUpPrompt.build(CharacterStyle.REAL, CharacterSpec.defaults(), outerOnly,
                List.of(DressUpSlot.OUTER));
        check(outerCharacter.contains("She wears the plain white t-shirt from Image 1 UNDER the outerwear")
                        && !outerCharacter.contains("wear it open at the front"),
                "상의를 안 고르면 캐릭터의 흰 티셔츠를 안에 입힘");
        String outerPhoto = DressUpImageClient.Request.photo(outerOnly, Map.of(DressUpSlot.OUTER, photoBytes),
                photoBytes, null).prompt();
        check(outerPhoto.contains("She wears her own top from Image 1 UNDER the outerwear"), "내 사진은 원래 상의를 안에 입힘");
        check(!characterPrompt.contains("LAYERING"), "아우터가 없으면 겹쳐 입기 지시 없음");

        BackgroundCatalog catalog = new BackgroundCatalog();
        check(catalog.all().size() == 20, "배경 20개");
        check(BackgroundCatalog.SEASONS.stream().allMatch(season -> catalog.bySeason(season).size() == 5),
                "계절마다 배경 5개");
        check(catalog.all().stream().allMatch(b -> b.imageUrl().startsWith("https://i.pinimg.com/")
                        && !b.shortName().isBlank() && b.source().contains("pinterest.com")
                        && b.name().startsWith(b.season() + " · " + b.country())),
                "배경마다 사진 주소·짧은 이름·출처(핀 주소)가 있음");
        Background first = catalog.find("BG-01");
        check(first != null && "봄 · 일본".equals(first.caption()), "배경 버튼 윗줄: 계절 · 나라");

        // 내 사진 확인 (UC-09 2a 동의, 3a 형식·용량)
        byte[] png = RemoteImages.toPng(new BufferedImage(400, 600, BufferedImage.TYPE_INT_RGB));
        expectError(() -> UserPhoto.of("me.png", png, false), "2a 동의하지 않으면 거절");
        expectError(() -> UserPhoto.of("me.png", new byte[0], true), "빈 사진 거절");
        expectError(() -> UserPhoto.of("me.gif", png, true), "3a GIF 거절");
        expectError(() -> UserPhoto.of("photo.jpg.exe", png, true), "3a 확장자 속이기 거절");
        expectError(() -> UserPhoto.of("big.jpg", new byte[(int) UserPhoto.MAX_BYTES + 1], true), "3a 10MB 초과 거절");
        expectError(() -> UserPhoto.of("fake.jpg", "not an image".getBytes(StandardCharsets.UTF_8), true),
                "사진이 아닌 파일 거절");
        byte[] tiny = RemoteImages.toPng(new BufferedImage(100, 120, BufferedImage.TYPE_INT_RGB));
        expectError(() -> UserPhoto.of("tiny.png", tiny, true), "너무 작은 사진 거절");
        UserPhoto ok = UserPhoto.of("ME.PNG", png, true);
        check(ok.image().getWidth() == 400 && ok.image().getHeight() == 600, "대문자 확장자 PNG 통과");

        BufferedImage forAi = RemoteImages.decode(UserPhoto.of("big.jpg", jpeg(2000, 3000), true).jpegForAi());
        check(forAi != null && forAi.getHeight() == 1536 && forAi.getWidth() == 1024,
                "AI 로는 긴 변 1536px JPEG 로 줄여 보냄");
        UserPhoto upright = UserPhoto.of("phone.jpg", withExifOrientation(jpeg(600, 400), 6), true);
        check(upright.image().getWidth() == 400 && upright.image().getHeight() == 600, "휴대폰 사진 EXIF 방향(6)대로 세움");
        check(!new String(upright.jpegForAi(), StandardCharsets.ISO_8859_1).contains("Exif"),
                "AI 로 보내는 사진에는 EXIF 가 없음");

        // 내 사진 요청: 이미지 순서와 지시문 번호
        Outfit wearing = new Outfit();
        wearing.put(DressUpSlot.TOP, top);
        wearing.put(DressUpSlot.EARRINGS, earring);
        byte[] person = {1};
        byte[] place = {2};
        byte[] garment = {3};
        DressUpImageClient.Request withPlace = DressUpImageClient.Request.photo(wearing,
                Map.of(DressUpSlot.TOP, garment), person, place);
        check(withPlace.images().size() == 3 && withPlace.images().get(0) == person && withPlace.images().get(1) == place
                && withPlace.images().get(2) == garment, "내 사진 요청: 1=내 사진, 2=배경, 3~=상품 사진");
        String prompt = withPlace.prompt();
        check(prompt.contains("a real photograph of a woman") && prompt.contains("location shown in Image 2")
                && prompt.contains("top: the exact top shown in Image 3")
                && prompt.contains("Image 3 is the product photo for the top")
                && prompt.contains("earrings: [") && prompt.contains("same face and identity")
                && prompt.contains("photorealistic"), "내 사진 지시문: 배경 Image 2, 상품 Image 3~, 얼굴·몸 유지");
        String keepBackground = DressUpImageClient.Request.photo(wearing, Map.of(DressUpSlot.TOP, garment), person, null)
                .prompt();
        check(keepBackground.contains("Keep the original background of Image 1")
                && keepBackground.contains("top: the exact top shown in Image 2"), "배경을 고르지 않으면 원래 배경, 상품은 Image 2~");
        String placeOnly = DressUpImageClient.Request.photo(new Outfit(), Map.of(), person, place).prompt();
        check(placeOnly.contains("Place her in the location shown in Image 2")
                && placeOnly.contains("keep everything she wears in Image 1"), "옷 없이 배경만 바꾸기");

        DressUpController controller = new DressUpController(new Session(), new UnconnectedDressUpClient(),
                productService, catalog);
        check(controller.backgrounds().size() == 20, "Controller 배경 목록");
        check(controller.productsFor(DressUpSlot.RING).size() == 2
                && controller.productsFor(DressUpSlot.RING).stream().allMatch(DressUpSlot.RING::accepts), "반지 칸에는 반지만");
        expectAiError(() -> controller.dressUpPhoto(null, null, wearing), "사진", "사진 없이 입혀보기 거절");
        expectAiError(() -> controller.dressUpPhoto(ok, null, new Outfit()), "배경", "옷도 배경도 없으면 거절");
        expectAiError(() -> controller.dressUpPhoto(ok, first, new Outfit()), "AI 연결 설정", "AI 미연결이면 연결 안내");
    }

    private static byte[] jpeg(int width, int height) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return out.toByteArray();
    }

    /** JPEG 맨 앞(SOI 뒤)에 EXIF 방향값만 든 APP1 을 끼운다 (휴대폰 사진 흉내). */
    private static byte[] withExifOrientation(byte[] jpeg, int orientation) {
        byte[] tiff = {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
                0, 0, 0, 0};
        byte[] exif = new byte[6 + tiff.length];
        System.arraycopy("Exif".getBytes(StandardCharsets.US_ASCII), 0, exif, 0, 4);
        System.arraycopy(tiff, 0, exif, 6, tiff.length);
        int length = exif.length + 2;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write(jpeg, 0, 2);
        out.write(0xFF);
        out.write(0xE1);
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.write(exif, 0, exif.length);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
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
