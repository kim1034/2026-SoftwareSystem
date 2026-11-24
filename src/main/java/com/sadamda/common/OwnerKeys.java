package com.sadamda.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 이 PC 에 회원별 파일(코디북·출석)을 둘 때 쓰는 회원 열쇠.
 * 회원 번호는 프로그램을 켤 때마다 다시 매겨지므로 이메일을 쓰고, 이메일을 그대로 드러내지 않도록
 * SHA-256 앞 16자리로 바꾼다. 대소문자·앞뒤 공백이 달라도 같은 회원으로 본다.
 */
public final class OwnerKeys {
    private OwnerKeys() {
    }

    /** 이메일이 비었으면 null. */
    public static String of(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
