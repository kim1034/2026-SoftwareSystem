package com.sadamda.view;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * UC-15 글씨 크기 조절 (FR-24). 본문 최소 18px (NFR-01).
 * TODO: 회원이면 계정에 저장 (UC-15 4단계). 지금은 이 PC 에만 저장한다.
 */
public enum FontSize {
    NORMAL("보통", 18), LARGE("크게", 22), XLARGE("아주 크게", 26);

    private static final Preferences PREFS = Preferences.userNodeForPackage(FontSize.class);
    private static final String KEY = "fontSize";

    private final String label;
    private final int px;

    FontSize(String label, int px) {
        this.label = label;
        this.px = px;
    }

    @Override
    public String toString() { // 선택 대화상자에 표시되는 이름
        return label;
    }

    public static FontSize load() {
        return FontSize.valueOf(PREFS.get(KEY, NORMAL.name()));
    }

    /** 모든 Swing 기본 글꼴 크기를 바꾸고 설정을 저장한다. */
    public void apply() {
        // 키를 먼저 복사한다. UIManager.get 이 지연 값을 풀면서 테이블을 수정하기 때문.
        List<Object> keys = new ArrayList<>(UIManager.getLookAndFeelDefaults().keySet());
        for (Object key : keys) {
            if (UIManager.get(key) instanceof FontUIResource font) {
                UIManager.put(key, new FontUIResource(font.deriveFont((float) px)));
            }
        }
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
        PREFS.put(KEY, name());
    }
}
