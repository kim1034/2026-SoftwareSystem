package com.sadamda.model.recommend;

import java.util.ArrayList;
import java.util.List;

/** 룩북 코디 (UC-07, UC-17). */
public class Lookbook {
    private Long id;
    private String title;
    private String imageUrl;
    private String occasion; // 모임, 여행, 일상 등
    private String season;
    private List<Long> productIds = new ArrayList<>(); // 구성 상품

    public Lookbook() {
    }

    public Lookbook(String title, String imageUrl, String occasion, String season, List<Long> productIds) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.occasion = occasion;
        this.season = season;
        setProductIds(productIds);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds == null ? new ArrayList<>() : new ArrayList<>(productIds);
    }

    @Override
    public String toString() {
        return title;
    }
}
