package com.sadamda.view.order;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 주문 목록·상세·배송 상태, [주문 취소] [교환·반품 신청] [리뷰 쓰기] */
public class OrderHistoryPanel extends ScreenPanel {
    public static final String NAME = "orderHistory";

    public OrderHistoryPanel(AppContext context, MainFrame frame) {
        super(context, frame, "주문 내역", "UC-13 주문·배송 조회 및 취소·교환·반품, UC-14 리뷰 작성");
    }
}
