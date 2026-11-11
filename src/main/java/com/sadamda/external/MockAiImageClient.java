package com.sadamda.external;

/** 실제 AI API 연동 전까지 쓰는 가짜 합성 서비스. 인물 사진을 그대로 돌려준다. */
public class MockAiImageClient implements AiImageClient {

    @Override
    public byte[] synthesize(byte[] personImage, byte[] backgroundImage, String clothingImageUrl) {
        return personImage;
    }
}
