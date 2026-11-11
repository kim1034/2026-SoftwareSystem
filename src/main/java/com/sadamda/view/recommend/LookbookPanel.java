package com.sadamda.view.recommend;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;

/** 상황·계절별 코디 목록, 코디 전체 담기 */
public class LookbookPanel extends ScreenPanel {
    public static final String NAME = "lookbook";

    public LookbookPanel(AppContext context, MainFrame frame) {
        super(context, frame, "룩북", "UC-07 룩북 코디 추천 보기");
    }
}
