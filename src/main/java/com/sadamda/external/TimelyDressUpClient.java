package com.sadamda.external;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
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
 * 타임리 AI 연동 키("tgpt_")로 옷 입혀보기 그림을 만든다.
 *
 * 타임리 AI 는 2026-11-18 에 OpenAI 호환 주소(.../bridge/openai)를 닫았고, 새 Timely API(.../direct/...)는 글만 돌려준다.
 * 대신 타임리 SDK API 의 내장 도구 edit_image(참조 그림을 보고 새로 그리기, gpt-image 계열)로 그림을 만들 수 있다.
 * 순서
 * 1. GET  {api}/sdk-auth/authenticate (X-Timely-API: 연동 키) → access_token (JWT). 받아 둔 토큰은 다시 쓴다.
 * 2. POST {api}/built-in-tool-node/edit_image/invoke
 *         {"args":{"prompt":…, "aspectRatio":"portrait", "referenceImageUrls":[기본 캐릭터, 상품 사진… (data URL)]}}
 *    → 결과는 그림 파일 이름("file-server:p_OPENAI_IMAGE_….png")만 온다.
 * 3. GET  {file}/files/by/name/{파일 이름}/get-url (Authorization: Bearer JWT) → downloadUrl (1시간 동안 쓸 수 있는 주소)
 * 4. downloadUrl 에서 그림을 내려받는다.
 *
 * 3번은 타임리 웹 화면이 그림을 보여줄 때 쓰는 주소다. 공개 문서에는 없어서 타임리가 바꾸면 이 부분을 고쳐야 한다.
 * 외부 라이브러리 없이 java.net.http 만 쓴다.
 */
public class TimelyDressUpClient implements DressUpImageClient {
    public static final String API_BASE = "https://hello.timelyai.io/api/v2/chat";
    public static final String FILE_BASE = "https://timelyai.io/proxy/api/file/timely-file-khdgj";
    private static final String TOOL = "edit_image";

