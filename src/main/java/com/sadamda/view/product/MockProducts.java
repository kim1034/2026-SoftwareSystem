package com.sadamda.view.product;

import com.sadamda.model.product.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 임시 목업 데이터. Product 에 getter 가 생기면 이 파일을 지우고
 * ProductListPanel 의 fetch / fetchPopular 를 context.product.listByCategory / popular 로 바꾼다.
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

    private MockProducts() {
    }

    /** ProductController.listByCategory(category, page) 와 같은 방식. page 는 0부터. */
    static List<ProductItem> listByCategory(Category category, int page) {
        List<ProductItem> all = BY_CATEGORY.get(category);
        int from = page * PAGE_SIZE;
        if (from >= all.size()) {
            return List.of();
        }
        return all.subList(from, Math.min(from + PAGE_SIZE, all.size()));
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
