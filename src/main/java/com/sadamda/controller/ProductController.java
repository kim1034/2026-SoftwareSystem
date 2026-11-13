package com.sadamda.controller;

import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.model.review.Review;
import com.sadamda.model.review.ReviewService;

import java.util.List;

/** UC-04 카테고리별 상품 조회, UC-05 검색 및 필터, UC-06 상품 상세 조회. 로그인 불필요. */
public class ProductController {
    private static final int POPULAR_LIMIT = 8;

    private final ProductService productService;
    private final ReviewService reviewService;

    public ProductController(ProductService productService, ReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    /** UC-04 2~3단계. page 는 0부터, [더 보기]마다 1씩 늘린다. */
    public List<Product> listByCategory(Category category, int page) {
        return productService.findByCategory(category, page);
    }

    /** UC-04 2a, UC-05 2a 결과가 없을 때 보여줄 인기 상품. */
    public List<Product> popular() {
        return productService.findPopular(POPULAR_LIMIT);
    }

    /** UC-05. 3a [내 체형에 맞는 상품]은 View 가 회원 체형 정보로 filter 를 채워 호출한다. */
    public List<Product> search(SearchFilter filter) {
        return productService.search(filter);
    }

    /** UC-06 2단계 */
    public Product detail(Long productId) {
        return productService.findById(productId);
    }

    /** UC-06 2단계 상세 화면의 리뷰 */
    public List<Review> reviews(Long productId) {
        return reviewService.findByProduct(productId);
    }
}
