package com.sadamda.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.List;

/**
 * 상단 왼쪽의 [사담다] 로고 (누르면 홈으로). 40~50대 여성 고객에게 맞게 차분하고 단정한 명조 글자로 쓴다.
 * 글자는 명조 계열 글꼴(나눔명조·애플명조·바탕 등)을 찾아 쓰고, 하나도 없으면 앱 기본 글꼴의 굵은 글씨로 대신한다.
 * 글씨 크기 설정과 상관없이 생성할 때 받은 크기로 고정된다.
 */
public final class SadamdaLogo extends JButton {
    private static final String NAME = "사담다";
    /** 앞에 있을수록 먼저 쓴다. 컴퓨터에 설치된 첫 글꼴을 고른다. */
    private static final List<String> SERIF_FAMILIES = Arrays.asList(
            "Nanum Myeongjo", "AppleMyungjo", "PCMyungjo", "Batang", "Noto Serif CJK KR", "Noto Serif KR");
    /** 글자 윤곽을 덧그려 조금 더 굵게 하는 두께(px). 명조는 가늘어서 작은 크기에서도 또렷하게 한다. */
    private static final float WEIGHT = 0.8f;
    /** 글자 사이를 벌리는 정도 (글자 크기 대비). */
    private static final float SPACING = 0.065f;
    /** 누르는 칸의 여백. 높이가 다른 메뉴처럼 48px 가까이 되게 한다 (NFR-02). */
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 12;

    /** 글자 뒤 골드 점의 지름과 글자와의 간격 (글자 크기 대비). */
    private static final float DOT = 0.2f;
    private static final float DOT_GAP = 0.08f;

    private static String serifFamily; // 한 번만 찾는다
    private static boolean serifSearched;

    private final float size;

    /** size: 글자 크기(px). */
    public SadamdaLogo(float size) {
        super(NAME); // 글자는 직접 그리지만 화면 읽기 프로그램을 위해 이름을 남긴다
        this.size = size;
        setContentAreaFilled(false);
        setBorder(BorderFactory.createEmptyBorder());
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(Theme.PRIMARY);
        getAccessibleContext().setAccessibleName("사담다, 홈으로");
    }

    /** 설치된 명조 글꼴 이름. 없으면 null. */
    private static synchronized String serifFamily() {
        if (!serifSearched) {
            serifSearched = true;
            List<String> installed = Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String family : SERIF_FAMILIES) {
                if (installed.contains(family) && new Font(family, Font.PLAIN, 20).canDisplayUpTo(NAME) == -1) {
                    serifFamily = family;
                    break;
                }
            }
        }
        return serifFamily;
    }

    private Font font() {
        String family = serifFamily();
        Font base = family == null ? UIManager.getFont("Button.font") : new Font(family, Font.BOLD, 20);
        return base.deriveFont(Font.BOLD, size);
    }

    /** 글자 사이를 조금 벌려 놓은 글자 윤곽. */
    private Path2D.Float name() {
        Font font = font();
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Path2D.Float path = new Path2D.Float();
        float x = 0;
        for (char c : NAME.toCharArray()) {
            TextLayout layout = new TextLayout(String.valueOf(c), font, frc);
            path.append(layout.getOutline(AffineTransform.getTranslateInstance(x, 0)), false);
            x += layout.getAdvance() + size * SPACING;
        }
        return path;
    }

    private float dotSpace() {
        return size * (DOT + DOT_GAP);
    }

    @Override
    public Dimension getPreferredSize() {
        Rectangle2D bounds = name().getBounds2D();
        return new Dimension((int) Math.ceil(bounds.getWidth() + dotSpace()) + PADDING_X * 2,
                (int) Math.ceil(bounds.getHeight()) + PADDING_Y * 2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Path2D.Float name = name();
        Rectangle2D bounds = name.getBounds2D();
        Graphics2D text = (Graphics2D) g2.create();
        text.setColor(Theme.PRIMARY);
        text.translate((getWidth() - bounds.getWidth() - dotSpace()) / 2 - bounds.getX(),
                (getHeight() - bounds.getHeight()) / 2 - bounds.getY());
        text.fill(name);
        text.setStroke(new BasicStroke(WEIGHT, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        text.draw(name);
        // 글자 오른쪽 아래의 골드 점
        float dot = size * DOT;
        text.setColor(Theme.GOLD);
        text.fill(new java.awt.geom.Ellipse2D.Double(bounds.getMaxX() + size * DOT_GAP,
                bounds.getMaxY() - dot, dot, dot));
        text.dispose();

        if (isFocusOwner()) { // 키보드 사용자를 위한 테두리
            g2.setColor(Theme.PRIMARY);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 10, 10));
        }
        g2.dispose();
    }
}
