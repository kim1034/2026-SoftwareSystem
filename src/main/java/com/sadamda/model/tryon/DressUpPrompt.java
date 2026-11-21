package com.sadamda.model.tryon;

import com.sadamda.model.product.Product;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 옷 입혀보기 요청을 AI 이미지 합성 서비스에 보낼 영어 지시문으로 바꾼다.
 *
 * 캐릭터 (build) — 함께 보내는 이미지 순서
 * - Image 1: 신체 스펙이 반영된 기본 캐릭터 (CharacterFigure.shaped)
 * - Image 2~: 고른 상품의 사진 (자리 순서대로, 사진을 구한 것만)
 *
 * 내 사진 (buildPhoto) — 함께 보내는 이미지 순서
 * - Image 1: 사용자가 올린 전신 사진
 * - Image 2: 고른 배경 사진 (배경을 고르지 않았으면 없음)
 * - 그다음: 고른 상품의 사진
 *
 * 지시문이 요구하는 것
 * 1) 사람의 얼굴·머리·몸 비율·자세는 Image 1 그대로 (캐릭터는 그림체·배경도 그대로)
 * 2) 상품 사진에서는 그 상품(옷) 하나만 골라내고, 사진을 붙이지 말고 사람의 자세에 맞게 입은 모습으로 새로 그릴 것
 * 3) 캐릭터는 옷도 같은 그림체로, 사진은 실제 사진처럼. 같은 제품임을 알아볼 수 있게 디자인은 유지할 것
 * 4) 사진을 보낸 상품은 사진만 따를 것 (상품명·색상 글은 보내지 않는다). 사진을 못 구한 상품만 글로 설명한다
 * 5) 내 사진에 배경을 고르면 그 장소에서 찍은 사진처럼 자연스럽게 넣을 것 (빛·크기·원근·그림자)
 * 6) 아우터 상품 사진 속 모델이 안에 입은 옷(니트·셔츠·치마 등)은 따라 그리지 말고, 고른 상의(없으면 원래 상의)를
 *    아우터 안에 입힐 것. 아우터를 열어 고른 상의가 보이게 한다 (LAYERING)
 */
public final class DressUpPrompt {
    private static final Map<String, String> SHAPE = Map.of(
            "마른 체형", "a slender frame",
            "보통 체형", "balanced proportions",
            "통통한 체형", "a soft, rounded figure",
            "상체 발달형", "a fuller upper body with slimmer legs",
            "하체 발달형", "fuller hips and thighs with a slimmer upper body");

    private static final String WHERE_ITEMS_GO = "Each item goes where it belongs: a top sits on her shoulders and torso, "
            + "a bottom hangs from her waist and follows her legs, a dress or two-piece set replaces both the top and "
            + "the bottom, a coat or jacket is worn over the top, a hat sits on her head over her hair, a scarf is worn "
            + "around her neck or shoulders, earrings on her ears, a necklace around her neck, a bracelet on her wrist, "
            + "a ring on a finger, a brooch pinned on her chest or lapel, socks on her feet (under the shoes if she "
            + "wears shoes), shoes on her feet standing on the floor, and a bag on her shoulder or in her hand. ";

    private DressUpPrompt() {
    }

