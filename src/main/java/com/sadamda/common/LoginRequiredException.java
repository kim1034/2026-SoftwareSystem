package com.sadamda.common;

/** 로그인이 필요한 기능을 비회원이 썼다. View 는 로그인 화면(UC-02)으로 보낸 뒤 원래 화면으로 돌아온다. */
public class LoginRequiredException extends AppException {
    public LoginRequiredException() {
        super("로그인이 필요합니다.");
    }
}
