package com.sadamda.model.tryon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 배경 사진 목록. resources/data/backgrounds.tsv 에서 읽는다.
 * 원본은 팀 구글 시트 "background" 이고, 앱에는 버튼에 쓸 짧은 이름 열을 더했다.
 */
public class BackgroundCatalog {
    public static final String RESOURCE = "/data/backgrounds.tsv";
    public static final List<String> SEASONS = List.of("봄", "여름", "가을", "겨울");

    private final List<Background> backgrounds;

    public BackgroundCatalog() {
        this(load());
    }

    public BackgroundCatalog(List<Background> backgrounds) {
        this.backgrounds = List.copyOf(backgrounds);
    }

    /** 전체 배경 (계절 순: 봄 → 겨울). */
    public List<Background> all() {
        return backgrounds;
    }

    /** 그 계절의 배경. season 이 null 이면 전체. */
    public List<Background> bySeason(String season) {
        if (season == null) {
            return backgrounds;
        }
        List<Background> found = new ArrayList<>();
        for (Background background : backgrounds) {
            if (background.season().equals(season)) {
                found.add(background);
            }
        }
        return found;
    }

    /** id 로 찾는다. 없으면 null. */
    public Background find(String id) {
        for (Background background : backgrounds) {
            if (background.id().equals(id)) {
                return background;
            }
        }
        return null;
    }

    private static List<Background> load() {
        InputStream in = BackgroundCatalog.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            return List.of(); // 데이터가 없으면 "원래 배경 그대로"만 쓸 수 있다
        }
        List<Background> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] cols = line.split("\t", -1);
                if (cols.length < 8) {
                    continue;
                }
                loaded.add(new Background(cols[0].trim(), cols[1].trim(), cols[2].trim(), cols[3].trim(),
                        cols[4].trim(), cols[5].trim(), cols[6].trim(), cols[7].trim()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        loaded.sort((a, b) -> Integer.compare(SEASONS.indexOf(a.season()), SEASONS.indexOf(b.season())));
        return loaded;
    }
}
