package com.sadamda.view;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 앱 전체 글꼴을 원티드 산스(Wanted Sans, SIL OFL 1.1)로 바꾼다.
 * src/main/resources/fonts 에 넣어 둔 글꼴 파일을 먼저 쓰고, 없으면 PC 에 설치된 원티드 산스를 쓴다.
 * 둘 다 없으면 기본 글꼴을 그대로 둔다. 크기는 [글씨 크기](FontSize)가 정한다.
 */
public final class AppFonts {
    /** 보통 글씨와 굵은 글씨. Swing 은 이 두 굵기만 고른다. */
    private static final String[] FILES = {
            "/fonts/WantedSans-Regular.ttf", "/fonts/WantedSans-Bold.ttf",
            "/fonts/WantedSans-Regular.otf", "/fonts/WantedSans-Bold.otf"};
    private static final String INSTALLED_FAMILY = "Wanted Sans";

    private static boolean installed;

    private AppFonts() {
    }

    /** 모든 Swing 기본 글꼴의 종류를 원티드 산스로 바꾼다. 크기·굵기는 그대로 둔다. 한 번만 한다. */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        String family = registerBundled();
        if (family == null && isInstalled(INSTALLED_FAMILY)) {
            family = INSTALLED_FAMILY;
        }
        if (family == null) {
            return; // 글꼴이 없으면 기본 글꼴
        }
        // 키를 먼저 복사한다. UIManager.get 이 지연 값을 풀면서 테이블을 수정하기 때문.
        List<Object> keys = new ArrayList<>(UIManager.getLookAndFeelDefaults().keySet());
        for (Object key : keys) {
            if (UIManager.get(key) instanceof FontUIResource font) {
                UIManager.put(key, new FontUIResource(family, font.getStyle(), font.getSize()));
            }
        }
    }

    /** 앱에 넣어 둔 글꼴 파일을 등록하고 그 이름을 돌려준다. 하나도 없으면 null. */
    private static String registerBundled() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String family = null;
        for (String file : FILES) {
            try (InputStream in = AppFonts.class.getResourceAsStream(file)) {
                if (in == null) {
                    continue;
                }
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                environment.registerFont(font);
                if (family == null) {
                    family = font.getFamily();
                }
            } catch (IOException | FontFormatException e) {
                // 읽을 수 없는 파일은 건너뛴다
            }
        }
        return family;
    }

    private static boolean isInstalled(String family) {
        return Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                .contains(family);
    }
}
