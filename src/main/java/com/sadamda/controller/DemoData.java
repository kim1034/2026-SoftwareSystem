package com.sadamda.controller;

import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.order.OrderStatus;
import com.sadamda.model.order.OrderService;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.product.ProductService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 시연용 데이터. 앱을 켜자마자 "배송 중"·"배송 완료" 주문을 볼 수 있게 (관리자가 배송 처리를 하지 않아도),
 * 회원이 처음 로그인할 때 예전 주문 2건(배송 중 1, 배송 완료 1)을 실제 OrderService 에 넣어 준다.
 * 배송 완료 주문으로 리뷰 쓰기(UC-14)와 교환·반품(UC-13 6단계)을 바로 시연할 수 있다.
 * 실제 서비스로 낼 때는 이 클래스와 AppContext.demo 를 지운다.
 */
public class DemoData {
    /** 시연용 일반 회원. 관리자 계정은 MemberService.ADMIN_EMAIL / ADMIN_PASSWORD. */
    public static final String DEMO_EMAIL = "demo@sadamda.com";
    public static final String DEMO_PASSWORD = "demo1234";
    /** 시연용 가족(딸) 회원. 엄마(demo)가 "가족에게 물어보기"로 보낸 옷을 받아 답하거나 대신 결제한다. */
    public static final String FAMILY_EMAIL = "daughter@sadamda.com";
    private static final String ADDRESS = "서울시 종로구 세종대로 1, 101동 101호";

    private final OrderService orderService;
    private final ProductService productService;
    private final Set<Long> seeded = new HashSet<>();

    DemoData(OrderService orderService, ProductService productService) {
        this.orderService = orderService;
        this.productService = productService;
    }

    /**
     * 시연용 회원을 만든다: 엄마 demo@sadamda.com / demo1234 (체형 정보·기본 배송지 포함)와
     * 딸 daughter@sadamda.com / demo1234 (가족에게 물어보기 시연용).
     */
    static void createDemoMember(MemberService memberService) {
        Member demo = memberService.signUp(DEMO_EMAIL, DEMO_PASSWORD, "김사담", "010-1234-5678",
                new BodyProfile("50대", 160, "M (66)", "보통 체형"));
        demo.setDefaultAddress(ADDRESS);
        Member daughter = memberService.signUp(FAMILY_EMAIL, DEMO_PASSWORD, "김지은", "010-2345-6789", null);
        daughter.setDefaultAddress("서울시 마포구 월드컵로 10, 202호");
    }

    /** 이 회원에게 아직 넣지 않았으면 예전 주문 2건을 넣는다. 데모 상품을 못 찾으면 조용히 건너뛴다. */
    public synchronized void seedOrdersOnce(Long memberId) {
        if (memberId == null || !seeded.add(memberId)) {
            return;
        }
        try {
            Order shipping = place(memberId, List.of(line("TOP-KR-02")), "DEMO-PAY-1");
            orderService.updateStatus(shipping.getId(), OrderStatus.PREPARING, null);
            orderService.updateStatus(shipping.getId(), OrderStatus.SHIPPING, "6012-3456-7890");

            Order delivered = place(memberId, List.of(line("BOT-KR-03"), line("BOT-KR-09")), "DEMO-PAY-2");
            orderService.updateStatus(delivered.getId(), OrderStatus.PREPARING, null);
            orderService.updateStatus(delivered.getId(), OrderStatus.SHIPPING, "6012-1111-2222");
            orderService.updateStatus(delivered.getId(), OrderStatus.DELIVERED, null);
        } catch (RuntimeException e) {
            System.getLogger(DemoData.class.getName())
                    .log(System.Logger.Level.WARNING, "시연용 주문을 넣지 못했습니다: " + e.getMessage());
        }
    }

    private Order place(Long memberId, List<OrderLine> lines, String paymentId) {
        return orderService.placeOrder(memberId, lines, ADDRESS, null, paymentId, "카드 결제");
    }

    /** 데모 데이터 상품(코드로 찾음)의 재고 있는 첫 옵션 1개. */
    private OrderLine line(String code) {
        Product product = productService.findAll().stream().filter(p -> code.equals(p.getCode())).findFirst()
                .orElseThrow(() -> new IllegalStateException("데모 데이터에 상품 " + code + " 이(가) 없습니다"));
        ProductOption option = product.getOptions().stream().filter(o -> o.stock() > 0).findFirst()
                .orElseThrow(() -> new IllegalStateException(code + " 의 재고가 없습니다"));
        return new OrderLine(product.getId(), option.id(), 1);
    }
}
