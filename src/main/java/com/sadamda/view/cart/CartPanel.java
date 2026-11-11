package com.sadamda.view.cart;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 담긴 상품·수량·총액, 찜 목록, [주문하기] */
public class CartPanel extends ScreenPanel {
    public static final String NAME = "cart";

    public CartPanel(AppContext context, MainFrame frame) {
        super(context, frame, "장바구니", "UC-10 장바구니·찜 관리");
    }
}
