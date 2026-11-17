package com.sadamda.view.order;

import com.sadamda.common.AppException;
import com.sadamda.common.RetryableException;
import com.sadamda.controller.CheckoutRequest;
import com.sadamda.controller.OrderSheet;
import com.sadamda.model.cart.CartItem;
import com.sadamda.model.order.GiftInfo;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.order.OrderStatus;
import com.sadamda.model.product.ProductOption;
import com.sadamda.view.cart.MockCart;
import com.sadamda.view.product.MockProducts;
import com.sadamda.view.product.ProductDetail;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 임시 목업 주문. OrderService 가 구현되면 이 파일을 지우고 context.order 의 같은 이름 메서드로 바꾼다.
 * (OrderController 의 prepare*·checkout·cancel·requestExchangeOrReturn 은 구현됐지만 안에서 부르는 Model 이 TODO)
 * 결제·결제 취소는 늘 승인된 것으로 본다. 앱을 켜 둔 동안만 주문을 기억한다.
 */
public final class MockOrders {
    /** 테스트용: true 면 다음 결제 한 번을 "승인 거절"로 만든다 (UC-11 6a 확인). */
    static boolean failNextPayment;
    /** 테스트용: true 면 다음 주문 취소 한 번을 "결제 취소 실패"로 만든다 (RetryableException 확인). */
    static boolean failNextCancel;

    private static final List<MockOrder> ORDERS = new ArrayList<>();
    /** 시연용 예전 주문을 이미 넣어 준 회원. */
    private static final Set<Long> SEEDED = new HashSet<>();
    private static long nextId = 1;

    /** 완료된 주문. 실제 Order 는 생성자·getter 가 아직 없어서 화면용으로 따로 둔다. */
    public record MockOrder(Long id, String orderNumber, Long memberId, List<OrderLine> lines, String shippingAddress,
                            String paymentMethod, int productTotal, int shippingFee, GiftInfo giftInfo,
                            OrderStatus status, LocalDateTime orderedAt, String trackingNumber) {
        public int totalPrice() {
            return productTotal + shippingFee;
        }

        MockOrder withStatus(OrderStatus newStatus) {
            return new MockOrder(id, orderNumber, memberId, lines, shippingAddress, paymentMethod, productTotal,
                    shippingFee, giftInfo, newStatus, orderedAt, trackingNumber);
        }
    }

    private MockOrders() {
    }

    /** OrderController.prepareFromCart() — 장바구니 전체로 주문서를 만든다. 비어 있으면 AppException. */
    public static OrderSheet prepareFromCart(Long memberId, String defaultAddress) {
        List<OrderLine> lines = MockCart.cart(memberId).stream()
                .map(item -> new OrderLine(item.productId(), item.optionId(), item.quantity())).toList();
        if (lines.isEmpty()) {
            throw new AppException("장바구니가 비어 있어요.");
        }
        return new OrderSheet(lines, defaultAddress, productTotal(lines), true);
    }

    /** OrderController.prepareDirect(line) — 상세의 [바로 구매] 한 상품으로 주문서를 만든다. */
    public static OrderSheet prepareDirect(OrderLine line, String defaultAddress) {
        checkStock(List.of(line));
        return new OrderSheet(List.of(line), defaultAddress, productTotal(List.of(line)), false);
    }

    /**
     * OrderController.checkout(request) — 결제하고 주문을 만든다.
     * 결제가 거절되면 주문을 만들지 않는다 (6a). 재고가 모자라면 결제를 취소하고 품절을 알린다 (7a).
     * 장바구니에서 온 주문(request.fromCart())이면 주문한 줄을 장바구니에서 뺀다.
     */
    public static MockOrder checkout(Long memberId, CheckoutRequest request, int shippingFee) {
        if (failNextPayment) {
            failNextPayment = false;
            throw new AppException("결제가 승인되지 않았어요. 결제 정보를 확인하고 다시 시도해 주세요.");
        }
        checkStock(request.lines());
        MockOrder order = add(memberId, request.lines(), request.shippingAddress(), request.paymentMethod(),
                shippingFee, request.giftInfo(), OrderStatus.PAID, LocalDateTime.now(), null);
        if (request.fromCart()) {
            for (CartItem item : MockCart.cart(memberId)) {
                if (request.lines().stream().anyMatch(line -> line.optionId().equals(item.optionId()))) {
                    MockCart.remove(memberId, item.id());
                }
            }
        }
        return order;
    }

    /** OrderController.myOrders() — 이 회원의 주문, 최신순. 처음 부르면 시연용 예전 주문 2건을 넣는다. */
    public static List<MockOrder> ordersOf(Long memberId) {
        seed(memberId);
        return ORDERS.stream().filter(order -> order.memberId().equals(memberId))
                .sorted((a, b) -> b.orderedAt().compareTo(a.orderedAt())).toList();
    }

