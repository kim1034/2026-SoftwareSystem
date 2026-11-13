package com.sadamda.external;

import com.sadamda.external.AiImageClient.AiImageException;
import com.sadamda.model.tryon.DressUpPrompt;
import com.sadamda.model.tryon.DressUpSlot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAI 이미지 API 형식(공식 또는 OpenAI 호환 중계 서비스)으로 옷 입혀보기 이미지를 만든다.
 * 보내는 방식(api) 두 가지
 * - "images": 참고 이미지가 있으면 POST {baseUrl}/images/edits (multipart, image[] 여러 장),
 *             없으면 POST {baseUrl}/images/generations (JSON). OpenAI 공식 API 의 기본값.
 * - "chat"  : POST {baseUrl}/chat/completions, modalities=[image,text], 참고 이미지는 image_url(data URL).
 *             여러 회사 모델을 묶은 중계 서비스(모델 이름이 "google/…" 처럼 생긴 곳)에서 쓰는 방식.
 *             응답의 message.images[].image_url.url (data URL) 을 결과로 쓴다.
 * - 인증: Authorization: Bearer {키}
 * - 응답: data[0].b64_json, 없으면 data[0].url 을 내려받는다 (중계 서비스마다 다르다)
 * 외부 라이브러리 없이 java.net.http 만 쓴다.
 */
public class OpenAiDressUpClient implements DressUpImageClient {
    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String DEFAULT_MODEL = "gpt-image-2";
    /** chat 방식의 기본 모델 (중계 서비스). */
    public static final String DEFAULT_CHAT_MODEL = "google/gemini-3.1-flash-image";
    public static final String API_IMAGES = "images";
    public static final String API_CHAT = "chat";
    /** 세로 전신 그림. */
    private static final String SIZE = "1024x1536";

    private static final Pattern B64 = Pattern.compile("\"b64_json\"\\s*:\\s*\"([A-Za-z0-9+/=\\\\]+)\"");
    private static final Pattern URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String quality;
    private final String api;
    private final int timeoutSeconds;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** images 방식, 기본 모델. */
    public OpenAiDressUpClient(String apiKey, String baseUrl, String model, String quality, int timeoutSeconds) {
        this(apiKey, baseUrl, API_IMAGES, model, quality, timeoutSeconds);
    }

