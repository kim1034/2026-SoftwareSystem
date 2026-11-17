package com.sadamda.model.product;

/**
 * 큰 카테고리 버튼으로 보여주는 상품 분류 (FR-05).
 * 모자·신발·양말·주얼리는 데모 데이터와 옷 입혀보기(UC-09)에 맞춰 추가했다.
 */
public enum Category {
    TOP("상의"), BOTTOM("하의"), DRESS("원피스"), OUTER("아우터"),
    HAT("모자"), SHOES("신발"), SOCKS("양말"), JEWELRY("주얼리");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 화면·데이터의 한글 이름으로 카테고리를 찾는다. 없으면 null. */
    public static Category fromLabel(String label) {
        for (Category category : values()) {
            if (category.label.equals(label)) {
                return category;
            }
        }
        return null;
    }
}
