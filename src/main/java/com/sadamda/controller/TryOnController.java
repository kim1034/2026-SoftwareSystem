package com.sadamda.controller;

import com.sadamda.common.AppException;
import com.sadamda.common.RetryableException;
import com.sadamda.external.AiImageClient;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.tryon.TryOnImage;
import com.sadamda.model.tryon.TryOnService;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** UC-09 AI 착용 이미지 생성. 모든 메서드 로그인 필요. 담당: 김대원 */
public class TryOnController {
    /** FR-13 업로드 가능한 형식과 최대 용량 (3a). */
    public static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    static final String MSG_NOT_AGREED = "사진 이용에 동의해야 AI 입어보기를 쓸 수 있습니다.";
    static final String MSG_NO_PERSON = "인물 사진을 올려 주세요.";
    static final String MSG_BAD_FORMAT = "JPG 또는 PNG 사진만 올릴 수 있습니다.";
    static final String MSG_TOO_LARGE = "사진 용량은 10MB 이하여야 합니다.";
    static final String MSG_NO_BACKGROUND = "배경을 골라 주세요.";
    static final String MSG_PERSON_NOT_FOUND = "사진에서 사람을 찾지 못했습니다. 전신이 나온 다른 사진을 올려 주세요.";
    static final String MSG_RETRY = "이미지를 만들지 못했습니다. 다시 시도해 주세요.";

    private static final System.Logger LOG = System.getLogger(TryOnController.class.getName());

    /** AI 호출 전용 스레드. 멈춘 호출이 앱 종료를 막지 않도록 데몬 스레드로 만든다 (NFR-09). */
    private static final ExecutorService AI_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "ai-image");
        thread.setDaemon(true);
        return thread;
    });

    private final Session session;
    private final AiImageClient aiImageClient;
    private final TryOnService tryOnService;
    private final ProductService productService;
    private final Duration timeout;

    /** timeout: AI 응답을 기다리는 최대 시간. 앱에서는 AiImageClient.TIMEOUT_SECONDS (NFR-05). */
    public TryOnController(Session session, AiImageClient aiImageClient, TryOnService tryOnService,
                           ProductService productService, Duration timeout) {
        this.session = session;
        this.aiImageClient = aiImageClient;
        this.tryOnService = tryOnService;
        this.productService = productService;
        this.timeout = timeout;
    }

    /** UC-09 4단계 기본 배경 템플릿 id 목록 (FR-14). */
    public List<String> backgroundTemplates() {
        session.requireLogin();
        return tryOnService.findBackgroundTemplates();
    }

    /**
     * UC-09 5~8단계 합성 이미지를 만든다 (FR-13~15).
     * 최대 30초 걸리므로(NFR-05) View 는 반드시 SwingWorker 등 별도 스레드에서 호출하고 진행 상태를 표시한다.
     * AppException 메시지: 동의 안 함(2a), 형식·용량 오류(3a), 인물 인식 실패(7a), 시간 초과·오류(7b).
     * 7b 이면 View 는 [다시 시도] 버튼을 보여준다.
     */
    public byte[] generate(TryOnRequest request) {
        session.requireLogin();
        validate(request);
        String clothingImageUrl = productService.findById(request.productId()).getMainImageUrl();
        byte[] background = request.backgroundImage() != null
                ? request.backgroundImage()
                : tryOnService.loadBackgroundTemplate(request.backgroundTemplateId());

        // 인물 사진은 이 호출 안에서만 쓰고 어디에도 남기지 않는다 (NFR-08).
        Future<byte[]> future = AI_EXECUTOR.submit(
                () -> aiImageClient.synthesize(request.personImage(), background, clothingImageUrl));
        byte[] result;
        try {
            result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            LOG.log(System.Logger.Level.WARNING, "AI 합성 시간 초과: " + timeout);
            throw new RetryableException(MSG_RETRY);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RetryableException(MSG_RETRY);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AiImageClient.AiImageException ai
                    && ai.reason() == AiImageClient.AiImageException.Reason.PERSON_NOT_FOUND) {
                throw new AppException(MSG_PERSON_NOT_FOUND);
            }
            LOG.log(System.Logger.Level.WARNING, "AI 합성 실패", cause);
            throw new RetryableException(MSG_RETRY);
        }
        if (result == null) {
            throw new RetryableException(MSG_RETRY);
        }
        return result;
    }

    private static void validate(TryOnRequest request) {
        if (!request.photoUseAgreed()) {
            throw new AppException(MSG_NOT_AGREED);
        }
        if (request.personImage() == null || request.personImage().length == 0) {
            throw new AppException(MSG_NO_PERSON);
        }
        if (!hasAllowedExtension(request.personFileName())) {
            throw new AppException(MSG_BAD_FORMAT);
        }
        if (request.personImage().length > MAX_IMAGE_BYTES
                || (request.backgroundImage() != null && request.backgroundImage().length > MAX_IMAGE_BYTES)) {
            throw new AppException(MSG_TOO_LARGE);
        }
        if (request.backgroundImage() == null && request.backgroundTemplateId() == null) {
            throw new AppException(MSG_NO_BACKGROUND);
        }
    }

    private static boolean hasAllowedExtension(String fileName) {
        if (fileName == null) {
            return false;
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && ALLOWED_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** UC-09 9단계 [보관함에 저장] (FR-16). */
    public TryOnImage saveToLocker(Long productId, byte[] image) {
        return tryOnService.save(session.requireLogin().getId(), productId, image);
    }

    /** 보관함 목록 (FR-16). */
    public List<TryOnImage> locker() {
        return tryOnService.findByMember(session.requireLogin().getId());
    }

    /** 보관함에서 삭제 (NFR-08). */
    public void deleteFromLocker(Long tryOnImageId) {
        tryOnService.delete(session.requireLogin().getId(), tryOnImageId);
    }
}
