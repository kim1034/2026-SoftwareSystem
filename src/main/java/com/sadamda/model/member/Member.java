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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(String defaultAddress) { this.defaultAddress = defaultAddress; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public BodyProfile getBodyProfile() { return bodyProfile; }
    public void setBodyProfile(BodyProfile bodyProfile) { this.bodyProfile = bodyProfile; }
}
