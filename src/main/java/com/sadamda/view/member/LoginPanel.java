package com.sadamda.view.member;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 이메일·비밀번호, [비밀번호 찾기]. 관리자는 로그인 후 관리자 화면으로 이동 */
public class LoginPanel extends ScreenPanel {
    public static final String NAME = "login";

    public LoginPanel(AppContext context, MainFrame frame) {
        super(context, frame, "로그인", "UC-02 로그인");
    }
}
