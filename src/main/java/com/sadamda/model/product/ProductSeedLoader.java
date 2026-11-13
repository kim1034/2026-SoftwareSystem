package com.sadamda.model.product;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 데모용 상품 데이터(resources/data/products.tsv, 300개)를 읽어 Product 로 만든다.
 * DB 스키마가 정해지면 이 클래스 대신 DB 조회로 바꾼다.
 *
 * 데모 데이터에 없는 값은 아래처럼 임시로 채운다.
 * - 옵션: 카테고리별 기본 사이즈 × 소재 문구의 괄호 안 색상, 재고는 모두 10
 * - 연령대 태그: 40대·50대·60대 전부 / 체형 태그: ProductTags.guessBodyTypes
 * - 유행 아이템: 세부 카테고리마다 앞의 2개 (관리자가 UC-17 로 바꿀 수 있다)
 */
public final class ProductSeedLoader {
    private static final String RESOURCE = "/data/products.tsv";
    private static final int DEMO_STOCK = 10;

    private ProductSeedLoader() {
    }

    public static List<Product> load() {
        InputStream in = ProductSeedLoader.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            return new ArrayList<>(); // 데이터 파일이 없어도 프로그램은 켜져야 한다
        }
        List<Product> products = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                Product product = parse(line.split("\t", -1));
                if (product != null) {
                    products.add(product);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("상품 데이터를 읽지 못했습니다", e);
        }
        return products;
    }

    private static Product parse(String[] cols) {
        if (cols.length < 11) {
            return null;
        }
        Category category = Category.fromLabel(cols[2]);
        if (category == null) {
            return null;
        }
        Product product = new Product();
        product.setCode(cols[0]);
        product.setCountry(cols[1]);
        product.setCategory(category);
        product.setSubCategory(cols[3]);
        product.setBrand(cols[4]);
        product.setName(cols[5]);
        product.setPrice(Integer.parseInt(cols[6]));
        product.setMaterial(cols[7]);
        product.setDescription(cols[8]);
        product.setProductUrl(cols[9]);
        product.setImageUrls(List.of(cols[10]));
        product.setAgeGroupTags(ProductTags.AGE_GROUPS);
        product.setBodyTypeTags(ProductTags.guessBodyTypes(category, cols[5] + " " + cols[8]));
        product.setTrending(cols[0].endsWith("-01") || cols[0].endsWith("-02"));
        product.setOptions(demoOptions(category, cols[7]));
        return product;
    }

    /** 옵션 id 는 저장할 때 ProductService 가 붙인다. */
    private static List<ProductOption> demoOptions(Category category, String material) {
        List<String> sizes = switch (category) {
            case TOP, BOTTOM, DRESS, OUTER -> List.of("55", "66", "77", "88");
            case SHOES -> List.of("230", "235", "240", "245", "250");
            default -> List.of("FREE");
        };
        List<ProductOption> options = new ArrayList<>();
        for (String color : colors(material)) {
            for (String size : sizes) {
                options.add(new ProductOption(null, size, color, DEMO_STOCK));
            }
        }
        return options;
    }

    /** "울 혼방 트윌 (차콜 그레이)" → [차콜 그레이], "(네이비/버건디)" → [네이비, 버건디]. 없으면 [기본]. */
    static List<String> colors(String material) {
        List<String> colors = new ArrayList<>();
        int open = material.lastIndexOf('(');
        int close = material.lastIndexOf(')');
        if (open >= 0 && close > open) {
            for (String part : material.substring(open + 1, close).split("/")) {
                if (!part.isBlank() && colors.size() < 3) {
                    colors.add(part.trim());
                }
            }
        }
        if (colors.isEmpty()) {
            colors.add("기본");
        }
        return colors;
    }
}
