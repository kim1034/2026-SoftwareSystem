package com.sadamda.view.product;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 큰 이미지·착용 사진·사이즈표·리뷰, [장바구니] [바로 구매] [AI 입어보기] [찜] */
public class ProductDetailPanel extends ScreenPanel {
    public static final String NAME = "productDetail";

    public ProductDetailPanel(AppContext context, MainFrame frame) {
        super(context, frame, "상품 상세", "UC-06 상품 상세 조회");
    }
}
