package com.sadamda.controller;

import com.sadamda.common.AccessDeniedException;
import com.sadamda.common.AppException;
import com.sadamda.common.RetryableException;
import com.sadamda.external.PaymentGateway;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.GiftInfo;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.order.OrderStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UC-11 주문 및 결제, UC-12 선물하기, UC-13 주문·배송 조회 및 취소·교환·반품.
 * 모든 메서드 로그인 필요 (UC-11 «include» 로그인). 담당: 김대원
 */
public class OrderController {
    static final String MSG_NO_LINES = "주문할 상품이 없습니다.";
    static final String MSG_NO_ADDRESS = "배송지를 입력해 주세요.";
    static final String MSG_NO_PAYMENT_METHOD = "결제 수단을 골라 주세요.";
    static final String MSG_GIFT_MISSING = "받는 분의 이름, 연락처, 주소를 모두 입력해 주세요.";
    static final String MSG_GIFT_BAD_PHONE = "받는 분 연락처를 숫자로 정확히 입력해 주세요.";
    static final String MSG_DECLINED_PREFIX = "결제가 완료되지 않았습니다. ";
    static final String MSG_GATEWAY_DOWN = "결제 시스템에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.";
    static final String MSG_PAYMENT_CANCELED_SUFFIX = " 결제는 취소되었습니다.";
    static final String MSG_SAVE_FAILED = "주문을 저장하지 못해 결제를 취소했습니다.";
    static final String MSG_CANCEL_FAILED = "결제 취소에 실패했습니다. 고객센터로 연락해 주세요.";
    static final String MSG_IN_PROGRESS = "결제가 진행 중입니다. 잠시만 기다려 주세요.";
    static final String MSG_NOT_OWNER = "본인 주문만 볼 수 있습니다.";
    static final String MSG_ALREADY_SHIPPED = "이미 배송이 시작되어 취소할 수 없습니다. 배송 완료 후 반품을 신청해 주세요.";
    static final String MSG_NOT_CANCELABLE = "취소할 수 없는 주문입니다.";
    static final String MSG_CANCEL_RETRY = "결제 취소에 실패했습니다. 잠시 후 다시 시도해 주세요.";
    static final String MSG_NOT_DELIVERED = "배송 완료된 주문만 교환·반품을 신청할 수 있습니다.";
    static final String MSG_NO_REASON = "사유를 골라 주세요.";

    private static final System.Logger LOG = System.getLogger(OrderController.class.getName());

    /** [결제하기] 중복 실행 방지. */
    private final AtomicBoolean checkoutInProgress = new AtomicBoolean(false);
    private final Session session;
    private final OrderService orderService;
    private final CartService cartService;
    private final MemberService memberService;
    private final PaymentGateway paymentGateway;

    public OrderController(Session session, OrderService orderService, CartService cartService,
                           MemberService memberService, PaymentGateway paymentGateway) {
        this.session = session;
        this.orderService = orderService;
        this.cartService = cartService;
        this.memberService = memberService;
        this.paymentGateway = paymentGateway;
    }

    /** UC-10 5단계 [주문하기] → UC-11 2단계 장바구니 전체로 주문서를 만든다. */
    public OrderSheet prepareFromCart() {
        Long memberId = session.requireLogin().getId();
        List<OrderLine> lines = cartService.getCart(memberId).stream()
                .map(item -> new OrderLine(item.productId(), item.optionId(), item.quantity()))
                .toList();
        if (lines.isEmpty()) {
            throw new AppException("장바구니가 비어 있습니다.");
        }
        return sheet(memberId, lines, true);
    }

    /** UC-06 4단계 [바로 구매] → UC-11 2단계 상품 하나로 주문서를 만든다. */
    public OrderSheet prepareDirect(OrderLine line) {
        Long memberId = session.requireLogin().getId();
        if (line.quantity() < 1) {
            throw new AppException("수량을 1개 이상 골라 주세요.");
        }
        return sheet(memberId, List.of(line), false);
    }

    private OrderSheet sheet(Long memberId, List<OrderLine> lines, boolean fromCart) {
        // 세션의 회원 정보는 오래됐을 수 있어 기본 배송지는 새로 읽는다.
        String defaultAddress = memberService.findById(memberId).getDefaultAddress();
        return new OrderSheet(lines, defaultAddress, orderService.calculateTotal(lines), fromCart);
    }

    /**
     * UC-11 4~8단계 [결제하기]. 결제 승인 → 주문 저장·재고 차감 → 주문 반환.
     * request.giftInfo() 가 있으면 UC-12 선물 주문.
     * AppException 메시지: 결제 실패·취소(6a, 주문 안 만듦), 재고 소진(7a, 결제 자동 취소).
     * 결제 시스템 응답을 기다리므로 View 는 별도 스레드에서 호출한다.
     */
    public Order checkout(CheckoutRequest request) {
        Long memberId = session.requireLogin().getId();
        String shippingAddress = validate(request);
        if (!checkoutInProgress.compareAndSet(false, true)) {
            throw new AppException(MSG_IN_PROGRESS);
        }
        try {
            int total = orderService.calculateTotal(request.lines());
            String paymentId = approve(total, request.paymentMethod());
            Order order = placeOrCancel(memberId, request, shippingAddress, paymentId);
            if (request.fromCart()) {
                orderService.clearOrderedFromCart(memberId, request.lines());
            }
            return order;
        } finally {
            checkoutInProgress.set(false);
        }
    }

