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
}
