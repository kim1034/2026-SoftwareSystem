package com.sadamda.model.member;

/** 회원 도메인 로직. 담당: 백예원. 실패는 AppException(사용자에게 보여줄 메시지)으로 알린다. */
public class MemberService {

    /** UC-01 회원가입 (FR-01). 이미 가입된 이메일(4a)·형식 오류(4b)면 AppException. */
    public Member signUp(String email, String rawPassword, String name, String phone, BodyProfile bodyProfile) {
        throw new UnsupportedOperationException("TODO UC-01");
    }

    /** UC-02 로그인 (FR-02). 계정 정보가 틀리면 AppException (3a). */
    public Member login(String email, String rawPassword) {
        throw new UnsupportedOperationException("TODO UC-02");
    }

    /** UC-02 2a 비밀번호 재설정 링크 발송 (FR-03). */
    public void sendPasswordResetLink(String email) {
        throw new UnsupportedOperationException("TODO UC-02 2a");
    }

    /** UC-03 2단계 현재 정보 조회. */
    public Member findById(Long memberId) {
        throw new UnsupportedOperationException("TODO UC-03");
    }

    /** UC-03 회원·체형 정보 수정 (FR-04). 형식 오류면 AppException (4a). */
    public Member updateProfile(Long memberId, String phone, String defaultAddress, BodyProfile bodyProfile) {
        throw new UnsupportedOperationException("TODO UC-03");
    }
}
