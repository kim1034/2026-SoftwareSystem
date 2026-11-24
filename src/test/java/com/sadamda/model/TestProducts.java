package com.sadamda.model;

import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.product.ProductService;

import java.util.List;

public final class TestProducts {
    public static final long COAT = 1L;
    public static final long COAT_M = 1L;
    public static final long COAT_L = 2L;
    public static final long COAT_SOLD_OUT = 3L;
    public static final long PANTS = 2L;
    public static final long PANTS_M = 4L;
    public static final int COAT_PRICE = 100_000;
    public static final int PANTS_PRICE = 30_000;

    private TestProducts() {
    }

    public static ProductService service() {
        return new ProductService(List.of(
                product("겨울 코트", Category.OUTER, COAT_PRICE, List.of(
                        new ProductOption(null, "M", "검정", 5),
                        new ProductOption(null, "L", "검정", 2),
                        new ProductOption(null, "XL", "검정", 0))),
                product("밴딩 바지", Category.BOTTOM, PANTS_PRICE, List.of(
                        new ProductOption(null, "M", "회색", 10)))));
    }

    private static Product product(String name, Category category, int price, List<ProductOption> options) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setPrice(price);
        product.setOptions(options);
        product.setImageUrls(List.of("https://example.com/" + name + ".jpg"));
        return product;
    }
}
