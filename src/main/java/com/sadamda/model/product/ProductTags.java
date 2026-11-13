package com.sadamda.model.product;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 추천 연령대·체형 태그에 쓰는 값 (FR-07, FR-12, FR-25).
 * 회원 체형 정보(BodyProfile)의 ageGroup·bodyType 과 같은 문자열을 써야 맞춤 추천이 동작한다.
 */
public final class ProductTags {
    public static final List<String> AGE_GROUPS = List.of("40대", "50대", "60대");
    public static final List<String> BODY_TYPES = List.of("마른형", "보통형", "통통형", "상체발달형", "하체발달형");

    private ProductTags() {
    }

    /**
     * 상품명·코디 포인트 문구에서 어울리는 체형 태그를 추정한다.
     * 데모 데이터에는 체형 태그가 없어서 넣은 임시 규칙이다. 관리자가 상품 관리(UC-16)에서 직접 고치면 그 값이 우선한다.
     */
    public static List<String> guessBodyTypes(Category category, String text) {
        if (category != Category.TOP && category != Category.BOTTOM
                && category != Category.DRESS && category != Category.OUTER) {
            return BODY_TYPES; // 모자·신발·양말·주얼리는 체형을 가리지 않는다
        }
        String source = text == null ? "" : text;
        Set<String> tags = new LinkedHashSet<>();
        tags.add("보통형");
        if (containsAny(source, "와이드", "A라인", "플레어", "밴딩", "스트레이트", "일자", "H라인")) {
            tags.add("하체발달형");
            tags.add("통통형");
        }
        if (containsAny(source, "박시", "루즈", "오버", "여유", "드레이프", "흐르", "체형", "커버")) {
            tags.add("통통형");
            tags.add("상체발달형");
        }
        if (containsAny(source, "슬림", "크롭", "타이트", "피트", "테일러")) {
            tags.add("마른형");
        }
        return new ArrayList<>(tags);
    }

    private static boolean containsAny(String source, String... words) {
        for (String word : words) {
            if (source.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
