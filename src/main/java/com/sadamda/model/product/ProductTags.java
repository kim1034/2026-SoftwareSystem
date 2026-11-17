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
    /** 회원가입·내 정보·상품 검색 화면(view/member/SignUpPanel)과 같은 문자열. */
    public static final List<String> BODY_TYPES = List.of("마른 체형", "보통 체형", "통통한 체형", "하체 발달형", "상체 발달형");
    /** 옷(상의·하의·원피스·아우터) 옵션 사이즈. 회원가입의 "평소 사이즈"와 같은 문자열. */
    public static final List<String> CLOTHING_SIZES = List.of("S (55)", "M (66)", "L (77)", "XL (88)", "XXL (99)");
    /** 화면에서 "선택 안 함"을 고르면 이 값이 온다. 조건 없음으로 본다. */
    public static final String NOT_SELECTED = "선택 안 함";

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
        tags.add("보통 체형");
        if (containsAny(source, "와이드", "A라인", "플레어", "밴딩", "스트레이트", "일자", "H라인")) {
            tags.add("하체 발달형");
            tags.add("통통한 체형");
        }
        if (containsAny(source, "박시", "루즈", "오버", "여유", "드레이프", "흐르", "체형", "커버")) {
            tags.add("통통한 체형");
            tags.add("상체 발달형");
        }
        if (containsAny(source, "슬림", "크롭", "타이트", "피트", "테일러")) {
            tags.add("마른 체형");
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

    /**
     * 체형 이름을 BODY_TYPES 의 값으로 맞춘다. 예전 값("보통형", "하체발달형")이나 띄어쓰기가 달라도 같은 체형으로 본다.
     * 비었거나 "선택 안 함"이거나 모르는 값이면 null.
     */
    public static String bodyType(String value) {
        if (value == null || value.isBlank() || NOT_SELECTED.equals(value.trim())) {
            return null;
        }
        String key = value.replace(" ", "");
        for (String each : BODY_TYPES) {
            String compact = each.replace(" ", "");
            if (compact.equals(key) || compact.replace("한체형", "형").replace("체형", "형").equals(key)) {
                return each;
            }
        }
        return null;
    }

    /** 연령대. "선택 안 함"·빈 값이면 null. */
    public static String ageGroup(String value) {
        return value == null || value.isBlank() || NOT_SELECTED.equals(value.trim()) ? null : value.trim();
    }

    /**
     * 사이즈가 같은지 본다. "M (66)", "66", "m" 처럼 표기가 달라도 같은 사이즈면 true.
     * 신발(235)·FREE 처럼 괄호가 없는 사이즈는 글자 그대로 비교한다.
     */
    public static boolean sameSize(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.trim().equalsIgnoreCase(b.trim())) {
            return true;
        }
        String[] left = sizeKeys(a);
        String[] right = sizeKeys(b);
        for (String x : left) {
            for (String y : right) {
                if (!x.isEmpty() && x.equalsIgnoreCase(y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** "M (66)" → [M, 66], "66" → [66] */
    private static String[] sizeKeys(String size) {
        return size.replace(")", "").split("\\s*\\(\\s*|\\s+");
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
