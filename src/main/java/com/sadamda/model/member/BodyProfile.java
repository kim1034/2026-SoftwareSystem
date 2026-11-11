package com.sadamda.model.member;

/** 체형 정보 (UC-01 선택 입력, UC-03 수정). 맞춤 추천(UC-08)에 사용된다. */
public record BodyProfile(String ageGroup, Integer heightCm, String usualSize, String bodyType) {
}
