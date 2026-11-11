package com.sadamda.external;

import java.util.UUID;

/** 실제 PG 연동 전까지 쓰는 가짜 결제 시스템. 항상 승인한다. */
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult approve(long amount, String method) {
        return new PaymentResult(true, UUID.randomUUID().toString(), "mock 승인");
    }

    @Override
    public PaymentResult cancel(String paymentId) {
        return new PaymentResult(true, paymentId, "mock 취소");
    }
}
