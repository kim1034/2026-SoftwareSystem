package com.sadamda.model.member;

/** 회원 도메인 로직. 담당: 백예원 */
public class MemberService {

    /** UC-01 회원가입 (FR-01). 이미 가입된 이메일이면 예외 (4a). */
    public Member signUp(Member member, String rawPassword) {
        throw new UnsupportedOperationException("TODO UC-01");
    }

    /** UC-02 로그인 (FR-02). 계정 정보가 틀리면 예외 (3a). */
    public Member login(String email, String rawPassword) {
        throw new UnsupportedOperationException("TODO UC-02");
    }

    /** UC-02 2a 비밀번호 재설정 링크 발송 (FR-03). */
    public void sendPasswordResetLink(String email) {
        throw new UnsupportedOperationException("TODO UC-02 2a");
    }

    /** UC-03 회원·체형 정보 수정 (FR-04). */
    public Member updateProfile(Member member) {
        throw new UnsupportedOperationException("TODO UC-03");
    }
}
