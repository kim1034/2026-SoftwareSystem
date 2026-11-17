package com.sadamda.model.tryon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * 기본 캐릭터 이미지를 신체 스펙(키·몸무게·체형)에 맞는 모습으로 바꾼다.
 *
 * AI 는 "160cm, 70kg" 같은 숫자를 그림에 정확히 옮기지 못하므로, 숫자는 여기서 계산으로 반영한다.
 * - 키: 화면 틀과 바닥(발 위치)은 그대로 두고 캐릭터를 키에 비례해 줄인다 (가장 큰 키 185cm 가 기본 이미지 크기).
 *       어른의 머리 크기는 키가 달라도 거의 같으므로 머리는 조금만 줄이고 몸을 더 줄인다 (키가 작을수록 등신이 줄어든다).
 * - 몸무게: 체질량지수(몸무게 ÷ 키²)가 클수록 몸 너비를 넓힌다. 허리·배가 가장 많이, 어깨·발목은 덜 넓어진다.
 * - 체형: 상체·하체 너비를 조금씩 다르게 한다.
 * 이렇게 만든 이미지를 화면에 보여주고, AI 에는 "이 몸 그대로 옷만 입혀라"고 보낸다.
 */
public final class CharacterFigure {
    /** 기본 캐릭터의 체질량지수 (네 캐릭터 모두 날씬한 편). */
    private static final double BASE_BMI = 20.0;
    /** 결과 이미지에서 발바닥(바닥)의 세로 위치 (이미지 높이 대비). 키가 달라도 발은 늘 여기에 있다. */
    private static final double FLOOR = 0.97;

    private static final Map<CharacterStyle, BufferedImage> BASES = new EnumMap<>(CharacterStyle.class);

    private CharacterFigure() {
    }

    /** 그림체의 기본 캐릭터 이미지 (올려 둔 원본, 스펙 반영 전). */
    public static synchronized BufferedImage base(CharacterStyle style) {
        return BASES.computeIfAbsent(style, key -> {
            try (InputStream in = CharacterFigure.class.getResourceAsStream(key.imageResource())) {
                if (in == null) {
                    throw new IllegalStateException("기본 캐릭터 이미지가 없습니다: " + key.imageResource());
                }
                return ImageIO.read(in);
            } catch (IOException e) {
                throw new UncheckedIOException("기본 캐릭터 이미지를 읽지 못했습니다", e);
            }
        });
    }

    /** 185cm 캐릭터의 머리끝~발끝이 화면 높이에서 차지하는 비율. 머리끝 5%, 발끝 97% 위치가 된다. */
    private static final double FULL_HEIGHT = 0.92;

    /** 키 비율: 185cm 가 1.0. 화면 속 캐릭터의 키(머리끝~발끝)가 이 비율로 줄어든다. */
    public static double heightScale(CharacterSpec spec) {
        return (double) spec.heightCm() / CharacterSpec.MAX_HEIGHT;
    }

    /** 스펙을 반영한 이미지에서 머리 꼭대기의 세로 위치 (이미지 높이 대비 0~1). 발은 FLOOR 에 고정. */
    public static double headTop(CharacterSpec spec) {
        return FLOOR - FULL_HEIGHT * heightScale(spec);
    }

    /** 몸 너비 비율: 기본 캐릭터(체질량지수 20)가 1.0. */
    public static double widthScale(CharacterSpec spec) {
        return Math.max(0.82, Math.min(1.75, Math.pow(spec.bmi() / BASE_BMI, 0.65)));
    }

