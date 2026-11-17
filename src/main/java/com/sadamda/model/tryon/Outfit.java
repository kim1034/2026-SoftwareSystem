package com.sadamda.model.tryon;

import com.sadamda.common.AppException;
import com.sadamda.model.product.Product;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 옷 입혀보기에서 캐릭터에게 입힌 한 벌. 자리마다 상품 1개. */
public class Outfit {
    private final Map<DressUpSlot, Product> items = new EnumMap<>(DressUpSlot.class);

    /** 자리에 상품을 입힌다. 그 자리의 카테고리와 다른 상품이면 예외. */
    public void put(DressUpSlot slot, Product product) {
        if (product.getCategory() != slot.category()) {
            throw new AppException(slot.label() + " 자리에는 " + slot.category().label() + " 상품만 입힐 수 있습니다.");
        }
        items.put(slot, product);
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

    /** 입힌 상품 전체 (모자 → 신발 순). */
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
}
