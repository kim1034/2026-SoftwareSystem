package com.sadamda.controller;

import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.CodiBookService;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.model.tryon.SavedCodi;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * UC-19 내 코디북. 모든 메서드 로그인 필요 (LoginRequiredException). 담당: 정우진
 * 옷 입혀보기 화면(TryOnPanel)의 [코디북에 저장]·[내 코디북]에서 부른다.
 */
public class CodiBookController {
    private final Session session;
    private final CodiBookService codiBookService;
    private final ProductService productService;

    public CodiBookController(Session session, CodiBookService codiBookService, ProductService productService) {
        this.session = session;
        this.codiBookService = codiBookService;
        this.productService = productService;
    }

    /** UC-19 3~4단계 새 코디로 저장. image 는 [입혀보기]로 만든 그림이며 없으면 null. */
    public SavedCodi save(String title, CharacterStyle style, CharacterSpec spec, Outfit outfit, byte[] image) {
        return codiBookService.save(owner(), title, style, spec, productIds(outfit), image);
    }

    /** UC-19 4a 다시 연 코디에 덮어쓰기. */
    public SavedCodi overwrite(long codiId, CharacterStyle style, CharacterSpec spec, Outfit outfit, byte[] image) {
        return codiBookService.overwrite(owner(), codiId, style, spec, productIds(outfit), image);
    }

    /** UC-19 1단계 내 코디북 (최근 것부터). */
    public List<SavedCodi> list() {
        return codiBookService.findAll(owner());
    }

    /** UC-19 5a */
    public SavedCodi rename(long codiId, String title) {
        return codiBookService.rename(owner(), codiId, title);
    }

    /** UC-19 5b */
    public void delete(long codiId) {
        codiBookService.delete(owner(), codiId);
    }

    /** 목록에 보여줄 구성 상품 (지금도 있는 상품만, 모자 → 신발 순). */
    public List<Product> products(SavedCodi codi) {
        List<Product> result = new ArrayList<>();
        for (Long productId : codi.productIds().values()) {
            Product product = productService.findByIdOrNull(productId);
            if (product != null) {
                result.add(product);
            }
        }
        return result;
    }

    /**
     * UC-19 6단계 [다시 입혀보기]: 저장한 코디를 지금 상품 정보로 되살린다.
     * 지워졌거나 판매가 끝난 상품은 빼고, 뺀 개수를 함께 돌려준다 (6a).
     */
    public Opened open(long codiId) {
        SavedCodi codi = codiBookService.find(owner(), codiId);
        Outfit outfit = new Outfit();
        int missing = 0;
        for (Map.Entry<DressUpSlot, Long> entry : codi.productIds().entrySet()) {
            Product product = productService.findByIdOrNull(entry.getValue());
            if (product == null || !product.isOnSale() || product.getCategory() != entry.getKey().category()) {
                missing++;
            } else {
                outfit.put(entry.getKey(), product);
            }
        }
        // 빠진 상품이 있으면 예전 그림은 지금 옷차림과 달라서 보여주지 않는다
        byte[] image = missing == 0 ? codiBookService.readImage(codi) : null;
        return new Opened(codi, outfit, image, missing);
    }

    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    private String owner() {
        return CodiBookService.ownerKey(session.requireLogin().getEmail());
    }

    private static Map<DressUpSlot, Long> productIds(Outfit outfit) {
        Map<DressUpSlot, Long> ids = new EnumMap<>(DressUpSlot.class);
        if (outfit != null) {
            for (DressUpSlot slot : DressUpSlot.values()) {
                Product product = outfit.get(slot);
                if (product != null) {
                    ids.put(slot, product.getId());
                }
            }
        }
        return ids;
    }

    /**
     * 다시 연 코디.
     * @param image   저장한 그림. 그림 없이 저장했거나 빠진 상품이 있으면 null
     * @param missing 지워졌거나 판매가 끝나 뺀 상품 수
     */
    public record Opened(SavedCodi codi, Outfit outfit, byte[] image, int missing) {
    }
}