    /** 신체 스펙을 반영한 캐릭터 이미지. 크기는 기본 이미지와 같다. */
    public static BufferedImage shaped(CharacterStyle style, CharacterSpec spec) {
        BufferedImage source = base(style);
        int width = source.getWidth();
        int height = source.getHeight();

        // 기본 이미지에서 캐릭터가 실제로 있는 범위 (그림체마다 조금씩 다르다)
        double[] extent = extent(style);
        double baseTop = extent[0];
        double baseFloor = extent[1];
        double baseChin = baseTop + (baseFloor - baseTop) * headShare(style);
        double normalize = FULL_HEIGHT / (baseFloor - baseTop); // 185cm 일 때 화면의 FULL_HEIGHT 를 채우게 하는 비율

        // 세로: 머리는 조금만, 몸은 나머지 전부를 줄여 전체 키가 heightScale 배가 되게 한다
        double scale = heightScale(spec);
        double headScale = normalize * Math.pow(scale, 0.6);
        double figure = FULL_HEIGHT * scale;
        double head = (baseChin - baseTop) * headScale;
        double bodyScale = (figure - head) / (baseFloor - baseChin);
        double chinAt = FLOOR - (figure - head); // 결과 이미지에서 턱의 세로 위치

        // 가로: 몸에서의 위치(머리끝 0 ~ 발끝 1)별 너비 비율
        double body = widthScale(spec);
        double upper = upperFactor(spec.bodyType());
        double lower = lowerFactor(spec.bodyType());
        double waist = 1 + Math.max(-0.03, Math.min(0.22, (spec.bmi() - 22) * 0.012));
        double face = headScale * (1 + (body - 1) * 0.5);
        double bodyBase = normalize * scale;
        double share = headShare(style);
        double[] at = {0.00, share * 0.85, share + 0.08, 0.29, 0.40, 0.51, 0.68, 0.79, 0.97, 1.00};
        double[] factor = {
                face, face,                                 // 머리
                bodyBase * Math.pow(body, 0.75) * upper,    // 어깨
                bodyBase * body * upper,                    // 가슴
                bodyBase * body * waist,                    // 허리·배
                bodyBase * body * lower,                    // 골반
                bodyBase * body * lower,                    // 허벅지
                bodyBase * Math.pow(body, 0.8),             // 무릎
                bodyBase * Math.pow(body, 0.5),             // 발목
                bodyBase * Math.pow(body, 0.5)};

        double centerX = width / 2.0;
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            double t = (double) y / height;
            double sourceT = t >= chinAt ? baseFloor - (FLOOR - t) / bodyScale : baseChin - (chinAt - t) / headScale;
            double position = (sourceT - baseTop) / (baseFloor - baseTop);
            double rowFactor = interpolate(at, factor, Math.max(0, Math.min(1, position)));
            double sourceY = sourceT * height;
            for (int x = 0; x < width; x++) {
                target.setRGB(x, y, sample(source, centerX + (x - centerX) / rowFactor, sourceY));
            }
        }
        return target;
    }

    private static final Map<CharacterStyle, double[]> EXTENTS = new EnumMap<>(CharacterStyle.class);

    /** 기본 이미지에서 캐릭터의 머리끝·발끝 세로 위치 (이미지 높이 대비). 한 번 재어 둔다. */
    private static synchronized double[] extent(CharacterStyle style) {
        return EXTENTS.computeIfAbsent(style, key -> {
            double[] found = figureExtent(base(key));
            return found == null || found[1] - found[0] < 0.5 ? new double[]{0.05, FLOOR} : found; // 재지 못하면 대략값
        });
    }

    /**
     * 단색에 가까운 배경 위에 선 캐릭터의 머리끝(모자 포함)·발끝 세로 위치 (이미지 높이 대비 0~1).
     * 캐릭터를 찾지 못하면 null.
     */
    public static double[] figureExtent(BufferedImage image) {
        int top = -1;
        int bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            if (hasFigure(image, y)) {
                if (top < 0) {
                    top = y;
                }
                bottom = y;
            }
        }
        return top < 0 ? null : new double[]{(double) top / image.getHeight(), (double) bottom / image.getHeight()};
    }

    /** 키에서 머리(머리끝~턱)가 차지하는 비율. 디즈니 캐릭터는 머리가 크다. */
    private static double headShare(CharacterStyle style) {
        return style == CharacterStyle.DISNEY ? 0.17 : 0.135;
    }

    /** 모자를 썼을 때 머리 위로 더해지는 높이 (키 대비, 대략값). */
    private static final double HAT_ALLOWANCE = 0.03;

    /**
     * AI 가 만든 결과에서 캐릭터의 화면 속 키를 스펙에 맞게 바로잡는다.
     * AI 는 "이 키 그대로"라는 지시를 가끔 어기고 캐릭터를 더 크게(길게) 그리므로, 결과 그림에서
     * 머리끝과 발끝을 찾아 전체를 같은 비율로 줄이거나 키워 발은 바닥에, 머리끝은 스펙의 높이에 오게 한다.
     * 가로·세로를 같은 비율로 바꾸므로 그림이 찌그러지지 않는다.
     * 캐릭터를 찾지 못했거나 고칠 양이 지나치게 크면(잘못 찾은 경우) 원본을 그대로 돌려준다.
     */
    public static BufferedImage fitHeight(BufferedImage result, CharacterSpec spec, boolean wearingHat) {
        int width = result.getWidth();
        int height = result.getHeight();
        double[] found = figureExtent(result);
        if (found == null) {
            return result;
        }
        double bottom = found[1] * height;
        double actual = found[1] - found[0];
        double expected = FULL_HEIGHT * heightScale(spec) * (wearingHat ? 1 + HAT_ALLOWANCE : 1);
        double ratio = expected / actual;
        if (actual < 0.4 || ratio < 0.7 || ratio > 1.3) {
            return result; // 캐릭터를 잘못 찾았을 가능성이 크다
        }
        double floorY = FLOOR * height;
        if (Math.abs(ratio - 1) < 0.015 && Math.abs(bottom - floorY) < height * 0.015) {
            return result; // 이미 맞다
        }
        double centerX = width / 2.0;
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            double sourceY = bottom + (y - floorY) / ratio;
            for (int x = 0; x < width; x++) {
                target.setRGB(x, y, sample(result, centerX + (x - centerX) / ratio, sourceY));
            }
        }
        return target;
    }

    /** 그 줄에 배경이 아닌 것(캐릭터)이 있는지. 배경색은 그 줄의 양 끝에서 얻어, 위아래로 밝기가 변하는 배경도 견딘다. */
    private static boolean hasFigure(BufferedImage image, int y) {
        int width = image.getWidth();
        int edge = Math.max(4, width / 60);
        long[] sum = new long[3];
        for (int i = 0; i < edge; i++) {
            for (int x : new int[]{i, width - 1 - i}) {
                int rgb = image.getRGB(x, y);
                sum[0] += (rgb >> 16) & 0xFF;
                sum[1] += (rgb >> 8) & 0xFF;
                sum[2] += rgb & 0xFF;
            }
        }
        int r = (int) (sum[0] / (edge * 2));
        int g = (int) (sum[1] / (edge * 2));
        int b = (int) (sum[2] / (edge * 2));
        int different = 0;
        for (int x = edge; x < width - edge; x++) {
            int rgb = image.getRGB(x, y);
            int diff = Math.max(Math.abs(((rgb >> 16) & 0xFF) - r),
                    Math.max(Math.abs(((rgb >> 8) & 0xFF) - g), Math.abs((rgb & 0xFF) - b)));
            if (diff > 28 && ++different >= Math.max(6, width / 100)) {
                return true;
            }
        }
        return false;
    }

    private static double upperFactor(String bodyType) {
        return switch (bodyType) {
            case "마른 체형" -> 0.96;
            case "통통한 체형" -> 1.05;
            case "상체 발달형" -> 1.08;
            case "하체 발달형" -> 0.97;
            default -> 1.0;
        };
    }

    private static double lowerFactor(String bodyType) {
        return switch (bodyType) {
            case "마른 체형" -> 0.96;
            case "통통한 체형" -> 1.05;
            case "상체 발달형" -> 0.97;
            case "하체 발달형" -> 1.10;
            default -> 1.0;
        };
    }

    private static double interpolate(double[] at, double[] value, double t) {
        if (t <= at[0]) {
            return value[0];
        }
        for (int i = 1; i < at.length; i++) {
            if (t <= at[i]) {
                double ratio = (t - at[i - 1]) / (at[i] - at[i - 1]);
                return value[i - 1] + (value[i] - value[i - 1]) * ratio;
            }
        }
        return value[value.length - 1];
    }

    /** 두 방향 선형 보간. 이미지 밖은 가장자리 색(배경)을 이어 쓴다. */
    private static int sample(BufferedImage image, double x, double y) {
        int maxX = image.getWidth() - 1;
        int maxY = image.getHeight() - 1;
        x = Math.max(0, Math.min(maxX, x));
        y = Math.max(0, Math.min(maxY, y));
        int x0 = (int) x;
        int y0 = (int) y;
        int x1 = Math.min(maxX, x0 + 1);
        int y1 = Math.min(maxY, y0 + 1);
        double fx = x - x0;
        double fy = y - y0;
        int a = image.getRGB(x0, y0);
        int b = image.getRGB(x1, y0);
        int c = image.getRGB(x0, y1);
        int d = image.getRGB(x1, y1);
        int result = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            double top = ((a >> shift) & 0xFF) * (1 - fx) + ((b >> shift) & 0xFF) * fx;
            double bottom = ((c >> shift) & 0xFF) * (1 - fx) + ((d >> shift) & 0xFF) * fx;
            result |= ((int) Math.round(top * (1 - fy) + bottom * fy)) << shift;
        }
        return 0xFF000000 | result;
    }
}