    /** 입력을 검증하고 실제 배송지(선물이면 받는 분 주소)를 돌려준다. */
    private static String validate(CheckoutRequest request) {
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new AppException(MSG_NO_LINES);
        }
        GiftInfo gift = request.giftInfo();
        if (gift == null && isBlank(request.shippingAddress())) {
            throw new AppException(MSG_NO_ADDRESS);
        }
        if (isBlank(request.paymentMethod())) {
            throw new AppException(MSG_NO_PAYMENT_METHOD);
        }
        if (gift == null) {
            return request.shippingAddress();
        }
        if (isBlank(gift.recipientName()) || isBlank(gift.recipientPhone()) || isBlank(gift.recipientAddress())) {
            throw new AppException(MSG_GIFT_MISSING);
        }
        if (!isValidPhone(gift.recipientPhone())) {
            throw new AppException(MSG_GIFT_BAD_PHONE);
        }
        return gift.recipientAddress();
    }

    private String approve(int total, String paymentMethod) {
        PaymentGateway.PaymentResult result;
        try {
            result = paymentGateway.approve(total, paymentMethod);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "결제 승인 요청 실패", e);
            throw new AppException(MSG_GATEWAY_DOWN);
        }
        if (!result.approved()) {
            throw new AppException(MSG_DECLINED_PREFIX + result.message());
        }
        return result.paymentId();
    }

    /** 주문을 저장한다. 어떤 이유로든 실패하면 결제를 취소한다 (UC-11 7a). */
    private Order placeOrCancel(Long memberId, CheckoutRequest request, String shippingAddress, String paymentId) {
        try {
            return orderService.placeOrder(memberId, request.lines(), shippingAddress, request.giftInfo(), paymentId);
        } catch (RuntimeException placeFailure) {
            if (!cancelPayment(paymentId)) {
                LOG.log(System.Logger.Level.ERROR, "주문 저장 실패 후 결제 취소 실패: paymentId={0}, memberId={1}",
                        paymentId, memberId);
                throw new AppException(MSG_CANCEL_FAILED);
            }
            if (placeFailure instanceof AppException) {
                throw new AppException(placeFailure.getMessage() + MSG_PAYMENT_CANCELED_SUFFIX);
            }
            LOG.log(System.Logger.Level.ERROR, "주문 저장 실패, 결제 취소함: paymentId=" + paymentId, placeFailure);
            throw new AppException(MSG_SAVE_FAILED);
        }
    }

    private boolean cancelPayment(String paymentId) {
        try {
            return paymentGateway.cancel(paymentId).approved();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "결제 취소 요청 실패", e);
            return false;
        }
    }

    private static boolean isValidPhone(String phone) {
        if (!phone.matches("[0-9-]+")) {
            return false;
        }
        int digits = phone.replace("-", "").length();
        return digits >= 10 && digits <= 11;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** UC-13 1~2단계 내 주문 목록, 최신순. */
    public List<Order> myOrders() {
        return orderService.findByMember(session.requireLogin().getId());
    }

    /** UC-13 3단계 주문 상세. 본인 주문이 아니면 AccessDeniedException. */
    public Order orderDetail(Long orderId) {
        return ownedOrder(orderId);
    }

    /**
     * UC-13 4~5단계 배송 전 취소: 결제 취소 후 "취소됨". 배송 중이면 AppException (4a).
     * 결제 취소가 실패하면 주문은 그대로 두고 RetryableException.
     */
    public Order cancel(Long orderId, String reason) {
        Order order = ownedOrder(orderId);
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.SHIPPING || status == OrderStatus.DELIVERED) {
            throw new AppException(MSG_ALREADY_SHIPPED);
        }
        if (status != OrderStatus.PAID && status != OrderStatus.PREPARING) {
            throw new AppException(MSG_NOT_CANCELABLE);
        }
        if (!cancelPayment(order.getPaymentId())) {
            LOG.log(System.Logger.Level.ERROR, "주문 취소 중 결제 취소 실패: orderId={0}, paymentId={1}",
                    orderId, order.getPaymentId());
            throw new RetryableException(MSG_CANCEL_RETRY);
        }
        return orderService.cancel(orderId, reason);
    }

    /** UC-13 6단계 교환·반품 신청. requested 는 EXCHANGE_REQUESTED 또는 RETURN_REQUESTED. */
    public Order requestExchangeOrReturn(Long orderId, OrderStatus requested, String reason) {
        if (requested != OrderStatus.EXCHANGE_REQUESTED && requested != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalArgumentException("requested 는 EXCHANGE_REQUESTED 또는 RETURN_REQUESTED: " + requested);
        }
        Order order = ownedOrder(orderId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException(MSG_NOT_DELIVERED);
        }
        if (isBlank(reason)) {
            throw new AppException(MSG_NO_REASON);
        }
        return orderService.requestChange(orderId, requested, reason);
    }

    private Order ownedOrder(Long orderId) {
        Long memberId = session.requireLogin().getId();
        Order order = orderService.findById(orderId);
        if (!memberId.equals(order.getMemberId())) {
            throw new AccessDeniedException(MSG_NOT_OWNER);
        }
        return order;
    }
}
