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
 * 옷 입혀보기(UC-09)용 외부 AI 이미지 합성 연동 인터페이스.
 * 신체 스펙이 반영된 기본 캐릭터, 또는 사용자가 올린 사진에 고른 상품을 입힌 이미지를 만든다.
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

    /** 연결되지 않았을 때 화면에 보여줄 안내 (왜 안 되는지, 무엇을 하면 되는지). 연결되어 있으면 null. */
    default String setupHint() {
        return null;
    }

    /** 응답을 기다리는 최대 시간(초). 화면의 안내 문구에 쓴다. */
    default int timeoutSeconds() {
        return AiImageClient.TIMEOUT_SECONDS;
    }

    /**
     * @param style         캐릭터 그림체. 내 사진이면 null.
     * @param spec          캐릭터 신체 스펙. 내 사진이면 null.
     * @param garmentImages 자리별 상품 사진(JPEG). 사진을 구하지 못한 자리는 빠져 있고, 그때는 상품 설명만으로 그린다.
     * @param baseCharacter Image 1: 신체 스펙이 반영된 기본 캐릭터, 또는 사용자가 올린 사진(JPEG). 반드시 있다.
     * @param background    내 사진일 때 고른 배경 사진(JPEG). 원래 배경 그대로면 null.
     * @param userPhoto     Image 1 이 사용자가 올린 실제 사진인지.
     */
    record Request(CharacterStyle style, CharacterSpec spec, Outfit outfit,
                   Map<DressUpSlot, byte[]> garmentImages, byte[] baseCharacter,
                   byte[] background, boolean userPhoto) {

        /** 캐릭터에 옷을 입히는 요청. */
        public Request(CharacterStyle style, CharacterSpec spec, Outfit outfit,
                       Map<DressUpSlot, byte[]> garmentImages, byte[] baseCharacter) {
            this(style, spec, outfit, garmentImages, baseCharacter, null, false);
        }

        /** 사용자가 올린 사진에 옷을 입히고(있으면) 배경을 바꾸는 요청. */
        public static Request photo(Outfit outfit, Map<DressUpSlot, byte[]> garmentImages,
                                    byte[] userPhoto, byte[] background) {
            return new Request(null, null, outfit, garmentImages, userPhoto, background, true);
        }

        /** 상품 사진을 함께 보내는 자리 (상의 → 가방 순). */
        public List<DressUpSlot> referenceSlots() {
            List<DressUpSlot> slots = new ArrayList<>();
            for (DressUpSlot slot : DressUpSlot.values()) {
                if (outfit.get(slot) != null && garmentImages.get(slot) != null) {
                    slots.add(slot);
                }
            }
            return slots;
        }

        /** 보내는 이미지: Image 1 = 캐릭터 또는 내 사진, (내 사진에 배경을 골랐으면) 배경, 그다음 referenceSlots 순서의 상품 사진. */
        public List<byte[]> images() {
            List<byte[]> images = new ArrayList<>();
            images.add(baseCharacter);
            if (userPhoto && background != null) {
                images.add(background);
            }
            for (DressUpSlot slot : referenceSlots()) {
                images.add(garmentImages.get(slot));
            }
            return images;
        }

        /** 위 이미지 순서에 맞춘 지시문. */
        public String prompt() {
            return userPhoto ? DressUpPrompt.buildPhoto(outfit, referenceSlots(), background != null)
                    : DressUpPrompt.build(style, spec, outfit, referenceSlots());
        }
    }
}
