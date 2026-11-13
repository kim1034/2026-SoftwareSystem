package com.sadamda.controller;

import com.sadamda.common.AccessDeniedException;
import com.sadamda.common.LoginRequiredException;
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

    public boolean isAdmin() {
        return isLoggedIn() && currentMember.getRole() == Member.Role.ADMIN;
    }

    /** 로그인하지 않았으면 LoginRequiredException. */
    public Member requireLogin() {
        if (!isLoggedIn()) {
            throw new LoginRequiredException();
        }
        return currentMember;
    }

    /** 로그인하지 않았으면 LoginRequiredException, 관리자가 아니면 AccessDeniedException. */
    public Member requireAdmin() {
        Member member = requireLogin();
        if (member.getRole() != Member.Role.ADMIN) {
            throw new AccessDeniedException();
        }
        return member;
    }

    public void login(Member member) {
        this.currentMember = member;
    }

    public void logout() {
        this.currentMember = null;
    }
}
