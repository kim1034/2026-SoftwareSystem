package com.sadamda.controller;

import com.sadamda.model.product.Product;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.model.recommend.OutfitHelper;
import com.sadamda.model.recommend.OutfitSuggestion;
import com.sadamda.model.recommend.RecommendService;

import java.time.LocalDate;
import java.util.List;

/** UC-07 룩북 코디 추천 보기, UC-08 유행·맞춤 아이템 추천 보기, UC-20 상황별 코디 도우미. */
public class RecommendController {
    private final Session session;
    private final RecommendService recommendService;
    private final OutfitHelper outfitHelper;

    public RecommendController(Session session, RecommendService recommendService) {
        this(session, recommendService, null);
    }

    public RecommendController(Session session, RecommendService recommendService, OutfitHelper outfitHelper) {
        this.session = session;
        this.recommendService = recommendService;
        this.outfitHelper = outfitHelper;
    }

    /** UC-07 */
    public List<Lookbook> lookbooks(String occasion, String season) {
        return recommendService.findLookbooks(occasion, season);
    }

    /** UC-07 */
    public Lookbook lookbook(Long lookbookId) {
        return recommendService.findLookbook(lookbookId);
    }

    /** UC-08 2단계 */
    public List<Product> trendingItems() {
        return recommendService.findTrendingItems();
    }

    /** UC-08 3단계: 로그인한 회원에게만 보여준다. */
    public List<Product> recommendedForMe() {
        if (!session.isLoggedIn()) {
            return List.of();
        }
        return recommendService.recommendFor(session.currentMember());
    }

    /** UC-07 4단계 코디의 구성 상품. 지워진 상품은 빠진다. 품절은 Product.isSoldOut 으로 확인 (6a). */
    public List<Product> lookbookProducts(Long lookbookId) {
        return recommendService.findLookbookProducts(lookbookId);
    }

    /** UC-07 2단계 상황 필터 값 (등록된 코디에 있는 것만). */
    public List<String> occasions() {
        return recommendService.occasions();
    }

    /** UC-07 2단계 계절 필터 값 (등록된 코디에 있는 것만). */
    public List<String> seasons() {
        return recommendService.seasons();
    }

    /** UC-08 3a: 로그인한 회원이 체형 정보를 등록했는지. false 면 화면이 체형 정보 입력(UC-03)을 권한다. */
    public boolean hasBodyType() {
        return session.isLoggedIn() && recommendService.hasBodyType(session.currentMember());
    }

    /** UC-20 2단계 상황 버튼 ("결혼식·행사", "모임·식사", "여행·나들이", "일상·산책"). */
    public List<String> helperOccasions() {
        return OutfitHelper.OCCASIONS;
    }

    /** UC-20 2단계 계절 버튼. */
    public List<String> helperSeasons() {
        return OutfitHelper.SEASONS;
    }

    /** UC-20 2단계 처음 골라 두는 계절 (오늘 날짜 기준). */
    public String currentSeason() {
        return OutfitHelper.seasonOf(LocalDate.now());
    }

    /**
     * UC-20 3~4단계 상황별 코디 도우미: 최대 3벌. 로그인 없이도 쓸 수 있다.
     * bodyType·ageGroup 은 옷 입혀보기 캐릭터의 신체 스펙(회원이면 등록한 체형)을 넘긴다. 비면 조건 없이 고른다.
     * 고를 상품이 없으면 빈 목록 (4a). 상황·계절이 목록에 없는 값이면 AppException.
     */
    public List<OutfitSuggestion> suggestOutfits(String occasion, String season, String bodyType, String ageGroup) {
        if (outfitHelper == null) {
            throw new IllegalStateException("OutfitHelper 없이 만든 RecommendController 입니다.");
        }
        return outfitHelper.suggest(occasion, season, bodyType, ageGroup);
    }
}
