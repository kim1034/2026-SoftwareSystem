package com.sadamda.model.product;

/** 큰 카테고리 버튼으로 보여주는 상품 분류 (FR-05). */
public enum Category {
    TOP("상의"), BOTTOM("하의"), DRESS("원피스"), OUTER("아우터");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
