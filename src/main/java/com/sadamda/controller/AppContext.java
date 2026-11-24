package com.sadamda.controller;

import com.sadamda.external.MockAiImageClient;
import com.sadamda.external.MockPaymentGateway;
import com.sadamda.external.AiImageClient;
import com.sadamda.external.DressUpClients;
import com.sadamda.external.DressUpImageClient;
import com.sadamda.external.PaymentGateway;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.family.AskService;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.recommend.RecommendService;
import com.sadamda.model.review.DemoReviews;
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
    public final DressUpController dressUp; // 옷 입혀보기 (정우진)
    /** 가족에게 물어보기 (사담다 차별 기능). */
    public final FamilyController family;
    /** 시연용 회원·예전 주문 (DemoData). 실제 서비스로 낼 때는 지운다. */
    public final DemoData demo;

    public AppContext() {
        PaymentGateway paymentGateway = new MockPaymentGateway(); // TODO: 실제 PG 연동으로 교체
        AiImageClient aiImageClient = new MockAiImageClient();    // TODO: 실제 AI API 연동으로 교체
        // 옷 입혀보기: 환경 변수나 화면의 [AI 연결 설정]에 이용 열쇠가 있으면 AI 로 합성 (DressUpClients 참고)
        DressUpImageClient dressUpClient = DressUpClients.create();

        MemberService memberService = new MemberService();
        DemoData.createDemoMember(memberService); // 시연용 일반 회원 demo@sadamda.com / demo1234
        ProductService productService = new ProductService();
        RecommendService recommendService = new RecommendService(productService);
        CartService cartService = new CartService(productService);
        OrderService orderService = new OrderService(productService, cartService);
        ReviewService reviewService = new ReviewService(orderService, memberService);
        DemoReviews.seed(reviewService, productService); // 시연용 리뷰. 실제 서비스로 낼 때는 지운다.
        TryOnService tryOnService = new TryOnService();

        member = new MemberController(session, memberService);
        product = new ProductController(productService, reviewService);
        recommend = new RecommendController(session, recommendService);
        tryOn = new TryOnController(session, aiImageClient, tryOnService);
        cart = new CartController(session, cartService);
        order = new OrderController(session, orderService, cartService, memberService, paymentGateway);
        review = new ReviewController(session, reviewService);
        admin = new AdminController(session, productService, recommendService, orderService, paymentGateway);
        dressUp = new DressUpController(session, dressUpClient, productService);
        family = new FamilyController(session, new AskService(), memberService, cartService, order);
        demo = new DemoData(orderService, productService);
        // 로그인하자마자 넣어야 시연용 주문이 회원이 새로 한 주문보다 아래(예전 주문)에 놓인다.
        session.onLogin(member -> {
            if (member != null && member.getRole() != com.sadamda.model.member.Member.Role.ADMIN
                    && !DemoData.FAMILY_EMAIL.equals(member.getEmail())) {
                demo.seedOrdersOnce(member.getId());
            }
        });
    }
}
