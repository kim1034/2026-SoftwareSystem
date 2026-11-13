package com.sadamda.external;

/**
 * AI 서비스의 이용 열쇠(API 키)가 없을 때 쓰는 자리 채움. 외부로 아무것도 보내지 않는다.
 * 상품 사진을 캐릭터 위에 붙이는 식의 가짜 합성은 하지 않고, 연결이 필요하다고만 알린다.
 * (화면은 이때에도 신체 스펙이 반영된 기본 캐릭터를 보여주고, 상품 탐색 등 다른 기능은 그대로 쓸 수 있다: NFR-09)
 */
public class UnconnectedDressUpClient implements DressUpImageClient {

    @Override
    public String name() {
        return "연결 안 됨";
    }

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public byte[] dressUp(Request request) throws AiImageClient.AiImageException {
        throw new AiImageClient.AiImageException(
                "그림 만드는 곳(AI)에 아직 연결되지 않았습니다. [AI 연결 설정]에서 이용 열쇠를 넣어 주세요.");
    }
}
