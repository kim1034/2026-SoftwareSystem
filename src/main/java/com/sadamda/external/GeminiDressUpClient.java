package com.sadamda.external;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google Gemini 이미지 생성 모델(Interactions API)로 옷 입혀보기 이미지를 만든다.
 * 요청: POST {endpoint}  본문 {"model": ..., "input": [{"type":"text"}, {"type":"image", "mime_type", "data"} ...]}
 * 응답: 본문 안의 마지막 이미지(base64 "data")를 결과로 쓴다.
 * 외부 라이브러리 없이 java.net.http 만 쓴다.
 */
public class GeminiDressUpClient implements DressUpImageClient {
    public static final String DEFAULT_MODEL = "gemini-3.1-flash-image";
    public static final String DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions";

    private static final Pattern IMAGE_DATA = Pattern.compile("\"data\"\\s*:\\s*\"([A-Za-z0-9+/=_-]{200,}+)\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final int timeoutSeconds;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public GeminiDressUpClient(String apiKey, String model, String endpoint, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
        this.endpoint = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public String name() {
        return "Gemini (" + model + ")";
    }

    @Override
    public byte[] dressUp(Request request) throws DressUpException {
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(request), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new DressUpException(timeoutSeconds + "초 안에 그림이 오지 않았습니다. 다시 시도해 주세요.");
        } catch (IOException e) {
            throw new DressUpException("그림 만드는 곳에 연결하지 못했습니다. 인터넷 연결을 확인해 주세요.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DressUpException("그림 만들기를 멈췄습니다.");
        }
        if (response.statusCode() != 200) {
            throw new DressUpException(errorMessage(response.statusCode(), response.body()));
        }
        byte[] image = lastImage(response.body());
        if (image == null) {
            throw new DressUpException("그림을 받지 못했습니다. 옷을 바꾸거나 다시 시도해 주세요.");
        }
        return image;
    }

    /** 보내는 순서: 지시문 → 기본 캐릭터(Image 1) → 자리 순서대로 상품 사진. 지시문의 "Image N" 번호와 맞춘다. */
    String requestBody(Request request) {
        String prompt = request.prompt();
        List<byte[]> images = request.images();

        StringBuilder json = new StringBuilder();
        json.append("{\"model\":\"").append(escape(model)).append("\",\"store\":false,\"input\":[");
        json.append("{\"type\":\"text\",\"text\":\"").append(escape(prompt)).append("\"}");
        for (byte[] image : images) {
            json.append(",{\"type\":\"image\",\"mime_type\":\"").append(mimeType(image))
                    .append("\",\"data\":\"").append(Base64.getEncoder().encodeToString(image)).append("\"}");
        }
        json.append("]}");
        return json.toString();
    }

    static byte[] lastImage(String body) {
        Matcher matcher = IMAGE_DATA.matcher(body);
        String data = null;
        while (matcher.find()) {
            data = matcher.group(1);
        }
        if (data == null) {
            return null;
        }
        try {
            return data.contains("-") || data.contains("_")
                    ? Base64.getUrlDecoder().decode(data)
                    : Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 오류 메시지는 외래어 대신 쉬운 한국어로 (NFR-03). 서비스가 준 원문은 괄호 안에 붙인다. */
    static String errorMessage(int status, String body) {
        Matcher matcher = ERROR_MESSAGE.matcher(body == null ? "" : body);
        String detail = matcher.find() ? " (" + matcher.group(1).replace("\\n", " ").replace("\\\"", "\"") + ")" : "";
        String reason = switch (status) {
            case 400 -> "그림 만드는 곳에서 요청을 받아들이지 않았습니다.";
            case 401, 403 -> "그림 만드는 곳의 이용 열쇠(API 키)가 맞지 않습니다.";
            case 404 -> "그림 만드는 모델 이름이 맞지 않습니다.";
            case 429 -> "지금은 이용이 많습니다. 잠시 뒤에 다시 시도해 주세요.";
            default -> "그림 만드는 곳에 문제가 생겼습니다. 잠시 뒤에 다시 시도해 주세요.";
        };
        return reason + detail;
    }

    private static String mimeType(byte[] image) {
        boolean png = image.length > 4 && (image[0] & 0xFF) == 0x89 && image[1] == 'P' && image[2] == 'N' && image[3] == 'G';
        return png ? "image/png" : "image/jpeg";
    }

    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
