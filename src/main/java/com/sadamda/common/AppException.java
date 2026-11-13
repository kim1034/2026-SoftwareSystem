package com.sadamda.common;

/**
 * 사용자에게 보여줄 오류. message 는 View 가 그대로 큰 글씨로 표시한다.
 * Model·Controller 는 검증 실패, 대안 흐름(예: UC-01 4a "이미 가입된 이메일입니다")을 이 예외로 알린다.
 */
public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }
}
