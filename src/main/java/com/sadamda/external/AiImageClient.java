package com.sadamda.external;

/** 외부 AI 이미지 합성 서비스 연동 인터페이스 (Secondary Actor). 담당: 김대원 */
public interface AiImageClient {

    /** NFR-05: 30초 안에 응답이 없으면 실패로 본다 (UC-09 7b). */
    int TIMEOUT_SECONDS = 30;

    /**
     * UC-09 6~7단계 인물·배경 사진에 의류를 입힌 이미지를 만든다 (FR-15).
     * 인물을 인식하지 못하면(7a) Reason.PERSON_NOT_FOUND, 그 밖의 오류(7b)면 Reason.FAILED 인 AiImageException.
     * 시간 제한은 호출하는 쪽(TryOnController)이 건다.
     */
    byte[] synthesize(byte[] personImage, byte[] backgroundImage, String clothingImageUrl) throws AiImageException;

    class AiImageException extends Exception {
        /** PERSON_NOT_FOUND: 사진에서 사람을 찾지 못함 (7a). FAILED: 그 밖의 오류 (7b). */
        public enum Reason { PERSON_NOT_FOUND, FAILED }

        private final Reason reason;

        public AiImageException(Reason reason, String message) {
            super(message);
            this.reason = reason;
        }

        public Reason reason() {
            return reason;
        }
    }
}
