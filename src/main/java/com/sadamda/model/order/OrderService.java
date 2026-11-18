package com.sadamda.model.order;

import com.sadamda.common.AppException;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.product.ProductService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 주문 저장·재고 차감·상태 변경. 결제 연동은 OrderController 가 맡는다. 담당: 백예원 */
public class OrderService {
    public static final int SHIPPING_FEE = 3000;

    private static final Set<OrderStatus> ACTIVE = EnumSet.of(OrderStatus.PAID, OrderStatus.PREPARING,
            OrderStatus.SHIPPING, OrderStatus.EXCHANGE_REQUESTED, OrderStatus.RETURN_REQUESTED);
    private static final Comparator<Order> NEWEST_FIRST = Comparator.comparing(Order::getOrderedAt)
            .thenComparing(Order::getId).reversed();

    private final ProductService productService;
    private final CartService cartService;
    private final Map<Long, Order> orders = new LinkedHashMap<>();
    private long nextId = 1;

    public OrderService(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
        productService.setActiveOrderChecker(this::hasActiveOrders);
    }

    /** UC-11 2단계 결제 금액 계산. 품절·판매 중지 상품이 있으면 AppException. */
    public synchronized int calculateTotal(List<OrderLine> lines) {
        return productTotal(lines) + SHIPPING_FEE;
    }

    /**
     * UC-11 7단계 결제 승인 후 주문 저장 및 재고 차감 (FR-19).
     * 그사이 재고가 소진됐으면 AppException (7a) — Controller 가 결제를 취소한다.
     * giftInfo 가 null 이 아니면 선물 주문 (UC-12, FR-20).
     */
    public synchronized Order placeOrder(Long memberId, List<OrderLine> lines, String shippingAddress,
                                         GiftInfo giftInfo, String paymentId) {
        int productTotal = productTotal(lines);
        List<OrderItem> items = new ArrayList<>();
        for (OrderLine line : lines) {
            Product product = productService.findById(line.productId());
            if (!productService.decreaseStock(line.productId(), line.optionId(), line.quantity())) {
                throw new AppException("'" + product.getName() + "'이(가) 품절되었습니다.");
            }
            items.add(new OrderItem(line.productId(), line.optionId(), line.quantity(), product.getPrice()));
        }
        Order order = new Order(nextId++, memberId, items, shippingAddress, productTotal, SHIPPING_FEE,
                paymentId, giftInfo, LocalDateTime.now());
        orders.put(order.getId(), order);
        return order;
    }

    /** UC-11 장바구니에서 주문한 상품을 장바구니에서 비운다. */
    public void clearOrderedFromCart(Long memberId, List<OrderLine> lines) {
        cartService.removeOrdered(memberId, lines);
    }

    /** UC-13 주문 목록 (FR-21), 최신순. */
    public synchronized List<Order> findByMember(Long memberId) {
        return orders.values().stream()
                .filter(order -> order.getMemberId().equals(memberId))
                .sorted(NEWEST_FIRST)
                .toList();
    }

    /** UC-13 주문 상세. 없으면 AppException. */
    public synchronized Order findById(Long orderId) {
        Order order = orderId == null ? null : orders.get(orderId);
        if (order == null) {
            throw new AppException("주문을 찾을 수 없습니다.");
        }
        return order;
    }

