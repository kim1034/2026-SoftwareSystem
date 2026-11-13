package com.sadamda.common;

/** 관리자 전용 기능(UC-16~18)을 관리자가 아닌 사용자가 썼다. */
public class AccessDeniedException extends AppException {
    public AccessDeniedException() {
        super("관리자만 사용할 수 있습니다.");
    }
}
