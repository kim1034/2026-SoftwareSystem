package com.sadamda.external;

import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpPrompt;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 옷 입혀보기(UC-09 확장)용 외부 AI 이미지 합성 연동 인터페이스.
 * 신체 스펙이 반영된 기본 캐릭터 이미지에 모자·상의·하의·주얼리·신발을 입힌 이미지를 만든다.
 * 실패 규칙은 AiImageClient 와 같다: 시간 초과·오류이면 DressUpException (UC-09 7b).
 */
public interface DressUpImageClient {

    /** 합성 결과 이미지(PNG 또는 JPEG 파일 내용). */
    byte[] dressUp(Request request) throws DressUpException;

    /** 화면에 보여줄 서비스 이름. */
    String name();

    /** AI 서비스에 연결되어 있는지. false 이면 dressUp 은 항상 실패하고, 화면은 연결 설정을 안내한다. */
    default boolean connected() {
        return true;
    }

    /** 응답을 기다리는 최대 시간(초). 화면의 안내 문구에 쓴다. */
    default int timeoutSeconds() {
        return AiImageClient.TIMEOUT_SECONDS;
    }

    /**
     * @param baseCharacter 신체 스펙이 반영된 기본 캐릭터 이미지 (CharacterFigure.shaped). 반드시 있다.
     * @param garmentImages 자리별 상품 사진(JPEG). 사진을 구하지 못한 자리는 빠져 있고, 그때는 상품 설명만으로 그린다.
     */
    record Request(CharacterStyle style, CharacterSpec spec, Outfit outfit,
                   Map<DressUpSlot, byte[]> garmentImages, byte[] baseCharacter) {

        /** 상품 사진을 함께 보내는 자리 (모자 → 신발 순). */
        public List<DressUpSlot> referenceSlots() {
            List<DressUpSlot> slots = new ArrayList<>();
            for (DressUpSlot slot : DressUpSlot.values()) {
                if (outfit.get(slot) != null && garmentImages.get(slot) != null) {
                    slots.add(slot);
                }
            }
            return slots;
        }

        /** 보내는 이미지: Image 1 = 기본 캐릭터, Image 2~ = referenceSlots 순서의 상품 사진. */
        public List<byte[]> images() {
            List<byte[]> images = new ArrayList<>();
            images.add(baseCharacter);
            for (DressUpSlot slot : referenceSlots()) {
                images.add(garmentImages.get(slot));
            }
            return images;
        }

        /** 위 이미지 순서에 맞춘 지시문. */
        public String prompt() {
            return DressUpPrompt.build(style, spec, outfit, referenceSlots());
        }
    }
}
