package com.sadamda.model.tryon;

import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;

import java.util.List;
import java.util.Locale;

/**
 * 옷 입혀보기에서 입히는 자리 14곳. 화면 오른쪽에 이 순서로, 부위(Group)별로 묶어 보여준다.
 * 주얼리는 데이터의 세부 분류(목걸이/펜던트, 팔찌/뱅글, 귀걸이/브로치/반지)와 상품명으로 다섯 자리로 나눈다.
 */
public enum DressUpSlot {
    TOP("상의", Group.CLOTHES, Category.TOP, "top", "keep the plain white t-shirt she wears in Image 1"),
    BOTTOM("하의", Group.CLOTHES, Category.BOTTOM, "bottom", "keep the plain white shorts she wears in Image 1"),
    DRESS("원피스", Group.CLOTHES, Category.DRESS, "dress or two-piece set", null),
    OUTER("아우터", Group.CLOTHES, Category.OUTER, "outerwear (coat or jacket)", null),
    HAT("모자", Group.HEAD_NECK, Category.HAT, "hat", "none (keep her head and hair exactly as in Image 1, no hat)"),
    SCARF("스카프", Group.HEAD_NECK, Category.SCARF, "scarf", null),
    EARRINGS("귀걸이", Group.JEWELRY, Category.JEWELRY, "earrings", null),
    NECKLACE("목걸이", Group.JEWELRY, Category.JEWELRY, "necklace", null),
    BRACELET("팔찌 · 뱅글", Group.JEWELRY, Category.JEWELRY, "bracelet", null),
    RING("반지", Group.JEWELRY, Category.JEWELRY, "ring", null),
    BROOCH("브로치", Group.JEWELRY, Category.JEWELRY, "brooch", null),
    SOCKS("양말", Group.FEET_BAG, Category.SOCKS, "socks", null),
    SHOES("신발", Group.FEET_BAG, Category.SHOES, "shoes", "none (keep her barefoot as in Image 1)"),
    BAG("가방", Group.FEET_BAG, Category.BAG, "bag", null);

    /** 화면에서 자리를 묶는 부위. */
    public enum Group {
        CLOTHES("옷"), HEAD_NECK("머리 · 목"), JEWELRY("주얼리"), FEET_BAG("발 · 소품");

        private final String label;

        Group(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final String label;
    private final Group group;
    private final Category category;
    private final String english;
    private final String whenEmpty;

    DressUpSlot(String label, Group group, Category category, String english, String whenEmpty) {
        this.label = label;
        this.group = group;
        this.category = category;
        this.english = english;
        this.whenEmpty = whenEmpty;
    }

    public String label() {
        return label;
    }

    public Group group() {
        return group;
    }

    /** 이 자리에 입힐 수 있는 상품 카테고리. 주얼리 다섯 자리는 모두 JEWELRY 이고 accepts 로 다시 나눈다. */
    public Category category() {
        return category;
    }

    public String english() {
        return english;
    }

    /**
     * 캐릭터에 아무것도 고르지 않았을 때의 지시: 기본 캐릭터의 차림을 그대로 둔다.
     * null 이면 기본 캐릭터에 원래 없는 것이라 지시문에 넣지 않는다 (OUTFIT 목록에 없는 것은 그리지 말라고 따로 적는다).
     */
    public String whenEmpty() {
        return whenEmpty;
    }

    /** 같이 입을 수 없는 자리. 원피스(투피스·셋업 포함)는 상의·하의를 대신한다. */
    public List<DressUpSlot> conflicts() {
        return switch (this) {
            case DRESS -> List.of(TOP, BOTTOM);
            case TOP, BOTTOM -> List.of(DRESS);
            default -> List.of();
        };
    }

    /** 이 상품을 이 자리에 입힐 수 있는지. */
    public boolean accepts(Product product) {
        return product != null && slotFor(product) == this;
    }

    /** 상품을 입힐 자리. 주얼리인데 종류를 알 수 없으면 null. */
    public static DressUpSlot slotFor(Product product) {
        if (product == null || product.getCategory() == null) {
            return null;
        }
        if (product.getCategory() == Category.JEWELRY) {
            return jewelrySlot(product.getSubCategory(), product.getName());
        }
        for (DressUpSlot slot : values()) {
            if (slot.category == product.getCategory()) {
                return slot;
            }
        }
        return null;
    }

    /**
     * 주얼리 종류. 세부 분류가 목걸이·팔찌면 그대로, "귀걸이/브로치/반지"처럼 묶여 있으면 상품명으로 나눈다.
     * "earring" 에 "ring" 이 들어 있으므로 귀걸이를 반지보다 먼저 본다.
     */
    static DressUpSlot jewelrySlot(String subCategory, String name) {
        String sub = subCategory == null ? "" : subCategory;
        String text = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (sub.contains("목걸이") || sub.contains("펜던트")) {
            return NECKLACE;
        }
        if (sub.contains("팔찌") || sub.contains("뱅글")) {
            return BRACELET;
        }
        if (containsAny(text, "브로치", "brooch", "코사지", "corsage")) {
            return BROOCH;
        }
        if (containsAny(text, "귀걸이", "귀고리", "이어링", "이어커프", "earring", "ear cuff", "hoop", "stud")) {
            return EARRINGS;
        }
        if (containsAny(text, "반지", "ring", "링")) {
            return RING;
        }
        if (containsAny(text, "목걸이", "펜던트", "necklace", "pendant")) {
            return NECKLACE;
        }
        if (containsAny(text, "팔찌", "뱅글", "bracelet", "bangle")) {
            return BRACELET;
        }
        return null;
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