    /**
     * 캐릭터에 옷을 입히는 지시문.
     *
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
            if (product != null) {
                text.append(itemLine(slot, product, referenceSlots, 2));
            } else {
                String empty = characterWhenEmpty(slot, outfit);
                if (empty != null) {
                    text.append("- ").append(slot.english()).append(": ").append(empty).append("\n");
                }
            }
        }
        text.append("\n");

        appendProductPhotos(text, referenceSlots, 2);
        appendLayering(text, outfit, referenceSlots, 2, false);

        text.append("HOW TO DRESS HER: Redraw each product as a real garment or accessory that she is actually wearing. ")
                .append("It must wrap around her body and follow her pose: sleeves follow her arms. ").append(WHERE_ITEMS_GO)
                .append("Draw natural folds, drape, layering and soft contact shadows. ")
                .append("Do NOT paste, overlay or collage the product photos. No flat cut-outs, cards, stickers, insets, ")
                .append("panels, frames, labels or side-by-side views. The result must be a single coherent picture of one woman. ")
                .append("Do not add any garment or accessory that is not in the OUTFIT list (no extra inner layers, scarves, ")
                .append("bags, belts or glasses). If a cardigan or jacket is worn open, the layer under it is what the list ")
                .append("says for that part.\n\n");

        text.append("STYLE CONSISTENCY: Draw every garment and accessory in the same art style as the character in Image 1 ")
                .append("(the same rendering technique, line quality, shading and lighting), so that she and her clothes ")
                .append("look like one image made by one artist. Do not leave the clothes looking like photographs ")
                .append("unless Image 1 itself is a photograph.\n\n");

        appendFidelity(text);

        text.append("OUTPUT: One portrait image, full body from the top of her head to her feet, same framing as Image 1, ")
                .append("no text, no watermark, no extra people.");
        return text.toString();
    }

    /**
     * 사용자가 올린 사진에 옷을 입히고 배경을 바꾸는 지시문.
     *
     * @param referenceSlots 상품 사진을 함께 보내는 자리 (보내는 순서). 배경이 있으면 Image 3 부터, 없으면 Image 2 부터.
     * @param hasBackground  배경 사진(Image 2)을 함께 보내는지. false 면 사진의 원래 배경을 그대로 둔다.
     */
    public static String buildPhoto(Outfit outfit, List<DressUpSlot> referenceSlots, boolean hasBackground) {
        int firstProduct = hasBackground ? 3 : 2;
        StringBuilder text = new StringBuilder();
        text.append("TASK: Edit Image 1, a real photograph of a woman. ");
        if (!outfit.isEmpty()) {
            text.append("Dress her in the products listed below");
            text.append(hasBackground ? " and place her in the location shown in Image 2" : "");
        } else {
            text.append("Place her in the location shown in Image 2");
        }
        text.append(". Return ONE finished, photorealistic full-body photograph of her.\n\n");

        text.append("PERSON (Image 1): Keep her exactly as she is: the same face and identity, facial expression, ")
                .append("hairstyle, skin tone, body shape and proportions, height and pose. She must still be clearly ")
                .append("recognizable as the same person. Do not beautify, slim, reshape, age or de-age her, and do not ")
                .append("change her body to fit the clothes: the clothes must fit her body.\n\n");

        if (hasBackground) {
            text.append("LOCATION (Image 2): Replace the original background of Image 1 with the place shown in Image 2. ")
                    .append("Use Image 2 only as the location; ignore any people, text, logos, borders or watermarks in it. ")
                    .append("Make it look as if she was photographed there: she stands on a natural spot on the ground, ")
                    .append("at a believable size for that spot, with the camera height and perspective of Image 2, ")
                    .append("lighting direction, color and brightness that match the scene, and soft contact shadows under ")
                    .append("her feet. Her whole body from head to feet stays in the picture.\n\n");
        } else {
            text.append("BACKGROUND: Keep the original background of Image 1 exactly as it is.\n\n");
        }

        text.append("OUTFIT:\n");
        if (outfit.isEmpty()) {
            text.append("- keep everything she wears in Image 1 exactly as it is\n");
        } else {
            for (DressUpSlot slot : DressUpSlot.values()) {
                Product product = outfit.get(slot);
                if (product != null) {
                    text.append(itemLine(slot, product, referenceSlots, firstProduct));
                }
            }
            if (outfit.get(DressUpSlot.DRESS) != null) {
                text.append("- the dress or set replaces whatever she wears on her upper and lower body in Image 1\n");
            }
            text.append("- everything else she wears in Image 1 (anything not listed above) stays exactly as it is\n");
        }
        text.append("\n");

        appendProductPhotos(text, referenceSlots, firstProduct);
        appendLayering(text, outfit, referenceSlots, firstProduct, true);

        if (!outfit.isEmpty()) {
            text.append("HOW TO DRESS HER: Replace only what the OUTFIT list changes. Redraw each product as a real ")
                    .append("garment or accessory that she is actually wearing, fitted to her own body and pose: ")
                    .append("sleeves follow her arms. ").append(WHERE_ITEMS_GO)
                    .append("Draw natural folds, drape, layering and soft contact shadows, with real fabric texture. ")
                    .append("Do NOT paste, overlay or collage the product photos. No flat cut-outs, cards, stickers, insets, ")
                    .append("panels, frames, labels or side-by-side views. The result must be a single coherent photograph ")
                    .append("of one woman. Do not add anything that is not in the OUTFIT list.\n\n");
            appendFidelity(text);
        }

        text.append("OUTPUT: One photorealistic portrait photograph, full body from the top of her head to her feet, ")
                .append("no text, no watermark, no extra people.");
        return text.toString();
    }

