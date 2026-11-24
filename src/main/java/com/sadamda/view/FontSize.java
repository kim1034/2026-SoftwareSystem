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
 * 상단 [글씨 크기] 손잡이를 좌우로 당기면 1px 씩, 누르면 보통·크게·아주 크게 중에서 고른다.
 * 이 PC 에 저장하고, 회원이면 계정에도 저장한다 (UC-15 4단계, MainFrame.applyAccountFontSize).
 */
public enum FontSize {
    NORMAL("보통", 18), LARGE("크게", 22), XLARGE("아주 크게", 26);

    /** 본문 글씨 크기 범위(px). 18px 보다 작게는 줄이지 않는다 (NFR-01). */
    public static final int MIN_PX = 18;
    public static final int MAX_PX = 32;

    private static final Preferences PREFS = Preferences.userNodeForPackage(FontSize.class);
    private static final String KEY = "fontSize";
    private static final String PX_KEY = "fontPx";

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

    /** 본문 글씨 크기(px). 글씨 크기와 상관없이 고정하는 곳(상단 메뉴 등)은 NORMAL.px() 를 쓴다. */
    public int px() {
        return px;
    }

    /** 지금 크기에 가장 가까운 단계 (선택 대화상자의 처음 값). */
    public static FontSize load() {
        int current = currentPx();
        FontSize nearest = NORMAL;
        for (FontSize size : values()) {
            if (Math.abs(size.px - current) < Math.abs(nearest.px - current)) {
                nearest = size;
            }
        }
        return nearest;
    }

    /** 이 PC 에 저장해 둔 본문 글씨 크기(px). 예전 버전이 저장한 단계 이름("LARGE")도 읽는다. */
    public static int currentPx() {
        int px = PREFS.getInt(PX_KEY, -1);
        if (px < 0) {
            px = parse(PREFS.get(KEY, NORMAL.name()));
        }
        return px < 0 ? NORMAL.px : clamp(px);
    }

    /** 계정·PC 에 저장된 값("21" 또는 단계 이름 "LARGE")을 px 로 읽는다. 읽을 수 없으면 -1. */
    public static int parse(String saved) {
        if (saved == null || saved.isBlank()) {
            return -1;
        }
        try {
            return clamp(Integer.parseInt(saved.trim()));
        } catch (NumberFormatException e) {
            for (FontSize size : values()) {
                if (size.name().equals(saved)) {
                    return size.px;
                }
            }
            return -1;
        }
    }

    public static int clamp(int px) {
        return Math.max(MIN_PX, Math.min(MAX_PX, px));
    }

    /** 이 단계 크기로 바꾼다. */
    public void apply() {
        applyPx(px);
    }

    /** 모든 Swing 기본 글꼴 크기를 px 로 바꾸고 설정을 저장한다. 범위를 넘으면 끝 값으로 맞춘다. */
    public static void applyPx(int px) {
        float size = clamp(px);
        // 키를 먼저 복사한다. UIManager.get 이 지연 값을 풀면서 테이블을 수정하기 때문.
        List<Object> keys = new ArrayList<>(UIManager.getLookAndFeelDefaults().keySet());
        for (Object key : keys) {
            if (UIManager.get(key) instanceof FontUIResource font) {
                UIManager.put(key, new FontUIResource(font.deriveFont(size)));
            }
        }
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
        PREFS.putInt(PX_KEY, clamp(px));
    }
}
