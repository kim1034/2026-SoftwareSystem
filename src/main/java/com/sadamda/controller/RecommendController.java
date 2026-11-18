package com.sadamda.controller;

import com.sadamda.model.product.Product;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.model.recommend.RecommendService;

import java.util.List;

/** UC-07 룩북 코디 추천 보기, UC-08 유행·맞춤 아이템 추천 보기. */
public class RecommendController {
    private final Session session;
    private final RecommendService recommendService;

    public RecommendController(Session session, RecommendService recommendService) {
        this.session = session;
        this.recommendService = recommendService;
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
}
