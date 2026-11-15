package com.sadamda.view.order;

import com.sadamda.common.AppException;
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
import java.util.List;

/**
 * 임시 목업 주문. OrderController 의 prepareFromCart / prepareDirect / checkout 이 구현되면 이 파일을 지운다.
 * 결제는 늘 승인된 것으로 본다. 앱을 켜 둔 동안만 주문을 기억한다 (주문 내역 UC-13 에서 쓸 예정).
 */
public final class MockOrders {
    /** 테스트용: true 면 다음 결제 한 번을 "승인 거절"로 만든다 (UC-11 6a 확인). */
    static boolean failNextPayment;

    private static final List<MockOrder> ORDERS = new ArrayList<>();
    private static long nextId = 1;

    /** 완료된 주문. 실제 Order 는 생성자·getter 가 아직 없어서 화면용으로 따로 둔다. */
    public record MockOrder(Long id, String orderNumber, Long memberId, List<OrderLine> lines, String shippingAddress,
                            String paymentMethod, int productTotal, int shippingFee, GiftInfo giftInfo,
                            OrderStatus status, LocalDateTime orderedAt) {
        public int totalPrice() {
            return productTotal + shippingFee;
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
        long id = nextId++;
        LocalDateTime now = LocalDateTime.now();
        String number = String.format("SD%1$tY%1$tm%1$td-%2$04d", now, id);
        MockOrder order = new MockOrder(id, number, memberId, request.lines(), request.shippingAddress(),
                request.paymentMethod(), productTotal(request.lines()), shippingFee, request.giftInfo(),
                OrderStatus.PAID, now);
        ORDERS.add(order);
        if (request.fromCart()) {
            for (CartItem item : MockCart.cart(memberId)) {
                if (request.lines().stream().anyMatch(line -> line.optionId().equals(item.optionId()))) {
                    MockCart.remove(memberId, item.id());
                }
            }
        }
        return order;
    }

    /** 이 회원의 주문, 최신순. (UC-13 에서 쓸 예정) */
    public static List<MockOrder> ordersOf(Long memberId) {
        return ORDERS.stream().filter(order -> order.memberId().equals(memberId))
                .sorted((a, b) -> b.id().compareTo(a.id())).toList();
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