    private static final Pattern TOKEN = Pattern.compile("\"access_?[tT]oken\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern FILE_REF = Pattern.compile("file-server:([A-Za-z0-9_.\\-]+)");
    private static final Pattern OUTPUT_URL = Pattern.compile("\"output\"\\s*:\\s*\"(https?://[^\"]+)\"");
    private static final Pattern DOWNLOAD_URL = Pattern.compile("\"downloadUrl\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final String apiKey;
    private final String apiBase;
    private final String fileBase;
    private final int timeoutSeconds;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private volatile String accessToken;

    public TimelyDressUpClient(String apiKey, int timeoutSeconds) {
        this(apiKey, API_BASE, FILE_BASE, timeoutSeconds);
    }

    /** 주소를 바꿔 끼운다 (점검용 가짜 서버, 타임리가 주소를 옮겼을 때). */
    public TimelyDressUpClient(String apiKey, String apiBase, String fileBase, int timeoutSeconds) {
        this.apiKey = DressUpSettings.normalizeKey(apiKey);
        this.apiBase = trimSlash(apiBase);
        this.fileBase = trimSlash(fileBase);
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String name() {
        return "타임리 AI (" + TOOL + ")";
    }

    @Override
    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public byte[] dressUp(Request request) throws DressUpException {
        String body = invoke(editBody(request.prompt(), request.images()));
        Matcher ref = FILE_REF.matcher(body);
        if (ref.find()) {
            return download(downloadUrl(ref.group(1)));
        }
        Matcher url = OUTPUT_URL.matcher(body);
        if (url.find()) {
            return download(unescape(url.group(1)));
        }
        Matcher message = ERROR_MESSAGE.matcher(body);
        throw new DressUpException("그림을 받지 못했습니다. 옷을 바꾸거나 다시 시도해 주세요."
                + (message.find() ? " (" + unescape(message.group(1)) + ")" : ""));
    }

    String editBody(String prompt, List<byte[]> images) {
        StringBuilder json = new StringBuilder("{\"args\":{");
        json.append("\"prompt\":\"").append(GeminiDressUpClient.escape(prompt)).append("\",");
        json.append("\"aspectRatio\":\"portrait\",");
        json.append("\"referenceImageUrls\":[");
        for (int i = 0; i < images.size(); i++) {
            byte[] image = images.get(i);
            json.append(i == 0 ? "" : ",").append("\"data:").append(isPng(image) ? "image/png" : "image/jpeg")
                    .append(";base64,").append(Base64.getEncoder().encodeToString(image)).append('"');
        }
        json.append("]}}");
        return json.toString();
    }

    /** 도구를 부른다. 받아 둔 토큰이 만료됐으면(401) 한 번 새로 받아 다시 부른다. */
    private String invoke(String json) throws DressUpException {
        URI uri = URI.create(apiBase + "/built-in-tool-node/" + TOOL + "/invoke");
        for (int attempt = 0; ; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + token(attempt > 0))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = send(request);
            if (response.statusCode() == 401 && attempt == 0) {
                continue;
            }
            check(response);
            return response.body();
        }
    }

    private synchronized String token(boolean renew) throws DressUpException {
        if (accessToken != null && !renew) {
            return accessToken;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + "/sdk-auth/authenticate"))
                .timeout(Duration.ofSeconds(Math.min(timeoutSeconds, 20)))
                .header("X-Timely-API", apiKey)
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        Matcher token = TOKEN.matcher(response.body());
        if (response.statusCode() / 100 != 2 || !token.find()) {
            throw new DressUpException("타임리 AI 연동 키가 맞지 않습니다. 타임리 AI 에서 연동 키를 다시 복사해 넣어 주세요."
                    + " [재발급]을 누르면 전에 쓰던 키는 쓸 수 없습니다.");
        }
        accessToken = token.group(1);
        return accessToken;
    }

    private String downloadUrl(String fileName) throws DressUpException {
        URI uri = URI.create(fileBase + "/files/by/name/" + URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                + "/get-url");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.min(timeoutSeconds, 20)))
                .header("Authorization", "Bearer " + token(false))
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        Matcher url = DOWNLOAD_URL.matcher(response.body());
        if (response.statusCode() / 100 != 2 || !url.find()) {
            throw new DressUpException("그림은 만들었지만 내려받지 못했습니다. 다시 시도해 주세요.");
        }
        return unescape(url.group(1));
    }

    private byte[] download(String url) throws DressUpException {
        try {
            HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.min(timeoutSeconds, 30))).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 == 2 && RemoteImages.decode(response.body()) != null) {
                return response.body();
            }
        } catch (IOException | IllegalArgumentException e) {
            // 아래에서 안내한다
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DressUpException("그림 만들기를 멈췄습니다.");
        }
        throw new DressUpException("그림은 만들었지만 내려받지 못했습니다. 다시 시도해 주세요.");
    }

    private HttpResponse<String> send(HttpRequest request) throws DressUpException {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new DressUpException(timeoutSeconds + "초 안에 그림이 오지 않았습니다. 다시 시도해 주세요.");
        } catch (IOException e) {
            throw new DressUpException("타임리 AI 에 연결하지 못했습니다. 인터넷 연결을 확인해 주세요.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DressUpException("그림 만들기를 멈췄습니다.");
        }
    }

    private static void check(HttpResponse<String> response) throws DressUpException {
        int status = response.statusCode();
        if (status / 100 == 2) {
            return;
        }
        String body = response.body();
        if (status == 402) {
            throw new DressUpException("타임리 AI 크레딧이 모자랍니다. 타임리 AI 에서 남은 크레딧을 확인해 주세요.");
        }
        if (status == 403) {
            Matcher message = ERROR_MESSAGE.matcher(body == null ? "" : body);
            throw new DressUpException("타임리 AI 에서 그림 만들기를 허용하지 않았습니다. 이 공간에서 이미지 도구를 쓸 수 있는지,"
                    + " 접속 IP 제한이 있는지 확인해 주세요." + (message.find() ? " (" + unescape(message.group(1)) + ")" : ""));
        }
        throw new DressUpException(GeminiDressUpClient.errorMessage(status, body));
    }

    private static String unescape(String text) {
        return text.replace("\\u0026", "&").replace("\\/", "/").replace("\\\"", "\"").replace("\\n", " ");
    }

    private static String trimSlash(String url) {
        String text = url.trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static boolean isPng(byte[] image) {
        return image.length > 4 && (image[0] & 0xFF) == 0x89 && image[1] == 'P' && image[2] == 'N' && image[3] == 'G';
    }
}
