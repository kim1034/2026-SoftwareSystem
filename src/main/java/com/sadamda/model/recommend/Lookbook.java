package com.sadamda.model.recommend;

import java.util.List;

/** 룩북 코디 (UC-07, UC-17). */
public class Lookbook {
    private Long id;
    private String title;
    private String imageUrl;
    private String occasion; // 모임, 여행, 일상 등
    private String season;
    private List<Long> productIds; // 구성 상품
}
