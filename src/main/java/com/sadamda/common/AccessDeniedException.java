package com.sadamda.common;

/** 권한이 없다: 관리자 전용 기능(UC-16~18)을 일반 회원이 썼거나, 남의 주문을 보려 했다. */
public class AccessDeniedException extends AppException {
    public AccessDeniedException() {
        this("관리자만 사용할 수 있습니다.");
    }

    public AccessDeniedException(String message) {
        super(message);
    }
}
