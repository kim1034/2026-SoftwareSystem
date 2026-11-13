package com.sadamda.controller;

import com.sadamda.external.AiImageClient;
import com.sadamda.model.tryon.TryOnImage;
import com.sadamda.model.tryon.TryOnService;

import java.util.List;

/** UC-09 AI 착용 이미지 생성. 모든 메서드 로그인 필요. 담당: 김대원 */
public class TryOnController {
    /** FR-13 업로드 가능한 형식과 최대 용량 (3a). */
    public static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final Session session;
    private final AiImageClient aiImageClient;
    private final TryOnService tryOnService;

    public TryOnController(Session session, AiImageClient aiImageClient, TryOnService tryOnService) {
        this.session = session;
        this.aiImageClient = aiImageClient;
        this.tryOnService = tryOnService;
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
        throw new UnsupportedOperationException("TODO UC-09 (계획 4단계)");
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
