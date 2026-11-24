package com.sadamda.model.tryon;

import com.sadamda.common.AppException;
import com.sadamda.model.product.Product;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 옷 입혀보기에서 입힌 한 벌. 자리마다 상품 1개. */
public class Outfit {
    private final Map<DressUpSlot, Product> items = new EnumMap<>(DressUpSlot.class);

    /**
     * 자리에 상품을 입힌다. 그 자리에 맞지 않는 상품이면 예외.
     * 같이 입을 수 없는 자리(원피스 ↔ 상의·하의)는 벗기고, 벗긴 자리를 돌려준다. 화면이 안내에 쓴다.
     */
    public List<DressUpSlot> put(DressUpSlot slot, Product product) {
        if (!slot.accepts(product)) {
            throw new AppException(slot.label() + " 자리에는 입힐 수 없는 상품입니다.");
        }
        List<DressUpSlot> removed = new ArrayList<>();
        for (DressUpSlot other : slot.conflicts()) {
            if (items.remove(other) != null) {
                removed.add(other);
            }
        }
        items.put(slot, product);
        return removed;
    }

    public void remove(DressUpSlot slot) {
        items.remove(slot);
    }

    public void clear() {
        items.clear();
    }

    /** 그 자리에 입힌 상품. 없으면 null. */
    public Product get(DressUpSlot slot) {
        return items.get(slot);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    /** 입힌 상품 전체 (자리 순서: 상의 → 가방). */
    public List<Product> products() {
        return new ArrayList<>(items.values());
    }

    public int totalPrice() {
        return items.values().stream().mapToInt(Product::getPrice).sum();
    }

    public Outfit copy() {
        Outfit copy = new Outfit();
        copy.items.putAll(items);
        return copy;
    }

    /** 다른 한 벌의 내용으로 바꾼다 (아이템 고르기 창에서 고른 것을 화면에 반영할 때). */
    public void replaceWith(Outfit other) {
        items.clear();
        items.putAll(other.items);
    }
}
