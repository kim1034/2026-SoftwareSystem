package com.sadamda.view.tryon;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 사진 이용 동의 → 인물 사진 업로드 → 배경 선택 → 결과 보기 */
public class TryOnPanel extends ScreenPanel {
    public static final String NAME = "tryOn";

    public TryOnPanel(AppContext context, MainFrame frame) {
        super(context, frame, "AI 입어보기", "UC-09 AI 착용 이미지 생성");
    }
}
