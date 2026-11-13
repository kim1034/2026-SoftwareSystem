package com.sadamda.external;

import com.sadamda.model.product.Product;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * AI 서비스의 이용 열쇠(API 키)가 없을 때 쓰는 미리보기 합성기. 외부로 아무것도 보내지 않는다.
 * 간단한 캐릭터 그림 위에 고른 상품 사진을 자리별로 붙여 보여준다 (종이 인형 방식).
 * 실제 AI 합성 결과가 아니므로 그림 아래에 "미리보기"라고 적는다.
 */
public class LocalDressUpClient implements DressUpImageClient {
    private static final int WIDTH = 768;
    private static final int HEIGHT = 1024;

    @Override
    public String name() {
        return "미리보기 (AI 연결 전)";
    }

    @Override
    public byte[] dressUp(Request request) {
        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Color[] palette = palette(request.style());
        g.setPaint(new GradientPaint(0, 0, palette[0], 0, HEIGHT, Color.WHITE));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawCharacter(g, request, palette);

        // 자리별 상품 사진: {가운데 x, 위 y, 너비, 높이}
        drawGarment(g, request, DressUpSlot.TOP, 384, 300, 250, 250);
        drawGarment(g, request, DressUpSlot.BOTTOM, 384, 560, 230, 290);
        drawGarment(g, request, DressUpSlot.SHOES, 384, 860, 220, 110);
        drawGarment(g, request, DressUpSlot.HAT, 384, 30, 180, 112);
        drawGarment(g, request, DressUpSlot.JEWELRY, 620, 230, 130, 130);

        g.setColor(new Color(60, 60, 60));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        String caption = "미리보기 · " + request.style().label() + " · " + request.spec().summary();
        g.drawString(caption, (WIDTH - g.getFontMetrics().stringWidth(caption)) / 2, HEIGHT - 20);
        g.dispose();
        return RemoteImages.toPng(canvas);
    }

    private static Color[] palette(CharacterStyle style) {
        return switch (style) { // {배경, 피부, 머리카락}
            case REAL -> new Color[]{new Color(232, 232, 228), new Color(238, 205, 180), new Color(70, 50, 40)};
            case DISNEY -> new Color[]{new Color(214, 232, 250), new Color(250, 218, 196), new Color(150, 80, 40)};
            case GAME_3D -> new Color[]{new Color(222, 218, 240), new Color(232, 196, 170), new Color(60, 60, 90)};
            case ANIME -> new Color[]{new Color(252, 224, 232), new Color(254, 226, 208), new Color(110, 70, 120)};
        };
    }

    /** 체형에 따라 어깨·허리·골반 너비를, 키에 따라 전체 길이를 조금 바꾼 단순한 여성 캐릭터. */
    private static void drawCharacter(Graphics2D g, Request request, Color[] palette) {
        String body = request.spec().bodyType();
        int shoulder = switch (body) { case "마른형" -> 84; case "통통형" -> 112; case "상체발달형" -> 116; default -> 96; };
        int waist = switch (body) { case "마른형" -> 58; case "통통형" -> 98; case "상체발달형" -> 80; default -> 72; };
        int hip = switch (body) { case "마른형" -> 80; case "통통형" -> 118; case "하체발달형" -> 124; default -> 100; };
        int cx = WIDTH / 2;
        // 키 140~185cm → 다리 길이 300~360
        int leg = 300 + (request.spec().heightCm() - 140) * 60 / 45;
        int hipY = 560;
        int footY = Math.min(hipY + leg, 940);

        Color skin = palette[1];
        Color hair = palette[2];

        g.setColor(hair); // 뒷머리
        g.fillRoundRect(cx - 78, 150, 156, 190, 120, 120);
        g.setColor(skin);
        g.fillRect(cx - 20, 270, 40, 50);                                   // 목
        int[] tx = {cx - shoulder, cx + shoulder, cx + waist, cx + hip, cx - hip, cx - waist};
        int[] ty = {320, 320, 460, hipY, hipY, 460};
        g.fillPolygon(tx, ty, 6);                                          // 몸통
        g.setStroke(new BasicStroke(34, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx - shoulder, 335, cx - shoulder - 40, 560);           // 팔
        g.drawLine(cx + shoulder, 335, cx + shoulder + 40, 560);
        g.setStroke(new BasicStroke(Math.max(40, hip - 50), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx - hip / 2, hipY, cx - 46, footY);                    // 다리
        g.drawLine(cx + hip / 2, hipY, cx + 46, footY);
        g.fillOval(cx - 62, 160, 124, 136);                                // 얼굴
        g.setColor(hair);
        g.fillArc(cx - 68, 150, 136, 110, 0, 180);                         // 앞머리
        g.setColor(new Color(60, 40, 40));
        g.fillOval(cx - 30, 222, 12, 14);                                  // 눈
        g.fillOval(cx + 18, 222, 12, 14);
        g.setColor(new Color(214, 96, 110));
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 16, 246, 32, 22, 200, 140);                         // 입
    }

    private static void drawGarment(Graphics2D g, Request request, DressUpSlot slot, int cx, int top, int width, int height) {
        Product product = request.outfit().get(slot);
        if (product == null) {
            return;
        }
        int x = cx - width / 2;
        RoundRectangle2D card = new RoundRectangle2D.Double(x, top, width, height, 28, 28);
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new RoundRectangle2D.Double(x + 4, top + 6, width, height, 28, 28));
        g.setColor(Color.WHITE);
        g.fill(card);

        BufferedImage photo = RemoteImages.decode(request.garmentImages().get(slot));
        if (photo != null) {
            // 자리를 꽉 채우도록 가운데를 잘라 그린다
            double scale = Math.max((double) width / photo.getWidth(), (double) height / photo.getHeight());
            int w = (int) Math.ceil(photo.getWidth() * scale);
            int h = (int) Math.ceil(photo.getHeight() * scale);
            Graphics2D clipped = (Graphics2D) g.create();
            clipped.setClip(card);
            clipped.drawImage(photo, x + (width - w) / 2, top + (height - h) / 2, w, h, null);
            clipped.dispose();
        } else {
            g.setColor(new Color(90, 90, 90));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            FontMetrics metrics = g.getFontMetrics();
            String text = slot.label() + " 사진 없음";
            g.drawString(text, cx - metrics.stringWidth(text) / 2, top + height / 2);
        }
        g.setColor(new Color(120, 120, 120));
        g.setStroke(new BasicStroke(2));
        g.draw(card);

        // 자리 이름표
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        FontMetrics metrics = g.getFontMetrics();
        int tagWidth = metrics.stringWidth(slot.label()) + 20;
        g.setColor(new Color(40, 40, 40, 200));
        g.fillRoundRect(x + 8, top + 8, tagWidth, 28, 14, 14);
        g.setColor(Color.WHITE);
        g.drawString(slot.label(), x + 18, top + 29);
    }
}
