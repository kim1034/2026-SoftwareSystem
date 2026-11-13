package com.sadamda.controller;

import com.sadamda.common.AppException;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.model.member.MemberService;

/** UC-01 회원가입, UC-02 로그인, UC-03 회원·체형 정보 관리. */
public class MemberController {
    private final Session session;
    private final MemberService memberService;

    public MemberController(Session session, MemberService memberService) {
        this.session = session;
        this.memberService = memberService;
    }

    /** UC-01 3~5단계. 가입 후 바로 로그인 상태가 된다. */
    public Member signUp(SignUpForm form) {
        if (!form.agreedToTerms()) {
            throw new AppException("약관에 동의해 주세요.");
        }
        Member created = memberService.signUp(form.email(), form.password(), form.name(), form.phone(),
                form.bodyProfile());
        session.login(created);
        return created;
    }

    /** UC-02 2~4단계. View 는 반환된 회원이 관리자면 관리자 화면으로 이동한다. */
    public Member login(String email, String password) {
        Member member = memberService.login(email, password);
        session.login(member);
        return member;
    }

    public void logout() {
        session.logout();
    }

    /** UC-02 2a */
    public void requestPasswordReset(String email) {
        memberService.sendPasswordResetLink(email);
    }

    /** 현재 로그인한 회원. 비회원이면 null. */
    public Member currentMember() {
        return session.currentMember();
    }

    /** UC-03 2단계. 로그인 필요. */
    public Member myInfo() {
        return memberService.findById(session.requireLogin().getId());
    }

    /** UC-03 3~4단계. 로그인 필요. */
    public Member updateProfile(String phone, String defaultAddress, BodyProfile bodyProfile) {
        Member updated = memberService.updateProfile(session.requireLogin().getId(), phone, defaultAddress,
                bodyProfile);
        session.login(updated);
        return updated;
    }
}