    /** OUTFIT 목록의 한 줄. 사진을 보낸 상품은 그 사진 번호, 아니면 글 설명. */
    private static String itemLine(DressUpSlot slot, Product product, List<DressUpSlot> referenceSlots, int firstImage) {
        int index = referenceSlots.indexOf(slot);
        if (index >= 0) {
            // 사진이 있으면 사진만 근거로 삼는다. 데이터의 상품명·색상 글이 사진과 다른 상품을 가리키는 경우가 있어서
            // 글을 함께 보내면 AI 가 글을 따라 그려 화면의 상품 사진과 다른 결과가 나온다.
            String line = "- " + slot.english() + ": the exact " + slot.english() + " shown in Image " + (index + firstImage);
            if (slot == DressUpSlot.OUTER) {
                line += ", ONLY the coat or jacket itself, worn as the outer layer (not the clothes the model wears "
                        + "under it in that photo)";
            }
            return line + "\n";
        }
        return "- " + slot.english() + ": " + describe(product) + " (no photo; draw it from this description)\n";
    }

    /** 캐릭터에서 비어 있는 자리의 지시. 원피스를 입으면 흰 티셔츠·반바지는 없고, 양말만 신으면 맨발이 아니다. */
    private static String characterWhenEmpty(DressUpSlot slot, Outfit outfit) {
        if ((slot == DressUpSlot.TOP || slot == DressUpSlot.BOTTOM) && outfit.get(DressUpSlot.DRESS) != null) {
            return "none (the dress replaces her white t-shirt and shorts)";
        }
        if (slot == DressUpSlot.SHOES && outfit.get(DressUpSlot.SOCKS) != null) {
            return "none (no shoes; she wears only the socks)";
        }
        return slot.whenEmpty();
    }

    private static void appendProductPhotos(StringBuilder text, List<DressUpSlot> referenceSlots, int firstImage) {
        if (referenceSlots.isEmpty()) {
            return;
        }
        text.append("PRODUCT PHOTOS: ");
        for (int i = 0; i < referenceSlots.size(); i++) {
            text.append("Image ").append(i + firstImage).append(" is the product photo for the ")
                    .append(referenceSlots.get(i).english()).append(". ");
        }
        text.append("From each product photo use ONLY that one product. Ignore everything else in the photo: ")
                .append("the model or mannequin wearing it, their body and pose, other clothes, the background, ")
                .append("props, text and logos of the shop. The product photo is the ONLY reference for that product: ")
                .append("copy its type, shape, colors, material and details from the photo, even if a product name ")
                .append("suggests something else. For example, if the photo shows a knitted hood, she wears that hood, ")
                .append("not a different kind of hat. A product photo often shows the model wearing other clothes too ")
                .append("(for example a knit, cardigan, turtleneck or shirt under a coat, or a skirt or trousers with a jacket). ")
                .append("Those other clothes are never part of the outfit: do not copy them onto her.\n\n");
    }

