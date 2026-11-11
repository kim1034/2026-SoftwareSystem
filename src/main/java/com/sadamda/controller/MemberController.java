package com.sadamda.controller;

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

    /** UC-01: 가입 후 바로 로그인 상태가 된다. */
    public Member signUp(Member member, String rawPassword) {
        Member created = memberService.signUp(member, rawPassword);
        session.login(created);
        return created;
    }

    /** UC-02 */
    public Member login(String email, String rawPassword) {
        Member member = memberService.login(email, rawPassword);
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

    /** UC-03 */
    public Member updateProfile(Member member) {
        return memberService.updateProfile(member);
    }
}
