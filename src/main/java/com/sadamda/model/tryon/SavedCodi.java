package com.sadamda.model.tryon;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 내 코디북에 저장한 코디 한 벌 (UC-19). 담당: 정우진
 * 입힌 상품은 상품 id 로만 기억한다. 다시 열 때 지금 상품 정보(가격·판매 여부)로 되살린다.
 *
 * @param productIds 자리별 상품 id (모자 → 신발 순)
 * @param imagePath  입혀본 그림 파일. [입혀보기] 전에 저장했으면 null
 */
public record SavedCodi(long id, String title, CharacterStyle style, CharacterSpec spec,
                        Map<DressUpSlot, Long> productIds, String imagePath, LocalDateTime savedAt) {
    public SavedCodi {
        EnumMap<DressUpSlot, Long> copy = new EnumMap<>(DressUpSlot.class);
        if (productIds != null) {
            copy.putAll(productIds);
        }
        productIds = Collections.unmodifiableMap(copy);
    }

    public boolean hasImage() {
        return imagePath != null;
    }
}
