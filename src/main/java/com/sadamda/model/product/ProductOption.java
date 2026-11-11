package com.sadamda.model.product;

/** 사이즈·색상별 옵션과 재고. 재고가 0이면 품절 (UC-06 4b). */
public record ProductOption(Long id, String size, String color, int stock) {
}
