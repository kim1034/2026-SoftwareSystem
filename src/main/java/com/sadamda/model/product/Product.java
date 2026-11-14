package com.sadamda.model.product;

import java.util.ArrayList;
import java.util.List;

/** 상품 (UC-04~06, UC-16). */
public class Product {
    public enum Status { ON_SALE, SUSPENDED } // UC-16 3a 판매 중지

    private Long id;
    private String code;               // 데모 데이터의 상품ID (예: TOP-KR-01). 직접 등록한 상품은 null
    private String brand;
    private String country;            // 브랜드 국가
    private String subCategory;        // 세부 카테고리 (예: 신발(로퍼/플랫))
    private String description;        // 코디·트렌드 포인트
    private String productUrl;         // 공식몰 판매 페이지
    private int salesCount;            // 누적 판매 수량. 인기순 정렬에 쓴다
    private String name;
    private Category category;
    private int price;
    private String material;
    private List<ProductOption> options = new ArrayList<>();
    private List<String> imageUrls = new ArrayList<>();    // 중장년 모델 착용 사진 포함
    private List<String> ageGroupTags = new ArrayList<>(); // 추천 연령대 태그
    private List<String> bodyTypeTags = new ArrayList<>(); // 추천 체형 태그
    private boolean trending;          // 유행 아이템 지정 여부 (UC-17)
    private Status status = Status.ON_SALE;

    /** UC-09 AI 합성에 보낼 대표 의류 이미지. 이미지가 없으면 null. (Controller 명세에서 사용) */
    public String getMainImageUrl() {
        return imageUrls == null || imageUrls.isEmpty() ? null : imageUrls.get(0);
    }

    /** 대표 이미지. 없으면 null. */
    public String mainImageUrl() {
        return imageUrls.isEmpty() ? null : imageUrls.get(0);
    }

    /** 모든 옵션의 재고가 0이면 품절 (UC-06 4b, UC-07 6a). */
    public boolean isSoldOut() {
        return options.stream().noneMatch(option -> option.stock() > 0);
    }

    public boolean isOnSale() {
        return status == Status.ON_SALE;
    }

    /** 옵션 id 로 옵션을 찾는다. 없으면 null. */
    public ProductOption option(Long optionId) {
        return options.stream().filter(option -> option.id().equals(optionId)).findFirst().orElse(null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public List<ProductOption> getOptions() { return options; }
    public void setOptions(List<ProductOption> options) { this.options = copy(options); }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = copy(imageUrls); }

    public List<String> getAgeGroupTags() { return ageGroupTags; }
    public void setAgeGroupTags(List<String> ageGroupTags) { this.ageGroupTags = copy(ageGroupTags); }

    public List<String> getBodyTypeTags() { return bodyTypeTags; }
    public void setBodyTypeTags(List<String> bodyTypeTags) { this.bodyTypeTags = copy(bodyTypeTags); }

    public boolean isTrending() { return trending; }
    public void setTrending(boolean trending) { this.trending = trending; }

    public int getSalesCount() { return salesCount; }
    public void setSalesCount(int salesCount) { this.salesCount = salesCount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status == null ? Status.ON_SALE : status; }

    private static <T> List<T> copy(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    @Override
    public String toString() {
        return "[" + (category == null ? "" : category.label()) + "] " + name;
    }
}
