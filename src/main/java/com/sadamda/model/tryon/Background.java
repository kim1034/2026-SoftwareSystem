package com.sadamda.model.tryon;

/**
 * 내 사진으로 입어보기에서 고르는 배경 사진 (계절·나라별, 사람 없는 건물·자연).
 * 사진 파일은 저장소에 넣지 않고 주소만 둔다. 출처(핀 주소)를 함께 보관한다.
 *
 * @param name      "봄 · 일본 벚꽃 가로수길" 처럼 계절·나라가 붙은 이름
 * @param shortName 배경 버튼에 크게 쓰는 짧은 장소 이름
 * @param kind      건물 또는 자연
 * @param source    사진을 찾은 Pinterest 핀 주소
 */
public record Background(String id, String name, String shortName, String imageUrl,
                         String season, String country, String kind, String source) {

    /** 배경 버튼 윗줄의 작은 글씨: "봄 · 일본". */
    public String caption() {
        return season + " · " + country;
    }
}
