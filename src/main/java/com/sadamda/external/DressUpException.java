package com.sadamda.external;

/**
 * 옷 입혀보기 합성이 실패했을 때 (시간 초과·연결 실패·결과 없음). 화면은 메시지를 보여 주고 [다시 시도]를 안내한다.
 * 사진으로 입어보기의 DressUpException 과 일부러 나눴다.
 * 그 예외는 Controller 담당(feat/tryon)이 생성자를 바꾸고 있어서, 같이 쓰면 브랜치를 합칠 때 컴파일 오류가 난다.
 */
public class DressUpException extends Exception {
    public DressUpException(String message) {
        super(message);
    }
}
