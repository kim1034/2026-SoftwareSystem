package com.sadamda.controller;

import com.sadamda.model.member.Member;

/** 현재 로그인한 사용자. null 이면 비회원. */
public class Session {
    private Member currentMember;

    public Member currentMember() {
        return currentMember;
    }

    public boolean isLoggedIn() {
        return currentMember != null;
    }

    public void login(Member member) {
        this.currentMember = member;
    }

    public void logout() {
        this.currentMember = null;
    }
}
