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
 * - 참고 이미지(캐릭터 기준·상품 사진)가 있으면  POST {baseUrl}/images/edits       (multipart, image[] 여러 장)
 * - 참고 이미지가 없으면                         POST {baseUrl}/images/generations (JSON)
 * - 인증: Authorization: Bearer {키}
 * - 응답: data[0].b64_json, 없으면 data[0].url 을 내려받는다 (중계 서비스마다 다르다)
 * 외부 라이브러리 없이 java.net.http 만 쓴다.
 */
public class OpenAiDressUpClient implements DressUpImageClient {
    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String DEFAULT_MODEL = "gpt-image-2";
    /** 세로 전신 그림. */
    private static final String SIZE = "1024x1536";

    private static final Pattern B64 = Pattern.compile("\"b64_json\"\\s*:\\s*\"([A-Za-z0-9+/=\\\\]+)\"");
    private static final Pattern URL = Pattern.compile("\"url\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)+)\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String quality;
    private final int timeoutSeconds;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** @param quality low·medium·high·auto 중 하나. null 이면 보내지 않는다 (서비스 기본값). */
    public OpenAiDressUpClient(String apiKey, String baseUrl, String model, String quality, int timeoutSeconds) {
        this.apiKey = apiKey;
        String base = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
        this.quality = quality == null || quality.isBlank() ? null : quality.trim();
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String name() {
        String host = URI.create(baseUrl).getHost();
        return "OpenAI 형식 (" + model + (host == null ? "" : " · " + host) + ")";
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
        HttpRequest httpRequest = images.isEmpty() ? generationRequest(prompt) : editRequest(prompt, images);
        return parse(send(httpRequest));
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
