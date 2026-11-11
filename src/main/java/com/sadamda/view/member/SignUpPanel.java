package com.sadamda.view.member;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 필수: 이메일·비밀번호·이름·연락처 / 선택: 연령대·키·평소 사이즈·체형 */
public class SignUpPanel extends ScreenPanel {
    public static final String NAME = "signUp";

    public SignUpPanel(AppContext context, MainFrame frame) {
        super(context, frame, "회원가입", "UC-01 회원가입");
    }
}
