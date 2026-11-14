package com.sadamda.model.tryon;

import com.sadamda.model.product.Product;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 옷 입혀보기 요청을 AI 이미지 합성 서비스에 보낼 영어 지시문으로 바꾼다.
 *
 * 함께 보내는 이미지 순서
 * - Image 1: 신체 스펙이 반영된 기본 캐릭터 (CharacterFigure.shaped)
 * - Image 2~: 고른 상품의 사진 (자리 순서대로, 사진을 구한 것만)
 *
 * 지시문이 요구하는 것
 * 1) 캐릭터의 얼굴·머리·몸 비율·자세·배경·그림체는 Image 1 그대로
 * 2) 상품 사진에서는 그 상품(옷) 하나만 골라내고, 사진을 붙이지 말고 캐릭터의 자세에 맞게 입은 모습으로 새로 그릴 것
 * 3) 옷도 캐릭터와 같은 그림체로 그리되, 같은 제품임을 알아볼 수 있게 디자인은 유지할 것
 * 4) 사진을 보낸 상품은 사진만 따를 것 (상품명·색상 글은 보내지 않는다). 사진을 못 구한 상품만 글로 설명한다
 */
public final class DressUpPrompt {
    private static final Map<String, String> SHAPE = Map.of(
            "마른 체형", "a slender frame",
            "보통 체형", "balanced proportions",
            "통통한 체형", "a soft, rounded figure",
            "상체 발달형", "a fuller upper body with slimmer legs",
            "하체 발달형", "fuller hips and thighs with a slimmer upper body");

    private DressUpPrompt() {
    }

