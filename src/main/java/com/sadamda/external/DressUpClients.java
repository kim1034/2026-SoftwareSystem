package com.sadamda.external;

/**
 * 실행 환경 설정을 보고 옷 입혀보기 합성기를 고른다. 이용 열쇠는 코드나 저장소에 넣지 않는다.
 *
 * 환경 변수 (또는 같은 뜻의 -D 옵션)
 * - GEMINI_API_KEY   (-Dsadamda.ai.key)      : 있으면 Gemini 로 합성, 없으면 미리보기 합성기
 * - SADAMDA_AI_MODEL (-Dsadamda.ai.model)    : 모델 이름. 기본 gemini-3.1-flash-image
 * - SADAMDA_AI_ENDPOINT (-Dsadamda.ai.endpoint) : API 주소. 기본 Gemini Interactions API
 * - SADAMDA_AI_TIMEOUT (-Dsadamda.ai.timeout) : 기다리는 시간(초). 기본 30 (NFR-05)
 */
public final class DressUpClients {
    private DressUpClients() {
    }

    public static DressUpImageClient fromEnvironment() {
        String apiKey = setting("sadamda.ai.key", "GEMINI_API_KEY");
        if (apiKey == null) {
            return new LocalDressUpClient();
        }
        int timeout = AiImageClient.TIMEOUT_SECONDS;
        String timeoutText = setting("sadamda.ai.timeout", "SADAMDA_AI_TIMEOUT");
        if (timeoutText != null) {
            try {
                timeout = Math.max(5, Integer.parseInt(timeoutText.trim()));
            } catch (NumberFormatException ignored) {
                // 숫자가 아니면 기본값을 쓴다
            }
        }
        return new GeminiDressUpClient(apiKey.trim(),
                setting("sadamda.ai.model", "SADAMDA_AI_MODEL"),
                setting("sadamda.ai.endpoint", "SADAMDA_AI_ENDPOINT"), timeout);
    }

    private static String setting(String property, String env) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(env);
        }
        return value == null || value.isBlank() ? null : value;
    }
}
