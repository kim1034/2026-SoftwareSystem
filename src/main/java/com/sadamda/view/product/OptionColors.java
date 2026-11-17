package com.sadamda.view.product;

import java.awt.Color;
import java.util.Map;

/**
 * 색상 옵션 이름("다크 브라운", "차콜 블랙")을 화면에 칠할 색으로 바꾼다. 상품 상세의 색상 칩을 고르면 이 색으로 채운다.
 * 이름에서 가장 앞에 나오는 색 낱말을 쓴다 ("차콜 블랙" → 차콜). 모르는 이름이면 null.
 */
final class OptionColors {
    private static final Map<String, Color> WORDS = Map.ofEntries(
            Map.entry("블랙", new Color(0x1F1F1F)), Map.entry("화이트", new Color(0xFAFAFA)),
            Map.entry("오프화이트", new Color(0xF4F1EA)), Map.entry("아이보리", new Color(0xF3EDDC)),
            Map.entry("크림", new Color(0xF2E8D0)), Map.entry("에크루", new Color(0xEDE3CF)),
            Map.entry("오트밀", new Color(0xDCCFB8)), Map.entry("베이지", new Color(0xD9C4A3)),
            Map.entry("누드", new Color(0xE0BFA4)), Map.entry("샌드", new Color(0xD6C29E)),
            Map.entry("토프", new Color(0x8B7D6B)), Map.entry("카멜", new Color(0xB5835A)),
            Map.entry("카라멜", new Color(0xAF6E3D)), Map.entry("머스타드", new Color(0xC9A227)),
            Map.entry("브라운", new Color(0x6B4A33)), Map.entry("다크브라운", new Color(0x4A3324)),
            Map.entry("초콜릿", new Color(0x4E3426)), Map.entry("에스프레소", new Color(0x3E2B22)),
            Map.entry("커피", new Color(0x5A3E2E)), Map.entry("타바코", new Color(0x6E4B2E)),
            Map.entry("로즈우드", new Color(0x65413A)), Map.entry("오크", new Color(0x8A6440)),
            Map.entry("네이비", new Color(0x1F2A44)), Map.entry("인디고", new Color(0x2E3A6B)),
            Map.entry("블루", new Color(0x2F5D9E)), Map.entry("터콰이즈", new Color(0x3AA3A0)),
            Map.entry("그레이", new Color(0x8A8A8A)), Map.entry("콘크리트", new Color(0x9A9A96)),
            Map.entry("차콜", new Color(0x3C3C3C)), Map.entry("실버", new Color(0xC0C0C0)),
            Map.entry("플래티넘", new Color(0xD5D5D2)), Map.entry("골드", new Color(0xC9A646)),
            Map.entry("올리브", new Color(0x6B6B3A)), Map.entry("카키", new Color(0x7A7550)),
            Map.entry("그린", new Color(0x3F6B4A)), Map.entry("버건디", new Color(0x6D1F2E)),
            Map.entry("와인", new Color(0x6A1E33)), Map.entry("레드", new Color(0xB3261E)),
            Map.entry("핑크", new Color(0xE8B4BC)), Map.entry("퍼플", new Color(0x4B2E63)),
            Map.entry("옐로우", new Color(0xE6C84A)));

    private OptionColors() {
    }

    static Color of(String name) {
        String best = null;
        int first = Integer.MAX_VALUE;
        for (String word : WORDS.keySet()) {
            int at = name.indexOf(word);
            // 같은 자리에서 시작하면 긴 낱말을 쓴다 ("다크브라운"이 "브라운"보다, "카라멜"이 "카멜"보다 먼저)
            if (at >= 0 && (at < first || at == first && word.length() > best.length())) {
                first = at;
                best = word;
            }
        }
        return best == null ? null : WORDS.get(best);
    }
}
