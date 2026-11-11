package com.sadamda.model.product;

/** UC-05 검색·필터 조건 (FR-06, FR-07). null 이면 해당 조건을 적용하지 않는다. */
public record SearchFilter(String keyword, String ageGroup, String bodyType, String size,
                           Integer minPrice, Integer maxPrice, String color, String sort) {
}
