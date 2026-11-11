package com.sadamda.controller;

import com.sadamda.external.MockAiImageClient;
import com.sadamda.external.MockPaymentGateway;
import com.sadamda.external.AiImageClient;
import com.sadamda.external.PaymentGateway;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.recommend.RecommendService;
import com.sadamda.model.review.ReviewService;
import com.sadamda.model.tryon.TryOnService;

/** Model·외부 연동·Controller 객체를 한곳에서 생성해 View 에 넘긴다. */
public class AppContext {
    public final Session session = new Session();

    public final MemberController member;
    public final ProductController product;
    public final RecommendController recommend;
    public final TryOnController tryOn;
    public final CartController cart;
    public final OrderController order;
    public final ReviewController review;
    public final AdminController admin;

    public AppContext() {
        PaymentGateway paymentGateway = new MockPaymentGateway(); // TODO: 실제 PG 연동으로 교체
        AiImageClient aiImageClient = new MockAiImageClient();    // TODO: 실제 AI API 연동으로 교체

        MemberService memberService = new MemberService();
        ProductService productService = new ProductService();
        RecommendService recommendService = new RecommendService();
        CartService cartService = new CartService();
        OrderService orderService = new OrderService();
        ReviewService reviewService = new ReviewService();
        TryOnService tryOnService = new TryOnService();

        member = new MemberController(session, memberService);
        product = new ProductController(productService, reviewService);
        recommend = new RecommendController(session, recommendService);
        tryOn = new TryOnController(session, aiImageClient, tryOnService);
        cart = new CartController(session, cartService);
        order = new OrderController(session, orderService, paymentGateway);
        review = new ReviewController(reviewService);
        admin = new AdminController(session, productService, recommendService, orderService, paymentGateway);
    }
}
