package com.sadamda.view.admin;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 관리자 화면. 담당: 김대원 */
public class AdminPanel extends ScreenPanel {
    public static final String NAME = "admin";

    public AdminPanel(AppContext context, MainFrame frame) {
        super(context, frame, "관리자", "UC-16 상품 관리, UC-17 룩북·유행 아이템 관리, UC-18 주문 관리");
    }
}
