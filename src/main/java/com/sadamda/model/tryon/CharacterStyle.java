package com.sadamda.model.tryon;

/**
 * 옷 입혀보기 캐릭터의 그림체 4가지. 캐릭터는 모두 여성이다.
 * 그림체마다 기본 캐릭터 이미지가 하나씩 있다 (resources/characters/*.png, 896×1200).
 * 기본 캐릭터는 흰 반팔 티셔츠·흰 반바지·맨발 차림이고, 여기에 옷을 입힌다.
 */
public enum CharacterStyle {
    REAL("실사", "real.png",
            "a photorealistic studio photograph with natural skin, real fabric texture and soft studio lighting"),
    DISNEY("디즈니 캐릭터", "disney.png",
            "a 3D animated feature-film character render in the Disney/Pixar manner: "
                    + "soft rounded forms, simplified stylized materials, warm global illumination"),
    GAME_3D("3D 게임 캐릭터", "game3d.png",
            "a real-time 3D video game character render: clean game-engine shading, "
                    + "modelled 3D cloth with stylized but detailed materials"),
    ANIME("애니메이션 캐릭터", "anime.png",
            "a 2D Japanese anime illustration: clean dark outlines, flat cel shading with simple shadow shapes");

    private final String label;
    private final String imageFile;
    private final String prompt;

    CharacterStyle(String label, String imageFile, String prompt) {
        this.label = label;
        this.imageFile = imageFile;
        this.prompt = prompt;
    }

    public String label() {
        return label;
    }

    /** 기본 캐릭터 이미지의 리소스 경로. */
    public String imageResource() {
        return "/characters/" + imageFile;
    }

    /** AI 에 보내는 그림체 설명. 옷도 이 그림체로 그리게 한다. */
    public String prompt() {
        return prompt;
    }

    @Override
    public String toString() {
        return label;
    }
}
