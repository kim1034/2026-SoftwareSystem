package com.sadamda.controller;

import com.sadamda.external.AiImageClient;
import com.sadamda.model.tryon.TryOnImage;
import com.sadamda.model.tryon.TryOnService;

/** UC-09 AI 착용 이미지 생성. 담당: 김대원 */
public class TryOnController {
    private final Session session;
    private final AiImageClient aiImageClient;
    private final TryOnService tryOnService;

    public TryOnController(Session session, AiImageClient aiImageClient, TryOnService tryOnService) {
        this.session = session;
        this.aiImageClient = aiImageClient;
        this.tryOnService = tryOnService;
    }

    /**
     * UC-09 3~8단계.
     * TODO: 파일 형식·용량 검증(3a), 타임아웃·재시도·장애 격리(7b, NFR-05·09), 업로드 사진 삭제(NFR-08)
     */
    public byte[] generate(byte[] personImage, byte[] backgroundImage, String clothingImageUrl)
            throws AiImageClient.AiImageException {
        return aiImageClient.synthesize(personImage, backgroundImage, clothingImageUrl);
    }

    /** UC-09 9단계 [보관함에 저장] */
    public TryOnImage saveToLocker(Long productId, byte[] image) {
        throw new UnsupportedOperationException("TODO UC-09: session 회원 id 로 tryOnService.save 호출");
    }
}
