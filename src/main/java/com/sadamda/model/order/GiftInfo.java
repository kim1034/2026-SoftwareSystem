package com.sadamda.model.order;

/** UC-12 선물하기 수령인 정보와 선물 메시지 (FR-20). */
public record GiftInfo(String recipientName, String recipientPhone, String recipientAddress, String message) {
}