    /** UC-13 5단계 배송 전 취소. 결제 취소 성공 후 호출된다. 배송 중이면 AppException (4a). */
    public synchronized Order cancel(Long orderId, String reason) {
        Order order = findById(orderId);
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.SHIPPING || status == OrderStatus.DELIVERED) {
            throw new AppException("이미 배송이 시작되어 취소할 수 없습니다.");
        }
        if (status != OrderStatus.PAID && status != OrderStatus.PREPARING) {
            throw new AppException("취소할 수 없는 주문입니다.");
        }
        order.changeStatus(OrderStatus.CANCELED, reason);
        return order;
    }

    /** UC-13 6단계 교환·반품 신청 (FR-22). requested 는 EXCHANGE_REQUESTED 또는 RETURN_REQUESTED. */
    public synchronized Order requestChange(Long orderId, OrderStatus requested, String reason) {
        if (requested != OrderStatus.EXCHANGE_REQUESTED && requested != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalArgumentException("requested 는 EXCHANGE_REQUESTED 또는 RETURN_REQUESTED: " + requested);
        }
        Order order = findById(orderId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException("배송 완료된 주문만 교환·반품을 신청할 수 있습니다.");
        }
        order.changeStatus(requested, reason);
        return order;
    }

    /** UC-18 상태별 주문 목록 (FR-27). status 가 null 이면 전체. */
    public synchronized List<Order> findByStatus(OrderStatus status) {
        return orders.values().stream()
                .filter(order -> status == null || order.getStatus() == status)
                .sorted(NEWEST_FIRST)
                .toList();
    }

    /** UC-18 3단계 배송 상태 변경 (배송준비 → 배송중(송장번호) → 배송완료). 잘못된 전이면 AppException. */
    public synchronized Order updateStatus(Long orderId, OrderStatus status, String trackingNumber) {
        Order order = findById(orderId);
        OrderStatus current = order.getStatus();
        boolean allowed = (current == OrderStatus.PAID && status == OrderStatus.PREPARING)
                || (current == OrderStatus.PREPARING && status == OrderStatus.SHIPPING)
                || (current == OrderStatus.SHIPPING && status == OrderStatus.DELIVERED);
        if (!allowed) {
            throw new AppException("지금 상태에서는 그 상태로 바꿀 수 없습니다.");
        }
        if (status == OrderStatus.SHIPPING) {
            if (trackingNumber == null || trackingNumber.isBlank()) {
                throw new AppException("송장번호를 입력해 주세요.");
            }
            order.setTrackingNumber(trackingNumber.trim());
        }
        order.changeStatus(status, null);
        return order;
    }

    /** UC-18 4~5단계 교환·반품 승인 결과 반영. 반품은 환불 성공 후 호출된다. */
    public synchronized Order approveChange(Long orderId) {
        Order order = findById(orderId);
        if (order.getStatus() == OrderStatus.RETURN_REQUESTED) {
            order.changeStatus(OrderStatus.RETURNED, order.getReason());
        } else if (order.getStatus() == OrderStatus.EXCHANGE_REQUESTED) {
            order.setTrackingNumber(null);
            order.changeStatus(OrderStatus.PREPARING, order.getReason());
        } else {
            throw new AppException("처리할 교환·반품 신청이 없습니다.");
        }
        return order;
    }

    /** UC-18 4단계 교환·반품 거절. */
    public synchronized Order rejectChange(Long orderId, String reason) {
        Order order = findById(orderId);
        if (order.getStatus() != OrderStatus.EXCHANGE_REQUESTED && order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new AppException("처리할 교환·반품 신청이 없습니다.");
        }
        order.changeStatus(OrderStatus.DELIVERED, reason);
        return order;
    }

    public synchronized boolean hasActiveOrders(long productId) {
        for (Order order : orders.values()) {
            if (ACTIVE.contains(order.getStatus())
                    && order.getItems().stream().anyMatch(item -> item.productId() == productId)) {
                return true;
            }
        }
        return false;
    }

    private int productTotal(List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new AppException("주문할 상품이 없습니다.");
        }
        Map<Long, Integer> perOption = new LinkedHashMap<>();
        int total = 0;
        for (OrderLine line : lines) {
            if (line.quantity() < 1) {
                throw new AppException("수량을 1개 이상 골라 주세요.");
            }
            Product product = productService.findById(line.productId());
            if (!product.isOnSale()) {
                throw new AppException("'" + product.getName() + "'은(는) 판매가 중지된 상품입니다.");
            }
            ProductOption option = product.option(line.optionId());
            if (option == null) {
                throw new AppException("판매하지 않는 옵션입니다.");
            }
            int quantity = perOption.merge(line.optionId(), line.quantity(), Integer::sum);
            if (option.stock() < quantity) {
                throw new AppException("'" + product.getName() + "'이(가) 품절되었습니다.");
            }
            total += product.getPrice() * line.quantity();
        }
        return total;
    }
}