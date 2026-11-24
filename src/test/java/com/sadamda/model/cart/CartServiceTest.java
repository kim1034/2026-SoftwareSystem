package com.sadamda.model.cart;

import com.sadamda.common.AppException;
import com.sadamda.model.TestProducts;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.sadamda.model.TestProducts.COAT;
import static com.sadamda.model.TestProducts.COAT_L;
import static com.sadamda.model.TestProducts.COAT_M;
import static com.sadamda.model.TestProducts.COAT_PRICE;
import static com.sadamda.model.TestProducts.COAT_SOLD_OUT;
import static com.sadamda.model.TestProducts.PANTS;
import static com.sadamda.model.TestProducts.PANTS_M;
import static com.sadamda.model.TestProducts.PANTS_PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartServiceTest {
    private static final Long MEMBER = 7L;

    private final ProductService productService = TestProducts.service();
    private final CartService cart = new CartService(productService);

    @Test
    void add_thenCartAndTotalShowIt() {
        cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 2), new OrderLine(PANTS, PANTS_M, 1)));

        assertEquals(2, cart.getCart(MEMBER).size());
        assertEquals(COAT_PRICE * 2 + PANTS_PRICE, cart.getTotal(MEMBER));
    }

    @Test
    void addSameOption_mergesIntoOneLine() {
        cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 1)));
        cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 2)));

        List<CartItem> items = cart.getCart(MEMBER);
        assertEquals(1, items.size());
        assertEquals(3, items.get(0).quantity());
    }

    @Test
    void addMoreThanStock_throwsAndKeepsCart() {
        cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 4)));

        AppException e = assertThrows(AppException.class,
                () -> cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 2))));
        assertTrue(e.getMessage().contains("최대 5개"));
        assertEquals(4, cart.getCart(MEMBER).get(0).quantity());
    }

    @Test
    void addSoldOutOption_throws() {
        AppException e = assertThrows(AppException.class,
                () -> cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_SOLD_OUT, 1))));
        assertTrue(e.getMessage().contains("품절"));
    }

    @Test
    void addAllWithOneSoldOut_addsNothing() {
        assertThrows(AppException.class, () -> cart.add(MEMBER,
                List.of(new OrderLine(PANTS, PANTS_M, 1), new OrderLine(COAT, COAT_SOLD_OUT, 1))));
        assertTrue(cart.getCart(MEMBER).isEmpty());
    }

    @Test
    void addSuspendedProduct_throws() {
        productService.suspend(COAT);

        assertThrows(AppException.class, () -> cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 1))));
    }

    @Test
    void addZeroQuantity_throws() {
        assertThrows(AppException.class, () -> cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 0))));
    }

    @Test
    void updateItemToOptionAlreadyInCart_mergesLines() {
        cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 1), new OrderLine(COAT, COAT_L, 1)));
        Long mLine = cart.getCart(MEMBER).get(0).id();

        cart.updateItem(MEMBER, mLine, COAT_L, 1);

        List<CartItem> items = cart.getCart(MEMBER);
        assertEquals(1, items.size());
        assertEquals(COAT_L, items.get(0).optionId());
        assertEquals(2, items.get(0).quantity());
    }

    @Test
    void updateItemOverStock_throws() {
        cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_L, 1)));
        Long line = cart.getCart(MEMBER).get(0).id();

        assertThrows(AppException.class, () -> cart.updateItem(MEMBER, line, COAT_L, 3));
    }

    @Test
    void removeItem_removesOnlyThatLine() {
        cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 1), new OrderLine(PANTS, PANTS_M, 1)));
        Long first = cart.getCart(MEMBER).get(0).id();

        cart.removeItem(MEMBER, first);

        assertEquals(1, cart.getCart(MEMBER).size());
        assertEquals(PANTS_M, cart.getCart(MEMBER).get(0).optionId());
    }

    @Test
    void cartsAreSeparatedByMember() {
        cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 1)));

        assertTrue(cart.getCart(99L).isEmpty());
    }

    @Test
    void removeOrdered_removesOrderedOptions() {
        cart.add(MEMBER, List.of(new OrderLine(COAT, COAT_M, 1), new OrderLine(PANTS, PANTS_M, 1)));

        cart.removeOrdered(MEMBER, List.of(new OrderLine(COAT, COAT_M, 1)));

        assertEquals(1, cart.getCart(MEMBER).size());
        assertEquals(PANTS_M, cart.getCart(MEMBER).get(0).optionId());
    }

    @Test
    void toggleWish_addsThenRemoves() {
        assertTrue(cart.toggleWish(MEMBER, COAT));
        assertTrue(cart.isWished(MEMBER, COAT));

        assertFalse(cart.toggleWish(MEMBER, COAT));
        assertFalse(cart.isWished(MEMBER, COAT));
        assertTrue(cart.getWishList(MEMBER).isEmpty());
    }

    @Test
    void wishList_newestFirst() {
        cart.toggleWish(MEMBER, COAT);
        cart.toggleWish(MEMBER, PANTS);

        List<Product> wished = cart.getWishList(MEMBER);
        assertEquals(PANTS, wished.get(0).getId());
        assertEquals(COAT, wished.get(1).getId());
    }

    @Test
    void toggleWishUnknownProduct_throws() {
        assertThrows(AppException.class, () -> cart.toggleWish(MEMBER, 999L));
    }
}
