# 통합: 화면을 실제 Model 로 연결 (2026-11-19)

브랜치 `feat/integrate-real-models`. #14(화면)와 #15(장바구니·주문·리뷰 Model)를 합친 뒤, 화면이 쓰던 목업을 실제 Controller 로 바꿨습니다.

## 바뀐 것

| 화면 | 전에는 | 지금은 |
|------|--------|--------|
| `CartPanel` (UC-10) | `MockCart` | `context.cart.cart()/update()/remove()`, [주문하기] → `context.order.prepareFromCart()` |
| `ProductDetailPanel` (UC-06) | `MockCart.add`, `MockOrders.prepareDirect`, `MockReviews.reviews` | `context.cart.add()`, `context.order.prepareDirect()`, `context.product.reviews()` |
| `OrderPanel` (UC-11·12) | `MockOrders.checkout` | `context.order.checkout()` → 실제 `Order` |
| `OrderHistoryPanel` (UC-13) | `MockOrders.ordersOf/cancel/requestExchangeOrReturn` | `context.order.myOrders()/orderDetail()/cancel()/requestExchangeOrReturn()` |
| `ReviewWritePanel` (UC-14) | `MockReviews.write/written` | `context.review.write()/hasWritten()` |

- 지운 파일: `view/cart/MockCart.java`, `view/order/MockOrders.java`, `view/review/MockReviews.java`, `view/product/MockProducts.java`
- `ProductDetail.of(Product)`를 `public`으로 바꿔 화면들이 `context.product.detail(id)` 결과를 바로 씁니다.
- `OrderPanel.itemRow`는 `ProductController`를 받습니다. 주문 내역은 `OrderItem.unitPrice()`(주문할 때의 가격)로 금액을 보여 줍니다.
- `OrderPanel.SHIPPING_FEE`는 `OrderService.SHIPPING_FEE`를 그대로 씁니다.

## 새로 넣은 것

- `ReviewController.hasWritten(orderId, productId)`: 리뷰 쓰기 화면에서 이미 쓴 상품을 "작성함"으로 막을 때 씁니다.
- `Session.onLogin(...)`: 로그인할 때 부를 일을 정합니다 (패키지 안에서만).
- `controller/DemoData` + `AppContext.demo`: **시연용.** 관리자 화면(UC-18)이 아직 없어서 "배송 중/배송 완료" 주문을 만들 수 없으므로, 일반 회원이 처음 로그인할 때 예전 주문 2건(배송 중 1, 배송 완료 1)을 실제 `OrderService`에 넣습니다. 관리자 화면이 생기면 지워도 됩니다.

## 달라진 동작

- **결제 방법:** `Order`에는 결제 방법이 없어서 주문 완료 화면은 방금 고른 값을 보여 주고, 주문 상세에서는 "결제 방법" 줄을 뺐습니다. 필요하면 Model(`Order`)에 필드를 추가해야 합니다.
- **데모 리뷰:** `MockProducts`에 있던 데모 리뷰 6개는 사라졌습니다. 상품 상세에는 리뷰 쓰기로 쓴 리뷰만 보입니다.
- **테스트용 실패 스위치:** `MockOrders.failNextPayment/failNextCancel`이 없어졌습니다. 결제 실패는 `MockPaymentGateway`에서 확인해야 합니다.

## 확인한 것

로그인 → 장바구니 담기 → [주문하기] → [결제하기] → 장바구니 비워짐 → 주문 내역(새 주문 + 시연용 2건) → 리뷰 쓰기 → 상품 상세에 리뷰 표시 → 결제완료 주문 취소. 실제 앱 창을 띄워 순서대로 실행하고 화면을 캡처해 확인했습니다. JUnit 테스트는 Maven 이 없어 돌리지 못했습니다.

## 남은 UC 구현 (2026-11-19 추가)

### UC-16·17·18 관리자 화면 (`view/admin`)
관리자(`admin@sadamda.com` / `admin1234`)로 로그인하면 상단 [관리자] 메뉴로 들어갑니다. 탭 3개:

| 탭 | 할 수 있는 일 | 쓰는 Controller |
|----|---------------|-----------------|
| 상품 관리 (UC-16) | 상품명·카테고리로 찾기, [새 상품 등록]·[수정](줄 두 번 누르기도 됨), [판매 중지]/[판매 재개], [삭제] — 진행 중인 주문이 있으면 판매 중지를 권함 (3a), 빠진 필수 항목은 Model 문장 그대로 안내 (5a) | `admin.products/saveProduct/suspendProduct/hasActiveOrders/deleteProduct` |
| 룩북·유행 아이템 (UC-17) | 코디 등록·수정·삭제 (상품 고르기 창), 판매 중지 상품이 섞이면 경고 (2a), 유행 아이템 지정·해제 | `admin.saveLookbook/deleteLookbook/setTrending`, `recommend.lookbooks` |
| 주문 관리 (UC-18) | 상태별 목록, 배송준비 → 배송중(송장번호 입력) → 배송완료, 교환·반품 승인(반품은 환불, 실패하면 다시 시도 5a)·거절(사유) | `admin.orders/updateOrderStatus/approveRequest/rejectRequest` |

고른 주문의 상태에서 할 수 있는 버튼만 눌립니다. 파일: `AdminPanel`, `ProductAdminTab`, `ProductEditDialog`, `LookbookAdminTab`, `OrderAdminTab`, `ProductPicker`, `AdminUi`.

### UC-10 1a 찜
- 상품 상세에 [♡ 찜하기] 버튼 (다시 누르면 해제). 비회원이면 로그인 화면으로.
- 장바구니 화면 아래에 "찜한 상품 n개" (상세 보기 / 찜 해제).
- `CartController.isWished(productId)` 추가.

### UC-15 4단계 글씨 크기 계정 저장
- `Member.fontSize` 필드와 `MemberController.saveFontSize(name)` 추가.
- [글씨 크기]로 바꾸면 이 PC 와 (회원이면) 계정에 저장하고, 로그인하면 계정에 저장된 크기로 맞춥니다 (`MainFrame.applyAccountFontSize`).

### 시연용 계정
- 일반 회원 `demo@sadamda.com` / `demo1234` (김사담, 50대·160cm·M(66)·보통 체형, 기본 배송지 있음). 앱을 켤 때 `DemoData.createDemoMember`가 만듭니다.
- 일반 회원이 처음 로그인하면 시연용 주문 2건(배송 중·배송 완료)이 들어갑니다.

### 확인한 것
실제 앱 창으로: 데모 회원 로그인 → 찜 → 장바구니의 찜 목록 → 반품 신청 → 관리자 로그인 → 상품 등록(필수 항목 누락 안내 포함)·판매 중지 → 판매 중지 상품이 든 코디 저장 거절 → 코디 저장·유행 지정 → 배송완료 처리 → 반품 승인(환불). 13개 확인 항목 모두 통과, 화면 캡처로 확인.
