package com.sadamda.external;

/** 외부 AI 이미지 합성 서비스 연동 인터페이스 (Secondary Actor). 담당: 김대원 */
public interface AiImageClient {

    /** NFR-05: 30초 안에 응답이 없으면 실패로 본다 (UC-09 7b). */
    int TIMEOUT_SECONDS = 30;

    /**
     * UC-09 6~7단계 인물·배경 사진에 의류를 입힌 이미지를 만든다 (FR-15).
     * 인물을 인식하지 못하면(7a)·시간 초과·오류(7b)이면 AiImageException.
     */
    byte[] synthesize(byte[] personImage, byte[] backgroundImage, String clothingImageUrl) throws AiImageException;

    class AiImageException extends Exception {
        public AiImageException(String message) {
            super(message);
        }
    }
}
