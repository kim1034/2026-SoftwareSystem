package com.sadamda.model.tryon;

import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.product.ProductTags;

import java.util.List;

/** 옷 입혀보기 캐릭터의 신체 스펙: 키, 몸무게, 체형, 연령대. */
public record CharacterSpec(int heightCm, int weightKg, String bodyType, String ageGroup) {
    public static final int MIN_HEIGHT = 140;
    public static final int MAX_HEIGHT = 185;
    public static final int MIN_WEIGHT = 35;
    public static final int MAX_WEIGHT = 120;
    public static final List<String> BODY_TYPES = ProductTags.BODY_TYPES;
    public static final List<String> AGE_GROUPS = List.of("20대", "30대", "40대", "50대", "60대");

    public CharacterSpec {
        heightCm = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, heightCm));
        weightKg = Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, weightKg));
        if (bodyType == null || !BODY_TYPES.contains(bodyType)) {
            bodyType = "보통형";
        }
        if (ageGroup == null || !AGE_GROUPS.contains(ageGroup)) {
            ageGroup = "40대";
        }
    }

    public static CharacterSpec defaults() {
        return new CharacterSpec(160, 56, "보통형", "40대");
    }

    /** 회원이 등록한 체형 정보로 시작한다. 몸무게는 회원 정보에 없어서 키에 맞는 보통 몸무게로 둔다. */
    public static CharacterSpec from(BodyProfile profile) {
        if (profile == null) {
            return defaults();
        }
        int height = profile.heightCm() == null ? 160 : profile.heightCm();
        return new CharacterSpec(height, standardWeight(height), profile.bodyType(), profile.ageGroup());
    }

    /** 키에 맞는 보통 몸무게 (체질량지수 22 기준). */
    public static int standardWeight(int heightCm) {
        double meters = heightCm / 100.0;
        return (int) Math.round(22.0 * meters * meters);
    }

    /** 체질량지수 = 몸무게(kg) ÷ 키(m)². */
    public double bmi() {
        double meters = heightCm / 100.0;
        return weightKg / (meters * meters);
    }

    /** 화면에 보여주는 요약. 예: 160cm · 56kg · 보통형 · 40대 */
    public String summary() {
        return heightCm + "cm · " + weightKg + "kg · " + bodyType + " · " + ageGroup;
    }
}
