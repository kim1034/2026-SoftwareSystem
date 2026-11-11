package com.sadamda.view.recommend;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 메인 화면: 이번 시즌 유행 아이템, 나에게 어울리는 상품 */
public class HomePanel extends ScreenPanel {
    public static final String NAME = "home";

    public HomePanel(AppContext context, MainFrame frame) {
        super(context, frame, "홈", "UC-08 유행·맞춤 아이템 추천 보기");
    }
}
