package com.sadamda.controller;

import com.sadamda.external.PaymentGateway;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.order.OrderStatus;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.model.recommend.RecommendService;

import java.util.List;

/**
 * UC-16 상품 관리, UC-17 룩북·유행 아이템 관리, UC-18 주문 관리. 담당: 김대원
 * TODO: 모든 메서드에서 관리자 로그인 여부 확인 (UC-02)
 */
public class AdminController {
    private final Session session;
    private final ProductService productService;
    private final RecommendService recommendService;
    private final OrderService orderService;
    private final PaymentGateway paymentGateway;

    public AdminController(Session session, ProductService productService, RecommendService recommendService,
                           OrderService orderService, PaymentGateway paymentGateway) {
        this.session = session;
        this.productService = productService;
        this.recommendService = recommendService;
        this.orderService = orderService;
        this.paymentGateway = paymentGateway;
    }

    /** UC-16 */
    public Product saveProduct(Product product) {
        return productService.save(product);
    }

    /** UC-16 */
    public void deleteProduct(Long productId) {
        productService.delete(productId);
    }

    /** UC-17 1~3단계 */
    public Lookbook saveLookbook(Lookbook lookbook) {
        return recommendService.saveLookbook(lookbook);
    }

    /** UC-17 4~5단계 */
    public void setTrending(Long productId, boolean trending) {
        recommendService.setTrending(productId, trending);
    }

    /** UC-18 2단계 */
    public List<Order> ordersByStatus(OrderStatus status) {
        return orderService.findByStatus(status);
    }

    /** UC-18 3단계 */
    public Order updateOrderStatus(Long orderId, OrderStatus status, String trackingNumber) {
        return orderService.updateStatus(orderId, status, trackingNumber);
    }

    /** UC-18 4~5단계: 반품 승인 시 환불 요청. 실패하면 오류 기록 후 재시도 안내 (5a). */
    public Order approveReturn(Long orderId) {
        throw new UnsupportedOperationException("TODO UC-18");
    }
}
