package com.sadamda.view.product;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 큰 카테고리 버튼, 검색창, 필터, 큰 이미지 카드 목록 */
public class ProductListPanel extends ScreenPanel {
    public static final String NAME = "productList";

    public ProductListPanel(AppContext context, MainFrame frame) {
        super(context, frame, "상품", "UC-04 카테고리별 상품 조회, UC-05 상품 검색 및 필터");
    }
}
