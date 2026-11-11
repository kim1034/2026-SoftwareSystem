package com.sadamda.model.tryon;

import java.time.LocalDateTime;

/** AI 합성 결과 보관함 항목 (UC-09, FR-16). */
public record TryOnImage(Long id, Long memberId, Long productId, String imagePath, LocalDateTime createdAt) {
}
