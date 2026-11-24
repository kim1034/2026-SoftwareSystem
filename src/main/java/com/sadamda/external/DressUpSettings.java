package com.sadamda.external;

import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * 화면의 [AI 연결 설정]에서 넣은 값. 이 PC 의 사용자 설정(java.util.prefs)에만 저장하고,
 * 코드·저장소·보관함 어디에도 남기지 않는다. 화면에서 저장한 열쇠가 있으면 환경 변수보다 먼저 쓴다.
 *
 * 복사·붙여넣기로 흔히 섞여 들어오는 것(앞뒤·중간 공백, 줄바꿈, 보이지 않는 문자, 따옴표, "Bearer ",
 * 주소 끝의 /chat/completions)은 저장하기 전에 지운다.
 */
public record DressUpSettings(String apiKey, String baseUrl, String model) {
    /** 학교에서 쓰는 타임리 AI 연동 키는 이것으로 시작한다. */
    public static final String TIMELY_KEY_PREFIX = "tgpt_";
    /**
     * 타임리 AI 서버 주소. 타임리 키는 TimelyDressUpClient 가 정해진 주소로 보내므로 서비스 주소가 필요 없다.
     * 예전에 저장해 둔 타임리 OpenAI 호환 주소(2026-11-18 종료)로 다른 회사 열쇠를 보내지 않도록 저장값에서 지운다.
     */
    private static final String[] TIMELY_HOSTS = {"timelyai.io", "timelygpt.co.kr"};

    private static final Preferences PREFS = Preferences.userNodeForPackage(DressUpSettings.class);
    private static final String KEY = "dressUp.apiKey";
    private static final String BASE_URL = "dressUp.baseUrl";
    private static final String MODEL = "dressUp.model";
    private static final String[] ENDPOINT_SUFFIXES = {"/chat/completions", "/images/edits", "/images/generations"};

    public DressUpSettings {
        apiKey = normalizeKey(apiKey);
        baseUrl = normalizeBaseUrl(baseUrl);
        model = clean(model);
    }

    public static DressUpSettings load() {
        return new DressUpSettings(PREFS.get(KEY, null), PREFS.get(BASE_URL, null), PREFS.get(MODEL, null));
    }

    public void save() {
        put(KEY, apiKey);
        put(BASE_URL, baseUrl);
        put(MODEL, model);
    }

    public boolean hasKey() {
        return apiKey != null;
    }

    public static boolean isTimelyKey(String key) {
        return key != null && key.startsWith(TIMELY_KEY_PREFIX);
    }

    /**
     * 붙여넣은 열쇠를 다듬는다: 보이지 않는 문자·공백·줄바꿈을 모두 지우고, 감싼 따옴표와 앞의 "Bearer " 를 뗀다.
     * 열쇠에는 공백이 들어가지 않으므로 중간의 공백·줄바꿈도 지운다. 남는 것이 없으면 null.
     */
    public static String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String key = stripInvisible(value).trim();
        key = unquote(key);
        if (key.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            key = key.substring("bearer ".length());
        }
        key = unquote(key.replaceAll("\\s+", ""));
        return key.isEmpty() ? null : key;
    }

    /**
     * 붙여넣은 서비스 주소를 다듬는다: 공백·따옴표·끝의 "/" 를 지우고, 실수로 함께 복사한
     * /chat/completions·/images/edits 같은 끝 경로를 뗀다 (프로그램이 붙이므로 두 번 붙지 않게).
     * 타임리 AI 주소는 지운다 (타임리 키는 주소가 필요 없고, 예전에 저장한 값이 OpenAI 열쇠를 타임리로 보내지 않게).
     */
    public static String normalizeBaseUrl(String value) {
        String url = trimBaseUrl(value);
        return url == null || isTimelyUrl(url) ? null : url;
    }

    static boolean isTimelyUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        for (String host : TIMELY_HOSTS) {
            if (lower.contains(host)) {
                return true;
            }
        }
        return false;
    }

    private static String trimBaseUrl(String value) {
        if (value == null) {
            return null;
        }
        String url = unquote(stripInvisible(value).trim()).replaceAll("\\s+", "");
        boolean changed = true;
        while (changed) {
            changed = false;
            while (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            for (String suffix : ENDPOINT_SUFFIXES) {
                if (url.toLowerCase(Locale.ROOT).endsWith(suffix)) {
                    url = url.substring(0, url.length() - suffix.length());
                    changed = true;
                }
            }
        }
        return url.isEmpty() ? null : url;
    }

    private static void put(String name, String value) {
        if (value == null) {
            PREFS.remove(name);
        } else {
            PREFS.put(name, value);
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String text = unquote(stripInvisible(value).trim());
        return text.isBlank() ? null : text;
    }

    /** 폭 없는 공백(U+200B~U+200D, U+2060), BOM(U+FEFF)과 줄바꿈 없는 공백(U+00A0)을 지운다. */
    private static String stripInvisible(String value) {
        return value.replaceAll("[\\u200B-\\u200D\\u2060\\uFEFF]", "").replace((char) 0x00A0, ' ');
    }

    private static String unquote(String value) {
        String text = value.trim();
        while (text.length() >= 2 && isQuote(text.charAt(0)) && isQuote(text.charAt(text.length() - 1))) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    private static boolean isQuote(char c) {
        return c == '"' || c == '\'' || c == '`' || c == '“' || c == '”' || c == '‘' || c == '’';
    }
}
