package com.sadamda.view.member;

import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;

import java.util.prefs.Preferences;

/**
 * FR-02 로그인 상태 유지. [로그인 상태 유지]를 체크하고 로그인한 회원을 이 PC 의 사용자 설정에 저장한다.
 * 회원 정보는 앱을 끄면 사라지므로, 새로 가입한 회원도 다음에 다시 만들 수 있게 회원 정보를 함께 저장한다.
 * 비밀번호는 원문이 아니라 이미 만들어 둔 해시만 저장한다. 앱을 켤 때 MainFrame 이 이 회원으로 다시 로그인하고,
 * 로그아웃하면 지운다.
 */
public final class KeepLogin {
    private static final Preferences PREFS = Preferences.userNodeForPackage(KeepLogin.class);
    private static final String EMAIL = "keepLogin.email";
    private static final String PASSWORD_HASH = "keepLogin.passwordHash";
    private static final String NAME = "keepLogin.name";
    private static final String PHONE = "keepLogin.phone";
    private static final String ADDRESS = "keepLogin.defaultAddress";
    private static final String FONT_SIZE = "keepLogin.fontSize";
    private static final String AGE_GROUP = "keepLogin.ageGroup";
    private static final String HEIGHT = "keepLogin.heightCm";
    private static final String SIZE = "keepLogin.usualSize";
    private static final String BODY_TYPE = "keepLogin.bodyType";
    private static final String[] ALL = {EMAIL, PASSWORD_HASH, NAME, PHONE, ADDRESS, FONT_SIZE,
            AGE_GROUP, HEIGHT, SIZE, BODY_TYPE};

    private KeepLogin() {
    }

    /** 저장해 둔 회원. 없으면 null. 이메일·비밀번호 해시만 믿을 수 있고, 나머지는 다시 만들 때 쓰는 값이다. */
    public static Member saved() {
        String email = PREFS.get(EMAIL, null);
        if (email == null) {
            return null;
        }
        Member member = new Member();
        member.setEmail(email);
        member.setPasswordHash(PREFS.get(PASSWORD_HASH, null));
        member.setName(PREFS.get(NAME, null));
        member.setPhone(PREFS.get(PHONE, null));
        member.setDefaultAddress(PREFS.get(ADDRESS, null));
        member.setFontSize(PREFS.get(FONT_SIZE, null));
        String height = PREFS.get(HEIGHT, null);
        BodyProfile body = new BodyProfile(PREFS.get(AGE_GROUP, null), height == null ? null : Integer.valueOf(height),
                PREFS.get(SIZE, null), PREFS.get(BODY_TYPE, null));
        boolean hasBody = body.ageGroup() != null || body.heightCm() != null || body.usualSize() != null
                || body.bodyType() != null;
        member.setBodyProfile(hasBody ? body : null);
        return member;
    }

    /** 이 회원으로 로그인 상태를 유지한다. 내 정보가 바뀌었을 수 있어 앱을 닫을 때도 다시 부른다. */
    public static void remember(Member member) {
        put(EMAIL, member.getEmail());
        put(PASSWORD_HASH, member.getPasswordHash());
        put(NAME, member.getName());
        put(PHONE, member.getPhone());
        put(ADDRESS, member.getDefaultAddress());
        put(FONT_SIZE, member.getFontSize());
        BodyProfile body = member.getBodyProfile();
        put(AGE_GROUP, body == null ? null : body.ageGroup());
        put(HEIGHT, body == null || body.heightCm() == null ? null : body.heightCm().toString());
        put(SIZE, body == null ? null : body.usualSize());
        put(BODY_TYPE, body == null ? null : body.bodyType());
    }

    public static void forget() {
        for (String key : ALL) {
            PREFS.remove(key);
        }
    }

    private static void put(String key, String value) {
        if (value == null) {
            PREFS.remove(key);
        } else {
            PREFS.put(key, value);
        }
    }
}
