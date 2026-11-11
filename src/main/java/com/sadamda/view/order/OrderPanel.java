package com.sadamda.view.order;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 배송지·결제 수단, [선물로 보내기] 수령인·메시지 입력 */
public class OrderPanel extends ScreenPanel {
    public static final String NAME = "order";

    public OrderPanel(AppContext context, MainFrame frame) {
        super(context, frame, "주문서", "UC-11 주문 및 결제, UC-12 선물하기");
    }
}
