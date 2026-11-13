package com.sadamda.model.tryon;

import com.sadamda.model.product.Category;

/** 옷 입혀보기에서 캐릭터에 입히는 자리 5곳. 화면 오른쪽에 이 순서로 보여준다. */
public enum DressUpSlot {
    HAT("모자", Category.HAT, "hat", "no hat"),
    TOP("상의", Category.TOP, "top", "a plain white fitted t-shirt"),
    BOTTOM("하의", Category.BOTTOM, "bottom", "plain light-gray straight trousers"),
    JEWELRY("주얼리", Category.JEWELRY, "jewelry", "no jewelry"),
    SHOES("신발", Category.SHOES, "shoes", "plain white flat shoes");

    private final String label;
    private final Category category;
    private final String english;
    private final String whenEmpty;

    DressUpSlot(String label, Category category, String english, String whenEmpty) {
        this.label = label;
        this.category = category;
        this.english = english;
        this.whenEmpty = whenEmpty;
    }

    public String label() {
        return label;
    }

    /** 이 자리에 입힐 수 있는 상품 카테고리. */
    public Category category() {
        return category;
    }

    public String english() {
        return english;
    }

    /** 아무것도 고르지 않았을 때 캐릭터가 입는 기본 차림 (AI 에 보내는 설명). */
    public String whenEmpty() {
        return whenEmpty;
    }
}
