package com.sadamda.view.member;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 연락처·기본 배송지·연령대·체형 정보 수정 */
public class MyInfoPanel extends ScreenPanel {
    public static final String NAME = "myInfo";

    public MyInfoPanel(AppContext context, MainFrame frame) {
        super(context, frame, "내 정보", "UC-03 회원·체형 정보 관리");
    }
}
