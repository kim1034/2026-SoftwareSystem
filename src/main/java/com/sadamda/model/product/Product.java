package com.sadamda.model.product;

import java.util.List;

/** 상품 (UC-04~06, UC-16). */
public class Product {
    public enum Status { ON_SALE, SUSPENDED } // UC-16 3a 판매 중지

    private Long id;
    private String name;
    private Category category;
    private int price;
    private String material;
    private List<ProductOption> options;
    private List<String> imageUrls;    // 중장년 모델 착용 사진 포함
    private List<String> ageGroupTags; // 추천 연령대 태그
    private List<String> bodyTypeTags; // 추천 체형 태그
    private boolean trending;          // 유행 아이템 지정 여부 (UC-17)
    private Status status = Status.ON_SALE;

    /** UC-09 AI 합성에 보낼 대표 의류 이미지. 이미지가 없으면 null. (Controller 명세에서 사용) */
    public String getMainImageUrl() {
        return imageUrls == null || imageUrls.isEmpty() ? null : imageUrls.get(0);
    }
}
