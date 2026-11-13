package com.sadamda.model.tryon;

import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.product.ProductTags;

import java.util.List;

/** 옷 입혀보기 캐릭터의 신체 스펙: 키, 체형, 연령대. */
public record CharacterSpec(int heightCm, String bodyType, String ageGroup) {
    public static final int MIN_HEIGHT = 140;
    public static final int MAX_HEIGHT = 185;
    public static final List<String> BODY_TYPES = ProductTags.BODY_TYPES;
    public static final List<String> AGE_GROUPS = List.of("20대", "30대", "40대", "50대", "60대");

    public CharacterSpec {
        heightCm = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, heightCm));
        if (bodyType == null || !BODY_TYPES.contains(bodyType)) {
            bodyType = "보통형";
        }
        if (ageGroup == null || !AGE_GROUPS.contains(ageGroup)) {
            ageGroup = "40대";
        }
    }

    public static CharacterSpec defaults() {
        return new CharacterSpec(160, "보통형", "40대");
    }

    /** 회원이 등록한 체형 정보로 시작한다. 빠진 값은 기본값. */
    public static CharacterSpec from(BodyProfile profile) {
        if (profile == null) {
            return defaults();
        }
        return new CharacterSpec(profile.heightCm() == null ? 160 : profile.heightCm(),
                profile.bodyType(), profile.ageGroup());
    }

    /** 화면에 보여주는 요약. 예: 160cm · 보통형 · 40대 */
    public String summary() {
        return heightCm + "cm · " + bodyType + " · " + ageGroup;
    }
}
