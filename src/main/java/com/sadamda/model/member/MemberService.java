package com.sadamda.model.member;

import com.sadamda.common.AppException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** 회원 도메인 로직. 담당: 백예원. 실패는 AppException(사용자에게 보여줄 메시지)으로 알린다. */
public class MemberService {
    public static final String ADMIN_EMAIL = "admin@sadamda.com";
    public static final String ADMIN_PASSWORD = "admin1234";
    public static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^01[016789]-?\\d{3,4}-?\\d{4}$");

    private final Map<Long, Member> members = new LinkedHashMap<>();
    private long nextId = 1;

    public MemberService() {
        Member admin = new Member();
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(PasswordHasher.hash(ADMIN_PASSWORD));
        admin.setName("관리자");
        admin.setPhone("010-0000-0000");
        admin.setRole(Member.Role.ADMIN);
        store(admin);
    }

    /** UC-01 회원가입 (FR-01). 이미 가입된 이메일(4a)·형식 오류(4b)면 AppException. */
    public synchronized Member signUp(String email, String rawPassword, String name, String phone, BodyProfile bodyProfile) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            throw new AppException("이메일을 입력해 주세요.");
        }
        if (!EMAIL.matcher(normalizedEmail).matches()) {
            throw new AppException("이메일 형식이 맞지 않습니다.");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new AppException("비밀번호는 8자 이상 입력해 주세요.");
        }
        if (isBlank(name)) {
            throw new AppException("이름을 입력해 주세요.");
        }
        String normalizedPhone = normalizePhone(phone);
        validateBodyProfile(bodyProfile);
        if (findByEmail(normalizedEmail) != null) {
            throw new AppException("이미 가입된 이메일입니다.");
        }

        Member member = new Member();
        member.setEmail(normalizedEmail);
        member.setPasswordHash(PasswordHasher.hash(rawPassword));
        member.setName(name.trim());
        member.setPhone(normalizedPhone);
        member.setBodyProfile(bodyProfile);
        return store(member);
    }

    /** UC-02 로그인 (FR-02). 계정 정보가 틀리면 AppException (3a). */
    public synchronized Member login(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new AppException("이메일과 비밀번호를 입력해 주세요.");
        }
        Member member = findByEmail(normalizedEmail);
        if (member == null || !PasswordHasher.matches(rawPassword, member.getPasswordHash())) {
            throw new AppException("이메일 또는 비밀번호가 맞지 않습니다.");
        }
        return member;
    }

    /** UC-02 2a 비밀번호 재설정 링크 발송 (FR-03). */
    public synchronized void sendPasswordResetLink(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            throw new AppException("가입하신 이메일을 입력해 주세요.");
        }
        if (!EMAIL.matcher(normalizedEmail).matches()) {
            throw new AppException("이메일 형식이 맞지 않습니다.");
        }
        if (findByEmail(normalizedEmail) == null) {
            throw new AppException("가입된 이메일이 아닙니다.");
        }
    }

    /** UC-03 2단계 현재 정보 조회. */
    public synchronized Member findById(Long memberId) {
        Member member = memberId == null ? null : members.get(memberId);
        if (member == null) {
            throw new AppException("회원 정보를 찾을 수 없습니다.");
        }
        return member;
    }

    /** UC-03 회원·체형 정보 수정 (FR-04). 형식 오류면 AppException (4a). */
    public synchronized Member updateProfile(Long memberId, String phone, String defaultAddress, BodyProfile bodyProfile) {
        Member member = findById(memberId);
        String normalizedPhone = normalizePhone(phone);
        validateBodyProfile(bodyProfile);
        member.setPhone(normalizedPhone);
        member.setDefaultAddress(isBlank(defaultAddress) ? null : defaultAddress.trim());
        member.setBodyProfile(bodyProfile);
        return member;
    }

    private Member store(Member member) {
        member.setId(nextId++);
        members.put(member.getId(), member);
        return member;
    }

    private Member findByEmail(String normalizedEmail) {
        for (Member member : members.values()) {
            if (member.getEmail().equals(normalizedEmail)) {
                return member;
            }
        }
        return null;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (isBlank(phone)) {
            throw new AppException("연락처를 입력해 주세요.");
        }
        String trimmed = phone.trim();
        if (!PHONE.matcher(trimmed).matches()) {
            throw new AppException("연락처 형식이 맞지 않습니다. 예: 010-1234-5678");
        }
        String digits = trimmed.replace("-", "");
        int middleLength = digits.length() - 7;
        return digits.substring(0, 3) + "-" + digits.substring(3, 3 + middleLength) + "-" + digits.substring(3 + middleLength);
    }

    private void validateBodyProfile(BodyProfile bodyProfile) {
        if (bodyProfile == null || bodyProfile.heightCm() == null) {
            return;
        }
        int height = bodyProfile.heightCm();
        if (height < 100 || height > 220) {
            throw new AppException("키는 100~220cm 사이로 입력해 주세요.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}