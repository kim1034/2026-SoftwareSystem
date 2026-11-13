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

    /**
     * 상품명·코디 포인트 문구에서 어울리는 연령대 태그를 추정한다 (FR-07, FR-12).
     * 데모 데이터에는 연령대 구분이 없어 모든 상품이 40·50·60대로 같았다. 그러면 연령대 필터·맞춤 추천이 결과를 바꾸지 못해 넣은 임시 규칙이다.
     * - 40대: 캐주얼·트렌디 (스니커즈, 데일리, 경쾌 …)
     * - 50대: 단정·우아 (클래식, 캐시미어, 모임 …)
     * - 60대: 편안함 (쿠션, 밴딩, 발볼, 안정 …)
     * 어느 쪽에도 해당하지 않으면 세 연령대 모두. 관리자가 상품 관리(UC-16)에서 직접 고치면 그 값이 우선한다.
     */
    public static List<String> guessAgeGroups(String text) {
        String source = text == null ? "" : text;
        List<String> tags = new ArrayList<>();
        if (containsAny(source, "캐주얼", "스니커즈", "데일리", "경쾌", "트렌디", "크롭", "스포티", "미니멀", "포인트", "스트리트")) {
            tags.add("40대");
        }
        if (containsAny(source, "클래식", "우아", "단정", "캐시미어", "격식", "모임", "헤리티지", "포멀", "테일러")) {
            tags.add("50대");
        }
        if (containsAny(source, "편안", "편한", "쿠션", "컴포트", "밴딩", "발볼", "안정", "커버", "체형", "낮은 굽")) {
            tags.add("60대");
        }
        return tags.isEmpty() ? AGE_GROUPS : tags;
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
