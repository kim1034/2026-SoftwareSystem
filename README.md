# 사담다

40~60대 중장년 여성을 위한 의류 쇼핑 애플리케이션 (2026 소프트웨어시스템 Team 2)

- 기술 스택: Java 17, Swing, Maven
- 구조: MVC (Model / View / Controller)

## 실행

```bash
mvn package
java -jar target/sadamda-0.1.0-SNAPSHOT.jar
```

IDE에서는 `com.sadamda.App`의 `main`을 실행하면 됩니다.

## 패키지 구조

```
src/main/java/com/sadamda
├── App.java                  진입점
├── model/                    Model: 도메인 객체와 로직
│   ├── member/               회원·체형 정보          (백예원)
│   ├── cart/                 장바구니·찜              (백예원)
│   ├── order/                주문·선물·주문 상태      (백예원)
│   ├── review/               리뷰                     (백예원)
│   ├── product/              상품·카테고리·검색 조건  (정우진)
│   ├── recommend/            룩북·유행·맞춤 추천      (정우진)
│   └── tryon/                AI 합성 이미지 보관함    (정우진)
├── external/                 외부 시스템 연동: 결제(PG), AI 이미지 합성 (김대원)
├── controller/               Controller: 화면 요청 처리, Model·외부 연동 조합 (김대원)
│   └── AppContext.java       객체 생성·연결
└── view/                     View: Swing 화면 (강준서, 관리자 화면은 김대원)
    ├── MainFrame.java        메인 창, 상단 메뉴, 화면 전환
    ├── ScreenPanel.java      화면 공통 부모
    ├── FontSize.java         글씨 크기 조절 (UC-15)
    └── member/ product/ recommend/ tryon/ cart/ order/ admin/
```

## Use Case ↔ 코드 위치

| UC | 이름 | View | Controller | Model |
|----|------|------|------------|-------|
| UC-01 | 회원가입 | `SignUpPanel` | `MemberController` | `MemberService` |
| UC-02 | 로그인 | `LoginPanel` | `MemberController` | `MemberService` |
| UC-03 | 회원·체형 정보 관리 | `MyInfoPanel` | `MemberController` | `MemberService` |
| UC-04 | 카테고리별 상품 조회 | `ProductListPanel` | `ProductController` | `ProductService` |
| UC-05 | 상품 검색 및 필터 | `ProductListPanel` | `ProductController` | `ProductService` |
| UC-06 | 상품 상세 조회 | `ProductDetailPanel` | `ProductController` | `ProductService`, `ReviewService` |
| UC-07 | 룩북 코디 추천 보기 | `LookbookPanel` | `RecommendController` | `RecommendService` |
| UC-08 | 유행·맞춤 아이템 추천 보기 | `HomePanel` | `RecommendController` | `RecommendService` |
| UC-09 | AI 착용 이미지 생성 | `TryOnPanel` | `TryOnController` | `TryOnService`, `AiImageClient` |
| UC-10 | 장바구니·찜 관리 | `CartPanel` | `CartController` | `CartService` |
| UC-11 | 주문 및 결제 | `OrderPanel` | `OrderController` | `OrderService`, `PaymentGateway` |
| UC-12 | 선물하기 | `OrderPanel` | `OrderController` | `OrderService` (`GiftInfo`) |
| UC-13 | 주문·배송 조회 및 취소·교환·반품 | `OrderHistoryPanel` | `OrderController` | `OrderService`, `PaymentGateway` |
| UC-14 | 리뷰 작성 | `OrderHistoryPanel` | `ReviewController` | `ReviewService` |
| UC-15 | 글씨 크기 조절 | `MainFrame`, `FontSize` | - | - |
| UC-16 | 상품 관리 | `AdminPanel` | `AdminController` | `ProductService` |
| UC-17 | 룩북·유행 아이템 관리 | `AdminPanel` | `AdminController` | `RecommendService` |
| UC-18 | 주문 관리 | `AdminPanel` | `AdminController` | `OrderService`, `PaymentGateway` |

아직 구현되지 않은 메서드는 `UnsupportedOperationException("TODO UC-xx")`를 던집니다.
결제 시스템과 AI 합성 서비스는 실제 연동 전까지 `MockPaymentGateway`, `MockAiImageClient`를 씁니다.