    /**
     * 아우터를 입힐 때 안에 입는 옷을 분명히 한다. 아우터 상품 사진의 모델은 보통 니트·셔츠 등을 안에 입고 있어서,
     * 이 지시가 없으면 AI 가 고른 상의 대신 그 옷을 그리는 일이 있었다 (2026-11-20 확인).
     */
    private static void appendLayering(StringBuilder text, Outfit outfit, List<DressUpSlot> referenceSlots,
                                       int firstImage, boolean photo) {
        if (outfit.get(DressUpSlot.OUTER) == null) {
            return;
        }
        int outerImage = imageNumber(DressUpSlot.OUTER, referenceSlots, firstImage);
        DressUpSlot innerSlot = outfit.get(DressUpSlot.DRESS) != null ? DressUpSlot.DRESS
                : outfit.get(DressUpSlot.TOP) != null ? DressUpSlot.TOP : null;
        String inner;
        if (innerSlot == null) {
            inner = photo ? "her own top from Image 1" : "the plain white t-shirt from Image 1";
        } else {
            int innerImage = imageNumber(innerSlot, referenceSlots, firstImage);
            inner = "the " + innerSlot.english() + (innerImage > 0 ? " from Image " + innerImage
                    : " described in the OUTFIT list");
        }
        text.append("LAYERING: She wears ").append(inner).append(" UNDER the outerwear. Wherever the inner layer ")
                .append("shows (neckline, collar, the front opening, cuffs and hem), it must be ").append(inner)
                .append(", with the same color, pattern, neckline and texture. ");
        if (outerImage > 0) {
            text.append("In Image ").append(outerImage).append(" the model wears other clothes under or with the ")
                    .append("coat or jacket (such as a knit, cardigan, turtleneck, shirt, skirt or trousers). They are NOT ")
                    .append("part of the product and must not appear on her; take only the coat or jacket from Image ")
                    .append(outerImage).append(". This includes small parts of those clothes: knit cuffs or sleeves ")
                    .append("turned back over the coat sleeves, a collar or hem peeking out. The coat sleeves end in the coat's ")
                    .append("own cuffs; any cuff or collar that shows from underneath belongs to ").append(inner).append(". ");
        }
        int bottomImage = imageNumber(DressUpSlot.BOTTOM, referenceSlots, firstImage);
        if (outfit.get(DressUpSlot.BOTTOM) != null) {
            text.append("Below the outerwear she wears the bottom ")
                    .append(bottomImage > 0 ? "from Image " + bottomImage : "described in the OUTFIT list")
                    .append(", not the bottom from the outerwear photo. ");
        }
        if (innerSlot != null) {
            text.append("If closing the outerwear would hide ").append(inner).append(", wear it open at the front ")
                    .append("so that ").append(inner).append(" is clearly visible. ");
        }
        text.append("Final check before you answer: the visible inner layer is ").append(inner)
                .append(", not the inner clothes of the outerwear photo.\n\n");
    }

    /** 그 자리 상품 사진이 몇 번째 이미지인지. 사진을 보내지 않았으면 -1. */
    private static int imageNumber(DressUpSlot slot, List<DressUpSlot> referenceSlots, int firstImage) {
        int index = referenceSlots.indexOf(slot);
        return index < 0 ? -1 : index + firstImage;
    }

    private static void appendFidelity(StringBuilder text) {
        text.append("PRODUCT FIDELITY: While matching the art style, keep each product's design so that a shopper ")
                .append("recognizes the same product: the same colors, pattern and print placement, silhouette and length, ")
                .append("neckline or collar, sleeves, closures (buttons, zippers, ties), pockets, trims, stitching details ")
                .append("and material look. Do not invent a different design.\n\n");
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
