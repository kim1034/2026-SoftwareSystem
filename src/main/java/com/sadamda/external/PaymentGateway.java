package com.sadamda.external;

/** 외부 결제 시스템(PG) 연동 인터페이스 (Secondary Actor: 결제 시스템). 담당: 김대원 */
public interface PaymentGateway {

    /** UC-11 5~6단계 결제 승인 요청. 실패·취소 시 approved=false (6a). */
    PaymentResult approve(long amount, String method);

    /** 결제 취소·환불 (UC-11 7a, UC-13 5단계, UC-18 5단계). */
    PaymentResult cancel(String paymentId);

    record PaymentResult(boolean approved, String paymentId, String message) {
    }
}
