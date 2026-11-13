package com.sadamda;

import com.sadamda.external.AiImageClient;
import com.sadamda.external.DressUpImageClient;
import com.sadamda.external.GeminiDressUpClient;
import com.sadamda.external.LocalDressUpClient;
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
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.model.tryon.TryOnImage;
import com.sadamda.model.tryon.TryOnService;
import com.sun.net.httpserver.HttpServer;

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
    private static int checks;

    public static void main(String[] args) throws Exception {
        products();
        recommend();
        locker();
        dressUp();
        openAiRelay();
        System.out.println("모두 통과: " + checks + "개 확인");
    }

    private static void products() {
        ProductService service = new ProductService();
        check(service.findAll().size() == 300, "데모 상품 300개");
        check(service.countByCategory(Category.SHOES) == 90, "신발 90개");
        check(service.countByCategory(Category.JEWELRY) == 90, "주얼리 90개");
        check(service.findByCategory(Category.TOP, 0).size() == ProductService.PAGE_SIZE, "UC-04 첫 쪽 12개");
        check(service.findByCategory(Category.TOP, 2).size() == 6, "UC-04 셋째 쪽 6개 (30개 중)");
        check(service.findByCategory(Category.DRESS, 0).isEmpty(), "UC-04 2a 원피스는 준비 중");
        check(service.findPopular(8).size() == 8, "UC-04 2a 인기 상품");

        List<Product> loafers = service.search(new SearchFilter("로퍼", null, null, null, null, null, null, null));
        check(!loafers.isEmpty() && loafers.stream().allMatch(p -> p.getCategory() == Category.SHOES), "UC-05 키워드 검색");
        check(service.search(new SearchFilter("신발", null, null, null, null, null, null, null)).size() == 90, "UC-05 카테고리 이름 검색");

        List<Product> cheap = service.search(new SearchFilter(null, null, null, null, null, 100000, null, ProductService.SORT_PRICE_ASC));
        check(!cheap.isEmpty() && cheap.stream().allMatch(p -> p.getPrice() <= 100000), "UC-05 가격대 필터");
        check(cheap.get(0).getPrice() <= cheap.get(cheap.size() - 1).getPrice(), "UC-05 낮은가격순");
        List<Product> newest = service.search(new SearchFilter(null, null, null, null, null, null, null, ProductService.SORT_NEWEST));
        check(newest.get(0).getId() > newest.get(1).getId(), "UC-05 신상품순");
        List<Product> navy = service.search(new SearchFilter(null, null, null, null, null, null, "네이비", null));
        check(!navy.isEmpty(), "UC-05 색상 필터");
        List<Product> size240 = service.search(new SearchFilter(null, null, null, "240", null, null, null, null));
        check(size240.size() == 90, "UC-05 사이즈 필터 (240 은 신발만)");
        List<Product> pear = service.search(new SearchFilter(null, "50대", "하체발달형", null, null, null, null, null));
        check(!pear.isEmpty() && pear.size() < 300, "UC-05 연령대·체형 필터");
        check(service.search(new SearchFilter("없는상품이름", null, null, null, null, null, null, null)).isEmpty(), "UC-05 2a 결과 없음");
        check(service.suggestKeywords("로파 신발").contains("신발"), "UC-05 2a 비슷한 검색어");

        Product first = service.findById(1L);
        check(first.getName() != null && first.mainImageUrl().startsWith("http"), "UC-06 상세");
        expectError(() -> service.findById(99999L), "UC-06 없는 상품");

        expectError(() -> service.save(new Product()), "UC-16 5a 필수 항목 누락");
        Product created = new Product();
        created.setName("테스트 원피스");
        created.setCategory(Category.DRESS);
        created.setPrice(50000);
        created.setOptions(List.of(new ProductOption(null, "66", "블랙", 2)));
        created.setImageUrls(List.of("https://example.com/a.jpg"));
        service.save(created);
        check(created.getId() == 301L && created.getOptions().get(0).id() != null, "UC-16 등록 시 id 부여");
        check(service.findByCategory(Category.DRESS, 0).size() == 1, "UC-16 등록한 상품이 목록에 반영");

        Long optionId = created.getOptions().get(0).id();
        check(!service.decreaseStock(created.getId(), optionId, 3), "UC-11 7a 재고보다 많이는 차감 불가");
        check(service.decreaseStock(created.getId(), optionId, 2) && created.isSoldOut(), "UC-11 재고 차감 → 품절");
        check(service.findPopular(1).get(0) == created, "인기순: 많이 팔린 상품이 먼저");

        service.setActiveOrderChecker(id -> id == created.getId());
        service.delete(created.getId());
        check(created.getStatus() == Product.Status.SUSPENDED, "UC-16 3a 주문이 있으면 판매 중지");
        check(service.findByCategory(Category.DRESS, 0).isEmpty(), "판매 중지 상품은 목록에서 빠짐");
        service.setActiveOrderChecker(null);
        service.delete(created.getId());
        check(service.findAll().size() == 300, "UC-16 삭제");
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
        check(trending.size() == 20, "UC-08 유행 아이템 20개");
        Product target = productService.findById(3L);
        service.setTrending(3L, true);
        check(service.findTrendingItems().contains(target), "UC-17 유행 지정");
        service.setTrending(3L, false);
        check(!service.findTrendingItems().contains(target), "UC-17 유행 해제");

        Member member = new Member();
        member.setId(7L);
        check(service.recommendFor(member).size() == RecommendService.RECOMMEND_LIMIT, "UC-08 3a 체형 정보 없음 → 인기 상품");
        check(!service.hasBodyType(member), "UC-08 3a 체형 입력 권유 대상");
        member.setBodyProfile(new BodyProfile("50대", 158, "66", "하체발달형"));
        List<Product> forMe = service.recommendFor(member);
        check(forMe.size() == RecommendService.RECOMMEND_LIMIT, "UC-08 맞춤 추천 12개");
        check(forMe.stream().filter(p -> p.getCategory() == Category.TOP || p.getCategory() == Category.BOTTOM)
                .allMatch(p -> p.getBodyTypeTags().contains("하체발달형")), "UC-08 옷은 체형 태그가 맞는 것만");
        check(forMe.get(0).getCategory() == Category.TOP || forMe.get(0).getCategory() == Category.BOTTOM,
                "UC-08 체형이 맞는 옷이 먼저");
        check(forMe.stream().map(Product::getCategory).distinct().count() >= 4, "UC-08 카테고리가 고르게");

        Product suspended = productService.findById(lookbook.getProductIds().get(0));
        suspended.setStatus(Product.Status.SUSPENDED);
        check(service.lookbookWarnings(lookbook).size() == 1, "UC-17 2a 판매 중지 상품 경고");
        check(!service.recommendFor(member).contains(suspended), "판매 중지 상품은 추천에서 빠짐");
    }

    private static void locker() throws Exception {
        Path dir = Files.createTempDirectory("sadamda-check");
        TryOnService service = new TryOnService(dir);
        byte[] png = new LocalDressUpClient().dressUp(new DressUpImageClient.Request(
                CharacterStyle.ANIME, CharacterSpec.defaults(), new Outfit(), Map.of(), null));

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
                    new CharacterSpec(165, "통통형", "50대"), outfit, Map.of(DressUpSlot.TOP, garment), pixel);

            byte[] result = new GeminiDressUpClient("test-key", null, base + "/ok", 5).dressUp(request);
            check(result.length == 300, "Gemini 응답에서 이미지 꺼내기");
            check("test-key".equals(seenKey.get()), "이용 열쇠는 헤더로 전달");
            String body = seenBody.get();
            check(body.contains("\"model\":\"gemini-3.1-flash-image\""), "기본 모델 이름");
            check(body.contains("Image 1 is the character reference") && body.contains("Image 2 shows the top"),
                    "지시문의 이미지 번호가 보내는 순서와 일치");
            check(body.contains("\"mime_type\":\"image/png\"") && body.contains("\"mime_type\":\"image/jpeg\""),
                    "기준 이미지(PNG)와 상품 사진(JPEG) 첨부");
            check(body.contains("in her 50s") && body.contains("165 cm") && body.contains("plus-size"), "신체 스펙 반영");
            check(body.contains("hat: no hat") && body.contains("bottom: plain light-gray"), "고르지 않은 자리는 기본 차림");
            check(body.contains("not any existing film character"), "디즈니 그림체는 새 캐릭터로");

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
                    CharacterSpec.defaults(), outfit, Map.of(DressUpSlot.TOP, garment, DressUpSlot.BOTTOM, garment), null);
            OpenAiDressUpClient client = new OpenAiDressUpClient("relay-key", base[0] + "/v1/", null, "medium", 5);

            byte[] edited = client.dressUp(withPhotos);
            check(java.util.Arrays.equals(edited, pixel), "OpenAI 형식: b64_json 응답 읽기");
            check("/v1/images/edits".equals(path.get()), "상품 사진이 있으면 images/edits");
            check("Bearer relay-key".equals(auth.get()), "이용 열쇠는 Bearer 헤더로 전달");
            check(type.get().startsWith("multipart/form-data; boundary="), "multipart 형식");
            check(body.get().split("name=\"image\\[\\]\"", -1).length - 1 == 2, "상품 사진 2장 첨부");
            check(body.get().contains("gpt-image-2") && body.get().contains("1024x1536") && body.get().contains("medium"),
                    "모델·크기·품질 전달");

            client.dressUp(new DressUpImageClient.Request(CharacterStyle.GAME_3D, CharacterSpec.defaults(),
                    new Outfit(), Map.of(), null));
            check("/v1/images/generations".equals(path.get()) && body.get().contains("\"size\":\"1024x1536\""),
                    "사진이 없으면 images/generations (JSON)");

            byte[] fromUrl = new OpenAiDressUpClient("k", base[0] + "/url", "my-model", null, 5).dressUp(withPhotos);
            check(java.util.Arrays.equals(fromUrl, pixel), "url 로 주는 중계 서비스도 처리");

            expectAiError(() -> new OpenAiDressUpClient("bad", base[0] + "/bad", null, null, 5).dressUp(withPhotos),
                    "이용 열쇠", "중계 서비스 키 오류 → 쉬운 한국어 안내");
        } finally {
            server.stop(0);
        }
    }

    private interface AiCall {
        void run() throws AiImageClient.AiImageException;
    }

    private static void expectAiError(AiCall call, String expectedWord, String name) {
        try {
            call.run();
            check(false, name);
        } catch (AiImageClient.AiImageException e) {
            check(e.getMessage().contains(expectedWord), name + " [" + e.getMessage() + "]");
        }
    }

    private static void expectError(Runnable call, String name) {
        try {
            call.run();
            check(false, name);
        } catch (IllegalArgumentException | IllegalStateException e) {
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