    /**
     * @param referenceSlots 상품 사진을 함께 보내는 자리. 보내는 순서와 같아야 한다 (Image 2 부터).
     */
    public static String build(CharacterStyle style, CharacterSpec spec, Outfit outfit, List<DressUpSlot> referenceSlots) {
        StringBuilder text = new StringBuilder();
        text.append("TASK: Edit Image 1. Dress the woman in Image 1 in the products listed below ")
                .append("and return ONE finished full-body picture of her wearing them.\n\n");

        text.append("CHARACTER (Image 1): Keep her exactly as she is: the same face, hairstyle, skin tone, pose, ")
                .append("camera framing, her size and position in the frame, the floor line and the plain background. ")
                .append("She looks like a woman in her ").append(spec.ageGroup().replace("대", "")).append("s. ")
                .append("The art style of Image 1 is ").append(style.prompt()).append(". Keep this art style.\n\n");

        text.append("BODY: She is ").append(spec.heightCm()).append(" cm tall and weighs ").append(spec.weightKg())
                .append(" kg (BMI ").append(String.format(Locale.ROOT, "%.1f", spec.bmi())).append(", ")
                .append(build(spec.bmi())).append("), with ").append(SHAPE.get(spec.bodyType())).append(". ")
                .append("Image 1 is already drawn at this height and weight: her height in the frame and the width of ")
                .append("her shoulders, waist, hips, arms and legs are correct. Preserve her height in the frame and these ")
                .append("body widths. You may only refine the anatomy so that it looks natural for this weight ")
                .append("(for example a softer belly, fuller arms, neck and face for a heavy build). ")
                .append("In Image 1 the top of her head is about ").append(headTopPercent(spec))
                .append("% down from the top edge and her feet are about 97% down; keep both positions. ")
                .append(spec.bmi() >= 25 ? "She is clearly overweight: a slimmer or more elongated result is wrong. "
                        : spec.bmi() < 18.5 ? "She is clearly thin: a fuller result is wrong. " : "")
                .append("Do not make her slimmer, heavier, taller or shorter, and do not zoom or crop. ")
                .append("The clothes must fit this body.\n\n");

        text.append("OUTFIT:\n");
        for (DressUpSlot slot : DressUpSlot.values()) {
            Product product = outfit.get(slot);
            if (product == null) {
                text.append("- ").append(slot.english()).append(": ").append(slot.whenEmpty()).append("\n");
            } else {
                int index = referenceSlots.indexOf(slot);
                if (index >= 0) {
                    // 사진이 있으면 사진만 근거로 삼는다. 데이터의 상품명·색상 글이 사진과 다른 상품을 가리키는 경우가 있어서
                    // 글을 함께 보내면 AI 가 글을 따라 그려 화면의 상품 사진과 다른 결과가 나온다.
                    text.append("- ").append(slot.english()).append(": the exact ").append(slot.english())
                            .append(" shown in Image ").append(index + 2);
                } else {
                    text.append("- ").append(slot.english()).append(": ").append(describe(product))
                            .append(" (no photo; draw it from this description)");
                }
                text.append("\n");
            }
        }
        text.append("\n");

        if (!referenceSlots.isEmpty()) {
            text.append("PRODUCT PHOTOS: ");
            for (int i = 0; i < referenceSlots.size(); i++) {
                text.append("Image ").append(i + 2).append(" is the product photo for the ")
                        .append(referenceSlots.get(i).english()).append(". ");
            }
            text.append("From each product photo use ONLY that one product. Ignore everything else in the photo: ")
                    .append("the model or mannequin wearing it, their body and pose, other clothes, the background, ")
                    .append("props, text and logos of the shop. The product photo is the ONLY reference for that product: ")
                    .append("copy its type, shape, colors, material and details from the photo, even if a product name ")
                    .append("suggests something else. For example, if the photo shows a knitted hood, she wears that hood, ")
                    .append("not a different kind of hat.\n\n");
        }

        text.append("HOW TO DRESS HER: Redraw each product as a real garment that she is actually wearing. ")
                .append("It must wrap around her body and follow her pose: sleeves follow her arms, the top sits on her ")
                .append("shoulders and torso, the bottom hangs from her waist and follows her legs, shoes are on her feet ")
                .append("standing on the floor, a hat sits on her head over her hair, jewelry is worn where it belongs ")
                .append("(neck, ear, wrist, finger or lapel). Draw natural folds, drape, layering and soft contact shadows. ")
                .append("Do NOT paste, overlay or collage the product photos. No flat cut-outs, cards, stickers, insets, ")
                .append("panels, frames, labels or side-by-side views. The result must be a single coherent picture of one woman. ")
                .append("Do not add any garment or accessory that is not in the OUTFIT list (no extra inner layers, scarves, ")
                .append("bags, belts or glasses). If a cardigan or jacket is worn open, the layer under it is what the list ")
                .append("says for that part.\n\n");

        text.append("STYLE CONSISTENCY: Draw every garment and accessory in the same art style as the character in Image 1 ")
                .append("(the same rendering technique, line quality, shading and lighting), so that she and her clothes ")
                .append("look like one image made by one artist. Do not leave the clothes looking like photographs ")
                .append("unless Image 1 itself is a photograph.\n\n");

        text.append("PRODUCT FIDELITY: While matching the art style, keep each product's design so that a shopper ")
                .append("recognizes the same product: the same colors, pattern and print placement, silhouette and length, ")
                .append("neckline or collar, sleeves, closures (buttons, zippers, ties), pockets, trims, stitching details ")
                .append("and material look. Do not invent a different design.\n\n");

        text.append("OUTPUT: One portrait image, full body from the top of her head to her feet, same framing as Image 1, ")
                .append("no text, no watermark, no extra people.");
        return text.toString();
    }

    /** 스펙을 반영한 기본 캐릭터에서 머리 꼭대기의 세로 위치(%). CharacterFigure 의 계산과 같다. */
    private static long headTopPercent(CharacterSpec spec) {
        return Math.round(CharacterFigure.headTop(spec) * 100);
    }

    /** 체질량지수를 영어 체격 설명으로. */
    static String build(double bmi) {
        if (bmi < 18.5) {
            return "a thin build";
        }
        if (bmi < 23) {
            return "an average build";
        }
        if (bmi < 25) {
            return "a slightly full build";
        }
        if (bmi < 30) {
            return "a visibly plump, overweight build";
        }
        return "a heavy, plus-size build";
    }

    /** 상품 설명: 종류, 이름, 소재·색상, 코디 포인트. 한글이 섞여도 된다. */
    public static String describe(Product product) {
        StringBuilder text = new StringBuilder();
        if (product.getSubCategory() != null && !product.getSubCategory().isBlank()) {
            text.append("[").append(product.getSubCategory()).append("] ");
        }
        text.append(product.getName());
        if (product.getMaterial() != null && !product.getMaterial().isBlank()) {
            text.append(" (").append(product.getMaterial()).append(")");
        }
        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            text.append(" - ").append(product.getDescription());
        }
        return text.toString();
    }
}
