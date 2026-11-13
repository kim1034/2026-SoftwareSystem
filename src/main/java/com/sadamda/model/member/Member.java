package com.sadamda.model.member;

/** 회원 (UC-01~03). 관리자는 role 로 구분한다. */
public class Member {
    public enum Role { MEMBER, ADMIN }

    private Long id;
    private String email;
    private String passwordHash; // NFR-07: bcrypt 해시 저장
    private String name;
    private String phone;
    private String defaultAddress;
    private Role role = Role.MEMBER;
    private BodyProfile bodyProfile; // 선택 입력

    // Controller 명세에서 쓰는 getter. 나머지 접근자는 Model 담당이 추가한다.
    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public String getDefaultAddress() {
        return defaultAddress;
    }
}
