package com.sadamda.model.tryon;

import java.util.List;

/** AI 합성 이미지 보관함. 업로드한 인물 사진은 합성 후 바로 삭제한다 (NFR-08). 담당: 정우진 */
public class TryOnService {

    /** UC-09 4단계 기본 배경 템플릿 id 목록 (FR-14). */
    public List<String> findBackgroundTemplates() {
        throw new UnsupportedOperationException("TODO UC-09");
    }

    /** UC-09 6단계 템플릿 id 로 배경 이미지를 읽는다. */
    public byte[] loadBackgroundTemplate(String templateId) {
        throw new UnsupportedOperationException("TODO UC-09");
    }

    /** UC-09 9단계 [보관함에 저장] (FR-16). */
    public TryOnImage save(Long memberId, Long productId, byte[] image) {
        throw new UnsupportedOperationException("TODO UC-09");
    }

    /** 보관함 목록 (FR-16). */
    public List<TryOnImage> findByMember(Long memberId) {
        throw new UnsupportedOperationException("TODO UC-09");
    }

    /** NFR-08 사용자가 언제든 삭제. 본인 것이 아니면 AppException. */
    public void delete(Long memberId, Long tryOnImageId) {
        throw new UnsupportedOperationException("TODO UC-09");
    }
}
