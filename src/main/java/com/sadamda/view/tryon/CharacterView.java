package com.sadamda.view.tryon;

import com.sadamda.view.Theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * 옷 입혀보기 화면 왼쪽의 큰 그림 칸. 모서리가 둥글고, 그림을 비율에 맞춰 가운데 크게 보여준다.
 *
 * 세로 그림이라 좌우에 남는 여백은 비워 두지 않고 채운다.
 * - 내 사진에 배경을 골랐으면(scene) 그 배경 사진을 칸 전체에 살짝 흐리게 깐다 → 고른 장소가 미리 보인다
 * - 그 밖에는 보여주는 그림 자체를 크게 늘려 많이 흐리게 깐다 → 캐릭터의 스튜디오 배경, 합성 결과의 장소가
 *   좌우로 자연스럽게 이어진다
 * 누르면 크게 보기 창이 열린다 (TryOnPanel 이 연결).
 */
class CharacterView extends JPanel {
    private static final int ARC = 80;
    /** 흐리게 만들 때 줄이는 폭(px). 작을수록 많이 흐리다. */
    private static final int SELF_BLUR_WIDTH = 14;
    private static final int SCENE_BLUR_WIDTH = 40;

    private BufferedImage image;
    private BufferedImage scene;
    private BufferedImage blurredImage;
    private BufferedImage blurredScene;
    private String message = "";

    CharacterView() {
        setOpaque(false);
        setPreferredSize(new Dimension(480, 640));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("눌러서 크게 보기");
    }

    /** 보여줄 그림. null 이면 안내 문구만 보인다. */
    void setImage(BufferedImage image) {
        this.image = image;
        this.blurredImage = image == null ? null : shrink(image, SELF_BLUR_WIDTH);
        repaint();
    }

    BufferedImage image() {
        return image;
    }

    /** 좌우 여백에 깔 장소 사진 (내 사진에 고른 배경). null 이면 그림 자체를 흐리게 깐다. */
    void setScene(BufferedImage scene) {
        if (scene != this.scene) {
            this.scene = scene;
            this.blurredScene = scene == null ? null : shrink(scene, SCENE_BLUR_WIDTH);
            repaint();
        }
    }

    /** 그림이 없을 때 가운데에 보여줄 안내 문구. */
    void setMessage(String message) {
        this.message = message == null ? "" : message;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        RoundRectangle2D frame = new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 4, ARC, ARC);
        g.setColor(Color.WHITE);
        g.fill(frame);

        if (image != null) {
            Graphics2D clipped = (Graphics2D) g.create();
            clipped.clip(frame);
            clipped.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // 좌우 여백 채우기: 작게 줄인 그림을 칸 전체로 늘리면 부드럽게 흐려진다
            BufferedImage backdrop = blurredScene != null ? blurredScene : blurredImage;
            TryOnUi.drawCover(clipped, backdrop, 2, 2, getWidth() - 4, getHeight() - 4, 0.5);
            if (blurredScene == null) {
                clipped.setColor(new Color(255, 255, 255, 50)); // 그림과 겹쳐 보이지 않게 조금 밝게
                clipped.fill(frame);
            }

            double scale = Math.min((double) (getWidth() - 4) / image.getWidth(),
                    (double) (getHeight() - 4) / image.getHeight());
            int width = (int) Math.round(image.getWidth() * scale);
            int height = (int) Math.round(image.getHeight() * scale);
            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;
            // 그림 옆에 옅은 그림자를 두어 앞의 그림과 뒤의 여백이 구분되게 한다
            for (int i = 1; i <= 6; i++) {
                clipped.setColor(new Color(0, 0, 0, 7));
                clipped.fillRect(x - i, y, width + i * 2, height);
            }
            clipped.drawImage(image, x, y, width, height, null);
            clipped.dispose();
        } else {
            g.setColor(new Color(70, 70, 70)); // 흰 바탕과 명도 대비 4.5:1 이상 (NFR-01)
            g.setFont(getFont());
            FontMetrics metrics = g.getFontMetrics();
            String[] lines = message.split("\n");
            int y = getHeight() / 2 - (lines.length - 1) * metrics.getHeight() / 2;
            for (String line : lines) {
                g.drawString(line, (getWidth() - metrics.stringWidth(line)) / 2, y);
                y += metrics.getHeight();
            }
        }

        g.setColor(Theme.LINE); // 다른 카드와 같은 연한 테두리
        g.setStroke(new BasicStroke(1));
        g.draw(frame);
        g.dispose();
    }

    /**
     * 흐리게 만든 그림: 폭 width 로 단계적으로 줄인 다음 8배로 다시 늘린다.
     * 작은 그림을 화면 크기로 한 번에 늘리면 네모난 얼룩이 보여서, 중간 크기로 한 번 부드럽게 늘려 둔다.
     */
    static BufferedImage shrink(BufferedImage source, int width) {
        BufferedImage current = source;
        while (current.getWidth() / 2 >= width) {
            current = scale(current, current.getWidth() / 2);
        }
        BufferedImage small = scale(current, width);
        BufferedImage medium = scale(small, width * 2);
        return scale(medium, width * 8);
    }

    private static BufferedImage scale(BufferedImage source, int width) {
        int height = Math.max(1, (int) Math.round((double) source.getHeight() * width / source.getWidth()));
        BufferedImage target = new BufferedImage(Math.max(1, width), height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, target.getWidth(), height, null);
        g.dispose();
        return target;
    }
}
