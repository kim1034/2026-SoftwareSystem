package com.sadamda.model.tryon;

import com.sadamda.model.product.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 옷 입혀보기 요청을 AI 이미지 합성 서비스에 보낼 영어 지시문으로 바꾼다. */
public final class DressUpPrompt {
    private static final Map<String, String> BODY = Map.of(
            "마른형", "a slim build",
            "보통형", "an average build",
            "통통형", "a soft, curvy plus-size build",
            "상체발달형", "a build with a fuller upper body and slimmer legs",
            "하체발달형", "a build with fuller hips and thighs and a slimmer upper body");

    private DressUpPrompt() {
    }

    /**
     * @param referenceLabels AI 에 함께 보내는 참고 이미지의 설명. 보내는 순서와 같아야 한다.
     *                        (hasBase 이면 첫 이미지는 캐릭터 기준 이미지이고 여기에 넣지 않는다)
     */
    public static String build(CharacterStyle style, CharacterSpec spec, Outfit outfit,
                               boolean hasBase, List<String> referenceLabels) {
        StringBuilder text = new StringBuilder();
        text.append("Create one full-body image: ").append(style.prompt()).append(". ");
        text.append("She is a woman in her ").append(spec.ageGroup().replace("대", "")).append("s, about ")
                .append(spec.heightCm()).append(" cm tall, with ").append(BODY.get(spec.bodyType())).append(". ");
        text.append("She must clearly look her age (a mature, graceful woman in her ")
                .append(spec.ageGroup().replace("대", "")).append("s, not a young girl), even in a stylized art style. ");
        text.append("She stands facing the viewer in a relaxed, friendly pose. ")
                .append("Show her whole body from the top of her head to her shoes, centered, ")
                .append("on a plain soft light-gray studio background. Portrait orientation. No text, no logos, no watermark. ");

        int imageNumber = 1;
        if (hasBase) {
            text.append("Image 1 is the character reference: keep exactly the same face, hairstyle, body, pose, ")
                    .append("art style and background, and change only what she wears. ");
            imageNumber++;
        }
        text.append("She wears exactly this outfit and nothing else on top of it: ");
        List<String> lines = new ArrayList<>();
        for (DressUpSlot slot : DressUpSlot.values()) {
            Product product = outfit.get(slot);
            if (product == null) {
                lines.add(slot.english() + ": " + slot.whenEmpty());
            } else {
                lines.add(slot.english() + ": " + describe(product));
            }
        }
        text.append(String.join("; ", lines)).append(". ");

        if (!referenceLabels.isEmpty()) {
            text.append("Product photos are attached for reference. ");
            for (String label : referenceLabels) {
                text.append("Image ").append(imageNumber++).append(" shows the ").append(label).append(". ");
            }
            text.append("Match each product's shape, color, material and details as closely as possible, ")
                    .append("fitted naturally on her body. Use only the product from each photo, ")
                    .append("not the model, background or other items in it.");
        }
        return text.toString();
    }

    /** 상품 설명: 이름, 소재·색상, 코디 포인트. 한글이 섞여도 된다. */
    public static String describe(Product product) {
        StringBuilder text = new StringBuilder(product.getName());
        if (product.getMaterial() != null && !product.getMaterial().isBlank()) {
            text.append(" (").append(product.getMaterial()).append(")");
        }
        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            text.append(" - ").append(product.getDescription());
        }
        return text.toString();
    }
}
