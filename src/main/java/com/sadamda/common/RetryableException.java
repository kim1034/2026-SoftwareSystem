package com.sadamda.common;

/**
 * 잠시 후 다시 하면 될 수 있는 실패 (AI 합성 시간 초과, 결제 취소·환불 실패 등).
 * View 는 메시지와 함께 [다시 시도] 버튼을 보여준다.
 */
public class RetryableException extends AppException {
    public RetryableException(String message) {
        super(message);
    }
}
