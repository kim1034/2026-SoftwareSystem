package com.sadamda.model.recommend;

import com.sadamda.model.member.Member;
import com.sadamda.model.product.Product;

import java.util.List;

/** 룩북·유행·맞춤 추천 로직. 담당: 정우진 */
public class RecommendService {

    /** UC-07 룩북 목록 (FR-09). occasion·season 이 null 이면 전체. */
    public List<Lookbook> findLookbooks(String occasion, String season) {
        throw new UnsupportedOperationException("TODO UC-07");
    }

    /** UC-07 코디 상세와 구성 상품 (FR-10). */
    public Lookbook findLookbook(Long lookbookId) {
        throw new UnsupportedOperationException("TODO UC-07");
    }

    /** UC-08 이번 시즌 유행 아이템 (FR-11). */
    public List<Product> findTrendingItems() {
        throw new UnsupportedOperationException("TODO UC-08");
    }

    /** UC-08 연령대·체형 맞춤 추천 (FR-12). 체형 정보가 없으면 연령대 인기 상품 (3a). */
    public List<Product> recommendFor(Member member) {
        throw new UnsupportedOperationException("TODO UC-08");
    }

    /** UC-17 코디 등록 (FR-26). 판매 중지 상품이 포함되면 경고 (2a). */
    public Lookbook saveLookbook(Lookbook lookbook) {
        throw new UnsupportedOperationException("TODO UC-17");
    }

    /** UC-17 유행 아이템 지정·해제 (FR-26). */
    public void setTrending(Long productId, boolean trending) {
        throw new UnsupportedOperationException("TODO UC-17");
    }
}
