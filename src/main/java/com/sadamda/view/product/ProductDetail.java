package com.sadamda.view.product;

import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;

import java.util.List;

/**
 * 상품 상세 화면에 보여줄 정보 (UC-06 2단계: 사진들, 가격, 소재, 사이즈·색상 옵션).
 * imageUrls 에는 중장년 모델 착용 사진도 들어 있다.
 */
public record ProductDetail(Long id, String name, int price, String material, List<String> imageUrls,
                     List<ProductOption> options) {

    static ProductDetail of(Product product) {
        return new ProductDetail(product.getId(), product.getName(), product.getPrice(), product.getMaterial(),
                product.getImageUrls(), product.getOptions());
    }
}
