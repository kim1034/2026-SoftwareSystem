package com.sadamda.controller;

/**
 * UC-09 [이미지 만들기] 입력.
 * 배경은 backgroundTemplateId(기본 템플릿)와 backgroundImage(직접 업로드) 중 하나만 채운다 (FR-14).
 */
public record TryOnRequest(Long productId, byte[] personImage, String personFileName,
                           String backgroundTemplateId, byte[] backgroundImage, boolean photoUseAgreed) {
}
