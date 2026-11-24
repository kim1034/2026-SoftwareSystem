package com.sadamda.model.order;

import com.sadamda.common.AppException;
import com.sadamda.model.TestProducts;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.sadamda.model.TestProducts.COAT;
import static com.sadamda.model.TestProducts.COAT_L;
import static com.sadamda.model.TestProducts.COAT_M;
import static com.sadamda.model.TestProducts.COAT_PRICE;
import static com.sadamda.model.TestProducts.PANTS;
import static com.sadamda.model.TestProducts.PANTS_M;
import static com.sadamda.model.TestProducts.PANTS_PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceTest {
    private static final Long MEMBER = 7L;
    private static final String ADDRESS = "광주광역시 북구 용봉로 77";

    private final ProductService productService = TestProducts.service();
    private final CartService cartService = new CartService(productService);
    private final OrderService orders = new OrderService(productService, cartService);

    private Order place(OrderLine... lines) {
        return orders.placeOrder(MEMBER, List.of(lines), ADDRESS, null, "pay-1", "카드 결제");
    }

    private int stock(Long productId, Long optionId) {
        return productService.findById(productId).option(optionId).stock();
    }

    private Order delivered() {
        Order order = place(new OrderLine(COAT, COAT_M, 1));
        orders.updateStatus(order.getId(), OrderStatus.PREPARING, null);
        orders.updateStatus(order.getId(), OrderStatus.SHIPPING, "6012-0000-1111");
        orders.updateStatus(order.getId(), OrderStatus.DELIVERED, null);
        return order;
    }

    @Test
    void calculateTotal_includesShippingFee() {
        int total = orders.calculateTotal(List.of(new OrderLine(COAT, COAT_M, 2), new OrderLine(PANTS, PANTS_M, 1)));

        assertEquals(COAT_PRICE * 2 + PANTS_PRICE + OrderService.SHIPPING_FEE, total);
    }

    @Test
    void calculateTotalOverStock_throws() {
        assertThrows(AppException.class, () -> orders.calculateTotal(List.of(new OrderLine(COAT, COAT_L, 3))));
    }

    @Test
    void calculateTotalCountsSameOptionTogether() {
        assertThrows(AppException.class, () -> orders.calculateTotal(
                List.of(new OrderLine(COAT, COAT_L, 2), new OrderLine(COAT, COAT_L, 1))));
    }

    @Test
    void calculateTotalEmpty_throws() {
        assertThrows(AppException.class, () -> orders.calculateTotal(List.of()));
    }

    @Test
    void placeOrder_savesOrderAndDecreasesStock() {
        Order order = place(new OrderLine(COAT, COAT_M, 2));

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(COAT_PRICE * 2, order.getProductTotal());
        assertEquals(OrderService.SHIPPING_FEE, order.getShippingFee());
        assertEquals(COAT_PRICE * 2 + OrderService.SHIPPING_FEE, order.getTotalPrice());
        assertEquals("카드 결제", order.getPaymentMethod());
        assertEquals(ADDRESS, order.getShippingAddress());
        assertEquals(COAT_PRICE, order.getItems().get(0).unitPrice());
        assertEquals(3, stock(COAT, COAT_M));
    }

    @Test
    void placeOrderWhenStockRanOut_throwsAndSavesNothing() {
        assertThrows(AppException.class, () -> place(new OrderLine(COAT, COAT_L, 3)));

        assertTrue(orders.findByMember(MEMBER).isEmpty());
        assertEquals(2, stock(COAT, COAT_L));
    }

    @Test
    void giftOrder_keepsGiftInfo() {
        GiftInfo gift = new GiftInfo("엄마", "010-1111-2222", "서울시 종로구 1", "사랑해요");
        Order order = orders.placeOrder(MEMBER, List.of(new OrderLine(PANTS, PANTS_M, 1)), gift.recipientAddress(),
                gift, "pay-2", "계좌이체");

        assertTrue(order.isGift());
        assertEquals("엄마", order.getGiftInfo().recipientName());
    }

    @Test
    void findByMember_newestFirstAndOnlyMine() {
        Order first = place(new OrderLine(COAT, COAT_M, 1));
        Order second = place(new OrderLine(PANTS, PANTS_M, 1));
        orders.placeOrder(99L, List.of(new OrderLine(PANTS, PANTS_M, 1)), ADDRESS, null, "pay-3", "카드 결제");

        List<Order> mine = orders.findByMember(MEMBER);
        assertEquals(2, mine.size());
        assertEquals(second.getId(), mine.get(0).getId());
        assertEquals(first.getId(), mine.get(1).getId());
    }

    @Test
    void findByIdUnknown_throws() {
        assertThrows(AppException.class, () -> orders.findById(999L));
    }

    @Test
    void deliveryFlow_followsSteps() {
        Order order = delivered();

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals("6012-0000-1111", order.getTrackingNumber());
    }

    @Test
    void skippingDeliveryStep_throws() {
        Order order = place(new OrderLine(COAT, COAT_M, 1));

        assertThrows(AppException.class,
                () -> orders.updateStatus(order.getId(), OrderStatus.DELIVERED, null));
    }

    @Test
    void shippingWithoutTrackingNumber_throws() {
        Order order = place(new OrderLine(COAT, COAT_M, 1));
        orders.updateStatus(order.getId(), OrderStatus.PREPARING, null);

        assertThrows(AppException.class, () -> orders.updateStatus(order.getId(), OrderStatus.SHIPPING, " "));
    }

    @Test
    void cancelBeforeShipping_marksCanceled() {
        Order order = place(new OrderLine(COAT, COAT_M, 1));

        assertEquals(OrderStatus.CANCELED, orders.cancel(order.getId(), "단순 변심").getStatus());
        assertEquals("단순 변심", order.getReason());
    }

    @Test
    void cancelAfterShipping_throws() {
        Order order = delivered();

        assertThrows(AppException.class, () -> orders.cancel(order.getId(), "단순 변심"));
    }

    @Test
    void requestChangeBeforeDelivery_throws() {
        Order order = place(new OrderLine(COAT, COAT_M, 1));

        assertThrows(AppException.class,
                () -> orders.requestChange(order.getId(), OrderStatus.RETURN_REQUESTED, "사이즈"));
    }

    @Test
    void approveReturn_marksReturned() {
        Order order = delivered();
        orders.requestChange(order.getId(), OrderStatus.RETURN_REQUESTED, "사이즈가 작아요");

        assertEquals(OrderStatus.RETURNED, orders.approveChange(order.getId()).getStatus());
    }

    @Test
    void approveExchange_goesBackToPreparing() {
        Order order = delivered();
        orders.requestChange(order.getId(), OrderStatus.EXCHANGE_REQUESTED, "색상 교환");

        Order approved = orders.approveChange(order.getId());
        assertEquals(OrderStatus.PREPARING, approved.getStatus());
        assertNull(approved.getTrackingNumber());
    }

    @Test
    void rejectChange_backToDeliveredWithReason() {
        Order order = delivered();
        orders.requestChange(order.getId(), OrderStatus.RETURN_REQUESTED, "사이즈가 작아요");

        Order rejected = orders.rejectChange(order.getId(), "착용 흔적이 있어요");
        assertEquals(OrderStatus.DELIVERED, rejected.getStatus());
        assertEquals("착용 흔적이 있어요", rejected.getReason());
    }

    @Test
    void findByStatus_filtersAndNullMeansAll() {
        delivered();
        place(new OrderLine(PANTS, PANTS_M, 1));

        assertEquals(1, orders.findByStatus(OrderStatus.PAID).size());
        assertEquals(1, orders.findByStatus(OrderStatus.DELIVERED).size());
        assertEquals(2, orders.findByStatus(null).size());
    }

    @Test
    void activeOrder_blocksProductDelete() {
        Order order = place(new OrderLine(COAT, COAT_M, 1));
        assertTrue(productService.hasActiveOrders(COAT));

        productService.delete(COAT);
        assertEquals(Product.Status.SUSPENDED, productService.findById(COAT).getStatus());

        orders.cancel(order.getId(), "단순 변심");
        assertFalse(orders.hasActiveOrders(COAT));
    }

    @Test
    void clearOrderedFromCart_emptiesOrderedLines() {
        cartService.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 1), new OrderLine(PANTS, PANTS_M, 1)));

        orders.clearOrderedFromCart(MEMBER, List.of(new OrderLine(COAT, COAT_M, 1)));

        assertEquals(1, cartService.getCart(MEMBER).size());
    }
}
