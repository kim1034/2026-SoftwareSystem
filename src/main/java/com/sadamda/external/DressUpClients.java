package com.sadamda.external;

/**
 * 설정을 보고 옷 입혀보기 합성기를 고른다. 이용 열쇠는 코드나 저장소에 넣지 않는다.
 *
 * 설정을 찾는 순서: ① 화면의 [AI 연결 설정]에서 저장한 값 (DressUpSettings) → ② 환경 변수 또는 같은 이름의 -D 옵션
 * 화면에서 열쇠를 저장했으면 그 열쇠·주소·모델을 쓴다. 다른 프로그램용으로 OPENAI_API_KEY 를 넣어 둔 PC 에서
 * 화면에 넣은 열쇠가 무시되지 않게 하기 위해서다. 화면에 열쇠가 없을 때만 환경 변수를 쓴다.
 *
 * 기본은 OpenAI 공식 이미지 API(gpt-image-2): OpenAI 열쇠("sk-")만 넣고 서비스 주소는 비워 두면 된다.
 *
 * 서비스 고르기 (SADAMDA_AI_PROVIDER = openai | gemini, 없으면 자동)
 * - 자동: Google 키("AIza" 로 시작)는 gemini, 그 밖의 키는 OpenAI 형식(공식 또는 호환 중계 서비스)
 * - 키가 없으면 UnconnectedDressUpClient: 외부로 아무것도 보내지 않고 연결 설정을 안내한다
 * - 타임리 AI 연동 키("tgpt_")는 TimelyDressUpClient: 타임리 SDK API 의 내장 도구 edit_image 로 그린다.
 *   (2026-11-18 에 타임리 OpenAI 호환 주소가 닫혀서 chat 방식으로는 그릴 수 없다. 서비스 주소·모델 이름은 쓰지 않는다)
 *
 * OpenAI 형식 (공식 또는 호환 중계 서비스)
 * - OPENAI_API_KEY (또는 SADAMDA_AI_KEY, GEMINI_API_KEY) : 이용 열쇠
 * - OPENAI_BASE_URL                       : 중계 서비스 주소. 기본 https://api.openai.com/v1
 * - SADAMDA_AI_API                        : images(이미지 API) 또는 chat(chat/completions).
 *                                           기본: 공식 주소면 images, 중계 서비스 주소면 chat
 * - SADAMDA_AI_MODEL                      : 모델 이름. 기본 images=gpt-image-2, chat=OpenAiDressUpClient.DEFAULT_CHAT_MODEL
 * - SADAMDA_AI_QUALITY                    : low·medium·high·auto (images 방식). 기본 medium
 *
 * Gemini
 * - GEMINI_API_KEY, SADAMDA_AI_MODEL(기본 gemini-3.1-flash-image), SADAMDA_AI_ENDPOINT
 *
 * 공통: SADAMDA_AI_TIMEOUT 기다리는 시간(초). 기본 DEFAULT_TIMEOUT_SECONDS
 */
public final class DressUpClients {
    /**
     * 기본 대기 시간(초). 실제 합성은 옷 1~3벌에 15초 안팎, 5벌이면 25초 안팎이 걸려
     * NFR-05 의 목표 30초에 가깝다. 목표를 조금 넘겼다고 바로 실패 처리하지 않도록 여유를 둔다.
     * 화면은 그동안 진행 상태를 보여주고, 이 시간을 넘으면 [다시 시도]를 보여준다 (UC-09 7b).
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private DressUpClients() {
    }

    /** 저장된 화면 설정 → 환경 변수 순으로 찾아 합성기를 만든다. */
    public static DressUpImageClient create() {
        return create(DressUpSettings.load());
    }

    public static DressUpImageClient create(DressUpSettings saved) {
        String key;
        String baseUrl;
        String model;
        String environmentUrl = DressUpSettings.normalizeBaseUrl(setting("OPENAI_BASE_URL"));
        if (saved.apiKey() != null) {
            // 화면에서 넣은 열쇠: 화면에서 넣은 주소 → 환경 변수 주소
            key = saved.apiKey();
            baseUrl = first(saved.baseUrl(), environmentUrl);
            model = first(saved.model(), setting("SADAMDA_AI_MODEL"));
        } else {
            key = environmentKey();
            if (key == null) {
                return new UnconnectedDressUpClient();
            }
            baseUrl = first(environmentUrl, saved.baseUrl());
            model = first(setting("SADAMDA_AI_MODEL"), saved.model());
        }
        if (DressUpSettings.isTimelyKey(key)) {
            return new TimelyDressUpClient(key, timeout());
        }

        String provider = setting("SADAMDA_AI_PROVIDER");
        if (provider == null) {
            provider = key.startsWith("AIza") ? "gemini" : "openai";
        }
        int timeout = timeout();
        if ("gemini".equalsIgnoreCase(provider)) {
            return new GeminiDressUpClient(key, model, setting("SADAMDA_AI_ENDPOINT"), timeout);
        }
        return new OpenAiDressUpClient(key, baseUrl, setting("SADAMDA_AI_API"), model,
                first(setting("SADAMDA_AI_QUALITY"), "medium"), timeout);
    }

    /** 환경 변수(또는 -D 옵션)에 열쇠가 있는지. 화면에 열쇠를 저장하지 않았을 때만 이 열쇠를 쓴다. */
    public static boolean keyFromEnvironment() {
        return environmentKey() != null;
    }

    private static String environmentKey() {
        return DressUpSettings.normalizeKey(
                first(first(setting("SADAMDA_AI_KEY"), setting("OPENAI_API_KEY")), setting("GEMINI_API_KEY")));
    }

    private static int timeout() {
        String text = setting("SADAMDA_AI_TIMEOUT");
        if (text != null) {
            try {
                return Math.max(5, Integer.parseInt(text));
            } catch (NumberFormatException ignored) {
                // 숫자가 아니면 기본값을 쓴다
            }
        }
        return DEFAULT_TIMEOUT_SECONDS;
    }

    /** -D 옵션이 있으면 그것을, 없으면 환경 변수를 쓴다. 앞뒤 공백은 지운다. */
    private static String setting(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String first(String a, String b) {
        return a != null ? a : b;
    }
}
