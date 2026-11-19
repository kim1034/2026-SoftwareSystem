package com.sadamda.controller;

import com.sadamda.common.AppException;
import com.sadamda.model.cart.CartService;
import com.sadamda.model.family.AskRequest;
import com.sadamda.model.family.AskService;
import com.sadamda.model.member.Member;
import com.sadamda.model.member.MemberService;
import com.sadamda.model.order.GiftInfo;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderLine;

import java.util.List;

/**
 * 가족에게 물어보기 (사담다의 차별 기능).
 * 중장년 회원이 고른 옷을 가족(딸·아들)에게 "이거 어때?" 묻고, 가족은 좋아요/다른 걸 추천해요와 한마디로 답하거나
 * 대신 결제해 준다. 대신 결제는 선물하기(UC-12)와 같은 방식으로, 묻는 사람의 기본 배송지로 보낸다.
 * 모든 메서드 로그인 필요.
 */
public class FamilyController {
    private final Session session;
    private final AskService askService;
    private final MemberService memberService;
    private final CartService cartService;
    private final OrderController orderController;

    public FamilyController(Session session, AskService askService, MemberService memberService,
                            CartService cartService, OrderController orderController) {
        this.session = session;
        this.askService = askService;
        this.memberService = memberService;
        this.cartService = cartService;
        this.orderController = orderController;
    }

    /** 상품 하나(상세 화면에서 고른 옵션·수량)를 가족에게 묻는다. */
    public AskRequest ask(String familyEmail, List<OrderLine> lines, String message) {
        Member me = session.requireLogin();
        Member family = findFamily(familyEmail);
        return askService.ask(me.getId(), family.getId(), family.getEmail(), lines, message);
    }

    /** 장바구니 전체를 가족에게 묻는다. */
    public AskRequest askCart(String familyEmail, String message) {
        Long memberId = session.requireLogin().getId();
        List<OrderLine> lines = cartService.getCart(memberId).stream()
                .map(item -> new OrderLine(item.productId(), item.optionId(), item.quantity())).toList();
        return ask(familyEmail, lines, message);
    }

    /** 내가 물어본 것, 최신순. 열어 보면 새 답 표시를 지운다. */
    public List<AskRequest> myAsks() {
        Long memberId = session.requireLogin().getId();
        List<AskRequest> list = askService.askedBy(memberId);
        askService.markSeen(memberId);
        return list;
    }

    /** 가족이 나에게 물어본 것, 최신순. */
    public List<AskRequest> asksToMe() {
        return askService.askedTo(session.requireLogin().getId());
    }

    /** 좋아요(liked) 또는 다른 걸 추천해요, 한마디(선택). */
    public AskRequest respond(Long requestId, boolean liked, String reply) {
        return askService.respond(session.requireLogin().getId(), requestId, liked, reply);
    }

    /**
     * 대신 결제하기: 묻는 사람에게 보내는 선물 주문으로 결제한다 (받는 분 = 묻는 사람, 기본 배송지).
     * 결제 시스템 응답을 기다리므로 View 는 별도 스레드에서 호출한다.
     */
    public Order payFor(Long requestId, String paymentMethod, String reply) {
        Long me = session.requireLogin().getId();
        AskRequest request = askService.checkPayable(me, requestId);
        Member requester = memberService.findById(request.getRequesterId());
        if (requester.getDefaultAddress() == null || requester.getDefaultAddress().isBlank()) {
            throw new AppException(requester.getName() + "님의 기본 배송지가 없어요. 배송지를 등록해 달라고 말씀드려 주세요.");
        }
        GiftInfo gift = new GiftInfo(requester.getName(), requester.getPhone(), requester.getDefaultAddress(),
                reply == null || reply.isBlank() ? "가족이 대신 결제했어요" : reply.trim());
        Order order = orderController.checkout(new CheckoutRequest(request.getLines(),
                requester.getDefaultAddress(), paymentMethod, gift, false));
        askService.markPaid(me, requestId, order.getId(), reply);
        return order;
    }

    /** 결제 전에 보여 줄 금액 (상품 + 배송비). */
    public int totalOf(AskRequest request) {
        session.requireLogin();
        return orderController.prepareLinesTotal(request.getLines());
    }

    /** 상단 메뉴의 새 소식 수. 비회원이면 0. */
    public int newsCount() {
        return session.isLoggedIn() ? askService.newsCount(session.currentMember().getId()) : 0;
    }

    /** 지난번에 물어본 가족 이메일 (물어보기 창에 미리 채운다). 없으면 빈 문자열. */
    public String lastFamilyEmail() {
        String email = askService.lastFamilyEmail(session.requireLogin().getId());
        return email == null ? "" : email;
    }

    /** 화면에 보여 줄 회원 이름. */
    public String nameOf(Long memberId) {
        return memberService.findById(memberId).getName();
    }

    private Member findFamily(String email) {
        if (email == null || email.isBlank()) {
            throw new AppException("가족의 이메일을 적어 주세요.");
        }
        Member family = memberService.findByEmailOrNull(email);
        if (family == null) {
            throw new AppException("사담다에 가입한 가족의 이메일을 적어 주세요. (" + email.trim() + " 은 가입되어 있지 않아요)");
        }
        return family;
    }
}
