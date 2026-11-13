package com.sadamda.external;

/**
 * 실행 환경 설정을 보고 옷 입혀보기 합성기를 고른다. 이용 열쇠는 코드나 저장소에 넣지 않는다.
 *
 * 서비스 고르기 (SADAMDA_AI_PROVIDER = openai | gemini | local, 없으면 자동)
 * - 자동: OPENAI_API_KEY 가 있으면 openai, GEMINI_API_KEY 가 있으면 키 모양으로 판단
 *         (Google 키는 "AIza" 로 시작 → gemini, 그 밖의 키 → OpenAI 호환 중계 서비스)
 * - 키가 하나도 없으면 외부로 아무것도 보내지 않는 미리보기 합성기
 *
 * OpenAI 형식 (공식 또는 호환 중계 서비스)
 * - OPENAI_API_KEY (또는 SADAMDA_AI_KEY)   : 이용 열쇠
 * - OPENAI_BASE_URL                       : 중계 서비스 주소. 기본 https://api.openai.com/v1
 * - SADAMDA_AI_API                        : images(이미지 API) 또는 chat(chat/completions).
 *                                           기본: 공식 주소면 images, 중계 서비스 주소면 chat
 * - SADAMDA_AI_MODEL                      : 모델 이름. 기본 images=gpt-image-2, chat=google/gemini-3.1-flash-image
 * - SADAMDA_AI_QUALITY                    : low·medium·high·auto (images 방식). 기본 medium
 *
 * Gemini
 * - GEMINI_API_KEY, SADAMDA_AI_MODEL(기본 gemini-3.1-flash-image), SADAMDA_AI_ENDPOINT
 *
 * 공통: SADAMDA_AI_TIMEOUT 기다리는 시간(초). 기본 30 (NFR-05)
 * 모든 값은 같은 이름의 -D 옵션으로도 줄 수 있다. 예: -DOPENAI_BASE_URL=https://relay.example.com/v1
 */
public final class DressUpClients {
    private DressUpClients() {
    }

    public static DressUpImageClient fromEnvironment() {
        String provider = setting("SADAMDA_AI_PROVIDER");
        String openAiKey = first(setting("SADAMDA_AI_KEY"), setting("OPENAI_API_KEY"));
        String geminiKey = setting("GEMINI_API_KEY");

        if (provider == null) {
            if (openAiKey != null) {
                provider = "openai";
            } else if (geminiKey != null) {
                provider = geminiKey.startsWith("AIza") ? "gemini" : "openai";
            } else {
                provider = "local";
            }
        }
        int timeout = timeout();
        switch (provider.trim().toLowerCase()) {
            case "openai" -> {
                String key = first(openAiKey, geminiKey);
                if (key == null) {
                    return new LocalDressUpClient();
                }
                return new OpenAiDressUpClient(key, setting("OPENAI_BASE_URL"), setting("SADAMDA_AI_API"),
                        setting("SADAMDA_AI_MODEL"), first(setting("SADAMDA_AI_QUALITY"), "medium"), timeout);
            }
            case "gemini" -> {
                String key = first(geminiKey, openAiKey);
                if (key == null) {
                    return new LocalDressUpClient();
                }
                return new GeminiDressUpClient(key, setting("SADAMDA_AI_MODEL"), setting("SADAMDA_AI_ENDPOINT"), timeout);
            }
            default -> {
                return new LocalDressUpClient();
            }
        }
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
        return AiImageClient.TIMEOUT_SECONDS;
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