    /**
     * @param api     "images" 또는 "chat". null 이면 OpenAI 공식 주소는 images, 다른 주소(중계 서비스)는 chat.
     * @param model   null 이면 방식별 기본 모델
     * @param quality low·medium·high·auto 중 하나 (images 방식만). null 이면 보내지 않는다.
     */
    public OpenAiDressUpClient(String apiKey, String baseUrl, String api, String model, String quality,
                               int timeoutSeconds) {
        this.apiKey = apiKey;
        String base = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        if (api == null || api.isBlank()) {
            api = this.baseUrl.equals(DEFAULT_BASE_URL) ? API_IMAGES : API_CHAT;
        }
        this.api = API_CHAT.equalsIgnoreCase(api.trim()) ? API_CHAT : API_IMAGES;
        String defaultModel = this.api.equals(API_CHAT) ? DEFAULT_CHAT_MODEL : DEFAULT_MODEL;
        this.model = model == null || model.isBlank() ? defaultModel : model.trim();
        this.quality = quality == null || quality.isBlank() ? null : quality.trim();
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String name() {
        return (api.equals(API_CHAT) ? "중계 서비스 (" : "OpenAI (") + model + ")";
    }

    @Override
    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public byte[] dressUp(Request request) throws AiImageException {
        List<String> labels = new ArrayList<>();
        List<byte[]> images = new ArrayList<>();
        if (request.baseCharacter() != null) {
            images.add(request.baseCharacter());
        }
        for (DressUpSlot slot : DressUpSlot.values()) {
            byte[] garment = request.garmentImages().get(slot);
            if (garment != null && request.outfit().get(slot) != null) {
                labels.add(slot.english());
                images.add(garment);
            }
        }
        String prompt = DressUpPrompt.build(request.style(), request.spec(), request.outfit(),
                request.baseCharacter() != null, labels);
        HttpRequest httpRequest = api.equals(API_CHAT) ? chatRequest(prompt, images)
                : images.isEmpty() ? generationRequest(prompt) : editRequest(prompt, images);
        return parse(send(httpRequest));
    }

    HttpRequest chatRequest(String prompt, List<byte[]> images) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"model\":\"").append(GeminiDressUpClient.escape(model)).append("\",");
        json.append("\"modalities\":[\"image\",\"text\"],");
        json.append("\"messages\":[{\"role\":\"user\",\"content\":[");
        json.append("{\"type\":\"text\",\"text\":\"").append(GeminiDressUpClient.escape(prompt)).append("\"}");
        for (byte[] image : images) {
            json.append(",{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:")
                    .append(isPng(image) ? "image/png" : "image/jpeg").append(";base64,")
                    .append(Base64.getEncoder().encodeToString(image)).append("\"}}");
        }
        json.append("]}]}");
        return base("/chat/completions")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                .build();
    }

    HttpRequest generationRequest(String prompt) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"model\":\"").append(GeminiDressUpClient.escape(model)).append("\",");
        json.append("\"prompt\":\"").append(GeminiDressUpClient.escape(prompt)).append("\",");
        json.append("\"size\":\"").append(SIZE).append("\",\"n\":1");
        if (quality != null) {
            json.append(",\"quality\":\"").append(GeminiDressUpClient.escape(quality)).append("\"");
        }
        json.append("}");
        return base("/images/generations")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                .build();
    }

    HttpRequest editRequest(String prompt, List<byte[]> images) {
        String boundary = "----sadamda" + UUID.randomUUID().toString().replace("-", "");
        Multipart body = new Multipart(boundary);
        body.field("model", model);
        body.field("prompt", prompt);
        body.field("size", SIZE);
        body.field("n", "1");
        if (quality != null) {
            body.field("quality", quality);
        }
        for (int i = 0; i < images.size(); i++) {
            byte[] image = images.get(i);
            boolean png = isPng(image);
            body.file("image[]", "image" + (i + 1) + (png ? ".png" : ".jpg"), png ? "image/png" : "image/jpeg", image);
        }
        return base("/images/edits")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.finish()))
                .build();
    }

    private HttpRequest.Builder base(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + apiKey);
    }

    private String send(HttpRequest request) throws AiImageException {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new AiImageException(timeoutSeconds + "초 안에 그림이 오지 않았습니다. 다시 시도해 주세요.");
        } catch (IOException e) {
            throw new AiImageException("그림 만드는 곳에 연결하지 못했습니다. 인터넷 연결과 서비스 주소를 확인해 주세요.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiImageException("그림 만들기를 멈췄습니다.");
        } catch (IllegalArgumentException e) {
            throw new AiImageException("서비스 주소가 올바르지 않습니다.");
        }
        if (response.statusCode() != 200) {
            throw new AiImageException(GeminiDressUpClient.errorMessage(response.statusCode(), response.body()));
        }
        return response.body();
    }

    /** b64_json 이 있으면 풀고, 없으면 url 의 그림을 내려받는다. */
    byte[] parse(String body) throws AiImageException {
        Matcher b64 = B64.matcher(body);
        if (b64.find()) {
            try {
                return Base64.getDecoder().decode(b64.group(1).replace("\\/", "/").replace("\\n", ""));
            } catch (IllegalArgumentException e) {
                throw new AiImageException("받은 그림을 열 수 없습니다. 다시 시도해 주세요.");
            }
        }
        Matcher url = URL.matcher(body);
        while (url.find()) {
            String address = url.group(1).replace("\\/", "/").replace("\\u0026", "&");
            if (address.startsWith("http")) {
                byte[] image = RemoteImages.fetch(address);
                if (image != null) {
                    return image;
                }
            } else if (address.startsWith("data:image")) {
                return Base64.getDecoder().decode(address.substring(address.indexOf(',') + 1));
            }
        }
        Matcher message = ERROR_MESSAGE.matcher(body);
        throw new AiImageException("그림을 받지 못했습니다. 옷을 바꾸거나 다시 시도해 주세요."
                + (message.find() ? " (" + message.group(1) + ")" : ""));
    }

    private static boolean isPng(byte[] image) {
        return image.length > 4 && (image[0] & 0xFF) == 0x89 && image[1] == 'P' && image[2] == 'N' && image[3] == 'G';
    }

    /** multipart/form-data 본문 만들기. */
    private static final class Multipart {
        private final String boundary;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Multipart(String boundary) {
            this.boundary = boundary;
        }

        void field(String name, String value) {
            write("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
            write(value);
            write("\r\n");
        }

        void file(String name, String fileName, String contentType, byte[] data) {
            write("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\""
                    + fileName + "\"\r\nContent-Type: " + contentType + "\r\n\r\n");
            out.writeBytes(data);
            write("\r\n");
        }

        byte[] finish() {
            write("--" + boundary + "--\r\n");
            return out.toByteArray();
        }

        private void write(String text) {
            out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
