package com.sadamda.external;

import java.util.prefs.Preferences;

/**
 * 화면의 [AI 연결 설정]에서 넣은 값. 이 PC 의 사용자 설정(java.util.prefs)에만 저장하고,
 * 코드·저장소·보관함 어디에도 남기지 않는다. 환경 변수(또는 -D 옵션)가 있으면 그쪽이 우선한다.
 */
public record DressUpSettings(String apiKey, String baseUrl, String model) {
    private static final Preferences PREFS = Preferences.userNodeForPackage(DressUpSettings.class);
    private static final String KEY = "dressUp.apiKey";
    private static final String BASE_URL = "dressUp.baseUrl";
    private static final String MODEL = "dressUp.model";

    public DressUpSettings {
        apiKey = clean(apiKey);
        baseUrl = clean(baseUrl);
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

    private static void put(String name, String value) {
        if (value == null) {
            PREFS.remove(name);
        } else {
            PREFS.put(name, value);
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
