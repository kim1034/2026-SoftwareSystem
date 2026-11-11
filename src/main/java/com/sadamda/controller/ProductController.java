package com.sadamda.controller;

import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.model.review.Review;
import com.sadamda.model.review.ReviewService;

import java.util.List;

/** UC-04 카테고리별 상품 조회, UC-05 검색 및 필터, UC-06 상품 상세 조회. */
public class ProductController {
    private final ProductService productService;
    private final ReviewService reviewService;

    public ProductController(ProductService productService, ReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    /** UC-04 */
    public List<Product> listByCategory(Category category, int page) {
        return productService.findByCategory(category, page);
    }

    /** UC-05 */
    public List<Product> search(SearchFilter filter) {
        return productService.search(filter);
    }

    /** UC-06 */
    public Product detail(Long productId) {
        return productService.findById(productId);
    }

    /** UC-06 상세 화면의 리뷰 */
    public List<Review> reviews(Long productId) {
        return reviewService.findByProduct(productId);
    }
}