    /** OrderController.orderDetail(orderId) — 본인 주문만. */
    public static MockOrder orderDetail(Long memberId, Long orderId) {
        return owned(memberId, orderId);
    }

    /**
     * OrderController.cancel(orderId, reason) — 배송 전이면 결제를 취소하고 "취소됨"으로 바꾼다 (UC-13 4~5).
     * 배송 중·배송 완료면 AppException (4a). 결제 취소가 실패하면 주문은 그대로 두고 RetryableException.
     */
    public static MockOrder cancel(Long memberId, Long orderId, String reason) {
        MockOrder order = owned(memberId, orderId);
        if (order.status() == OrderStatus.SHIPPING || order.status() == OrderStatus.DELIVERED) {
            throw new AppException("이미 배송 중인 주문은 취소할 수 없어요. 받으신 뒤 반품을 신청해 주세요.");
        }
        if (order.status() != OrderStatus.PAID && order.status() != OrderStatus.PREPARING) {
            throw new AppException("취소할 수 없는 주문이에요.");
        }
        if (failNextCancel) {
            failNextCancel = false;
            throw new RetryableException("결제 취소에 실패했어요. 잠시 후 다시 시도해 주세요.");
        }
        return replace(order.withStatus(OrderStatus.CANCELED));
    }

    /**
     * OrderController.requestExchangeOrReturn(orderId, requested, reason) — 배송 완료된 주문의
     * 교환·반품을 "신청됨"으로 등록한다 (UC-13 6). 관리자 화면(UC-18)에서 처리한다.
     */
    public static MockOrder requestExchangeOrReturn(Long memberId, Long orderId, OrderStatus requested, String reason) {
        MockOrder order = owned(memberId, orderId);
        if (order.status() != OrderStatus.DELIVERED) {
            throw new AppException("배송이 끝난 주문만 교환·반품을 신청할 수 있어요.");
        }
        return replace(order.withStatus(requested));
    }

    private static MockOrder owned(Long memberId, Long orderId) {
        return ORDERS.stream().filter(order -> order.id().equals(orderId) && order.memberId().equals(memberId))
                .findFirst().orElseThrow(() -> new AppException("주문을 찾을 수 없어요."));
    }

    private static MockOrder replace(MockOrder updated) {
        for (int i = 0; i < ORDERS.size(); i++) {
            if (ORDERS.get(i).id().equals(updated.id())) {
                ORDERS.set(i, updated);
            }
        }
        return updated;
    }

    /** 배송 중·배송 완료 화면을 볼 수 있게 예전 주문 2건을 넣는다 (시연용). */
    private static void seed(Long memberId) {
        if (!SEEDED.add(memberId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        add(memberId, List.of(demoLine("TOP-KR-02")), "서울시 종로구 세종대로 1, 101동 101호",
                "카드 결제", OrderPanel.SHIPPING_FEE, null, OrderStatus.SHIPPING, now.minusDays(2), "6012-3456-7890");
        add(memberId, List.of(demoLine("BOT-KR-03"), demoLine("BOT-KR-09")),
                "서울시 종로구 세종대로 1, 101동 101호", "계좌이체", OrderPanel.SHIPPING_FEE, null,
                OrderStatus.DELIVERED, now.minusDays(9), "6012-1111-2222");
    }

    /** 시연용 주문 한 줄: 데모 데이터 상품(코드로 찾음)의 첫 옵션 1개. */
    private static OrderLine demoLine(String code) {
        Long productId = MockProducts.idOf(code);
        return new OrderLine(productId, MockProducts.detail(productId).options().get(0).id(), 1);
    }

    private static MockOrder add(Long memberId, List<OrderLine> lines, String address, String method, int shippingFee,
                                 GiftInfo gift, OrderStatus status, LocalDateTime orderedAt, String trackingNumber) {
        long id = nextId++;
        String number = String.format("SD%1$tY%1$tm%1$td-%2$04d", orderedAt, id);
        MockOrder order = new MockOrder(id, number, memberId, lines, address, method, productTotal(lines),
                shippingFee, gift, status, orderedAt, trackingNumber);
        ORDERS.add(order);
        return order;
    }

    private static int productTotal(List<OrderLine> lines) {
        return lines.stream().mapToInt(line -> MockProducts.detail(line.productId()).price() * line.quantity()).sum();
    }

    private static void checkStock(List<OrderLine> lines) {
        for (OrderLine line : lines) {
            ProductDetail product = MockProducts.detail(line.productId());
            ProductOption option = product.options().stream()
                    .filter(each -> each.id().equals(line.optionId())).findFirst().orElseThrow();
            if (option.stock() < line.quantity()) {
                throw new AppException("'" + product.name() + "'이(가) 품절되어 결제를 취소했어요.");
            }
        }
    }
}
