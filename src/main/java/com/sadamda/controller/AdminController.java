package com.sadamda.controller;

import com.sadamda.common.AppException;
import com.sadamda.common.RetryableException;
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
 * UC-16 상품 관리, UC-17 룩북·유행 아이템 관리, UC-18 주문 관리.
 * 모든 메서드 관리자 로그인 필요 (아니면 LoginRequiredException / AccessDeniedException). 담당: 김대원
 */
public class AdminController {
    static final String MSG_NO_TRACKING = "송장번호를 입력해 주세요.";
    static final String MSG_REFUND_RETRY = "환불 요청에 실패했습니다. 다시 시도해 주세요.";
    static final String MSG_NO_REQUEST = "처리할 교환·반품 요청이 없습니다.";
    static final String MSG_NO_REJECT_REASON = "거절 사유를 입력해 주세요.";

    private static final System.Logger LOG = System.getLogger(AdminController.class.getName());

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

    // ---------- UC-16 상품 관리 (계획 5단계) ----------

    /** UC-16 2단계 판매 중지 포함 전체 상품. */
    public List<Product> products() {
        session.requireAdmin();
        return productService.findAll();
    }

    /** UC-16 4~5단계 등록(id 가 null)·수정. 필수 항목이 빠지면 AppException (5a). */
    public Product saveProduct(Product product) {
        session.requireAdmin();
        return productService.save(product);
    }

    /** UC-16 3a 삭제 전에 호출. true 면 View 가 "판매 중지로 바꿀까요?"를 묻는다. */
    public boolean hasActiveOrders(Long productId) {
        session.requireAdmin();
        return productService.hasActiveOrders(productId);
    }

    /** UC-16 3a 판매 중지 */
    public void suspendProduct(Long productId) {
        session.requireAdmin();
        productService.suspend(productId);
    }

    /** UC-16 3단계 삭제 */
    public void deleteProduct(Long productId) {
        session.requireAdmin();
        productService.delete(productId);
    }

    // ---------- UC-17 룩북·유행 아이템 관리 (계획 5단계) ----------

    /** UC-17 1~3단계 코디 등록. 판매 중지 상품이 들어 있으면 AppException (2a). */
    public Lookbook saveLookbook(Lookbook lookbook) {
        session.requireAdmin();
        return recommendService.saveLookbook(lookbook);
    }

    /** UC-17 4~5단계 유행 아이템 지정·해제 */
    public void setTrending(Long productId, boolean trending) {
        session.requireAdmin();
        recommendService.setTrending(productId, trending);
    }

    // ---------- UC-18 주문 관리 (계획 7단계) ----------

    /** UC-18 2단계 상태별 주문 목록. status 가 null 이면 전체. */
    public List<Order> orders(OrderStatus status) {
        session.requireAdmin();
        return orderService.findByStatus(status);
    }

    /** UC-18 3단계 배송 상태 변경. SHIPPING 으로 바꿀 때 trackingNumber 필수. */
    public Order updateOrderStatus(Long orderId, OrderStatus status, String trackingNumber) {
        session.requireAdmin();
        if (status == OrderStatus.SHIPPING && isBlank(trackingNumber)) {
            throw new AppException(MSG_NO_TRACKING);
        }
        return orderService.updateStatus(orderId, status, trackingNumber);
    }

    /** UC-18 4~5단계 교환·반품 승인. 반품이면 환불 후 반영, 환불 실패 시 RetryableException (5a). */
    public Order approveRequest(Long orderId) {
        session.requireAdmin();
        Order order = orderService.findById(orderId);
        if (order.getStatus() == OrderStatus.RETURN_REQUESTED) {
            if (!refund(order.getPaymentId())) {
                LOG.log(System.Logger.Level.ERROR, "반품 환불 실패: orderId={0}, paymentId={1}",
                        orderId, order.getPaymentId());
                throw new RetryableException(MSG_REFUND_RETRY);
            }
        } else if (order.getStatus() != OrderStatus.EXCHANGE_REQUESTED) {
            throw new AppException(MSG_NO_REQUEST);
        }
        return orderService.approveChange(orderId);
    }

    /** UC-18 4단계 교환·반품 거절 */
    public Order rejectRequest(Long orderId, String reason) {
        session.requireAdmin();
        if (isBlank(reason)) {
            throw new AppException(MSG_NO_REJECT_REASON);
        }
        return orderService.rejectChange(orderId, reason);
    }

    private boolean refund(String paymentId) {
        try {
            return paymentGateway.cancel(paymentId).approved();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "환불 요청 실패", e);
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
