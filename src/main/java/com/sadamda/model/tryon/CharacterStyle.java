package com.sadamda.model.tryon;

/** 옷 입혀보기 캐릭터의 그림체 4가지. 캐릭터는 모두 여성이다. */
public enum CharacterStyle {
    REAL("실사",
            "a photorealistic studio fashion photograph of a beautiful, elegant adult woman, natural skin, soft lighting"),
    DISNEY("디즈니 캐릭터",
            "an original adult woman character drawn in the style of a classic Disney animated feature film: "
                    + "large expressive eyes, graceful proportions, clean painted look. "
                    + "She must be a new original character, not any existing film character"),
    GAME_3D("3D 게임 캐릭터",
            "a stylized high-quality 3D video game character render of a beautiful adult woman, "
                    + "detailed materials, cinematic real-time lighting"),
    ANIME("애니메이션 캐릭터",
            "a Japanese anime illustration of a beautiful adult woman, clean line art, cel shading");

    private final String label;
    private final String prompt;

    CharacterStyle(String label, String prompt) {
        this.label = label;
        this.prompt = prompt;
    }

    public String label() {
        return label;
    }

    /** AI 에 보내는 그림체 설명. */
    public String prompt() {
        return prompt;
    }

    @Override
    public String toString() {
        return label;
    }
}
