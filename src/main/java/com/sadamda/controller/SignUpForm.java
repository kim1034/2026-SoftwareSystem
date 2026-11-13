package com.sadamda.controller;

import com.sadamda.model.member.BodyProfile;

/** UC-01 가입 양식. bodyProfile 은 선택 입력이라 null 일 수 있다. */
public record SignUpForm(String email, String password, String name, String phone,
                         BodyProfile bodyProfile, boolean agreedToTerms) {
}
