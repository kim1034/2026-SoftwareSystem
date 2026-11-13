package com.sadamda.external;

import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;

import java.util.Map;

/**
 * 옷 입혀보기(UC-09 확장)용 외부 AI 이미지 합성 연동 인터페이스.
 * 인물 사진 대신 그림체·신체 스펙으로 만든 여성 캐릭터에 모자·상의·하의·주얼리·신발을 입힌 이미지를 만든다.
 * 실패 규칙은 AiImageClient 와 같다: 시간 초과·오류이면 AiImageException (UC-09 7b).
 */
public interface DressUpImageClient {

    /** 합성 결과 이미지(PNG 또는 JPEG 파일 내용). */
    byte[] dressUp(Request request) throws AiImageClient.AiImageException;

    /** 화면에 보여줄 서비스 이름. */
    String name();

    /** 응답을 기다리는 최대 시간(초). 화면의 안내 문구에 쓴다. */
    default int timeoutSeconds() {
        return AiImageClient.TIMEOUT_SECONDS;
    }

    /**
     * @param garmentImages 자리별 상품 사진(JPEG). 사진을 구하지 못한 자리는 빠져 있고, 그때는 상품 설명만으로 그린다.
     * @param baseCharacter 같은 그림체·스펙으로 전에 만든 캐릭터 이미지. 얼굴과 자세를 유지하는 기준이며 없으면 null.
     */
    record Request(CharacterStyle style, CharacterSpec spec, Outfit outfit,
                   Map<DressUpSlot, byte[]> garmentImages, byte[] baseCharacter) {
    }
}
