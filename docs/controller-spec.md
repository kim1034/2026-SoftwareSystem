# Controller 명세

View(강준서)와 Model(백예원·정우진)이 이 문서를 기준으로 동시에 개발합니다.
문서에 적힌 메서드 이름·인자·반환값은 바꾸기 전에 김대원과 먼저 이야기해 주세요.

- 웹 기준으로 쓰인 요구사항 문서의 "REST API 명세"를 Swing 앱에 맞게 바꾼 것입니다.
  화면은 HTTP 요청 대신 `AppContext`의 Controller 메서드를 직접 호출합니다.
- 코드: `src/main/java/com/sadamda/controller/`

## 1. 공통 규칙

### 호출 방법

```java
AppContext context = ...;            // ScreenPanel 의 context 필드
Product p = context.product.detail(productId);
```

### 오류 처리

Controller·Model 은 실패를 아래 예외로 알립니다 (`com.sadamda.common`). 모두 `RuntimeException` 입니다.

| 예외 | 의미 | View 가 할 일 |
|------|------|---------------|
| `LoginRequiredException` | 로그인이 필요한 기능을 비회원이 씀 | 로그인 화면(UC-02)으로 이동 → 로그인 후 원래 화면으로 돌아와 다시 호출 |
| `AccessDeniedException` | 관리자 기능을 일반 회원이 씀, 남의 주문 조회 | 메시지 표시 |
| `AppException` | 그 밖의 검증 실패·대안 흐름 | `getMessage()`를 큰 글씨로 그대로 표시 |

- 메시지는 사용자에게 그대로 보여줄 쉬운 한국어로 씁니다 (NFR-03). 예: "이미 가입된 이메일입니다."
- `UnsupportedOperationException("TODO ...")`은 아직 구현되지 않았다는 뜻입니다.

### 오래 걸리는 호출

아래 두 메서드는 외부 서비스를 기다리므로 Swing 화면 스레드(EDT)에서 바로 부르면 화면이 멈춥니다.
`SwingWorker`로 별도 스레드에서 호출하고 진행 상태를 표시하세요.

- `TryOnController.generate` — 최대 30초 (NFR-05)
- `OrderController.checkout` — 결제 시스템 응답 대기

### 로그인 필요 여부

- 로그인 불필요: `ProductController`, `RecommendController`, `MemberController`의 가입·로그인
- 로그인 필요: `CartController`, `OrderController`, `ReviewController`, `TryOnController`, `MemberController.myInfo/updateProfile`
- 관리자 필요: `AdminController` 전체

## 2. Controller 별 메서드

### MemberController — UC-01~03

| 메서드 | UC | 반환 | 실패 |
|--------|----|------|------|
| `signUp(SignUpForm form)` | UC-01 | 가입한 `Member` (바로 로그인됨) | 약관 미동의, 중복 이메일(4a), 형식 오류(4b) |
| `login(String email, String password)` | UC-02 | `Member` — role 이 ADMIN 이면 관리자 화면으로 | 계정 불일치(3a) |
| `logout()` | | | |
| `requestPasswordReset(String email)` | UC-02 2a | | |
| `currentMember()` | | 로그인한 `Member`, 비회원이면 `null` | |
| `myInfo()` | UC-03 | `Member` | 로그인 필요 |
| `updateProfile(String phone, String defaultAddress, BodyProfile bodyProfile)` | UC-03 | 수정된 `Member` | 형식 오류(4a) |

`SignUpForm(email, password, name, phone, bodyProfile, agreedToTerms)` — `bodyProfile`은 선택이라 `null` 가능.

### ProductController — UC-04~06

| 메서드 | UC | 반환 |
|--------|----|------|
| `listByCategory(Category category, int page)` | UC-04 | `List<Product>` — page 는 0부터, [더 보기]마다 +1 |
| `popular()` | UC-04 2a, UC-05 2a | 결과가 없을 때 보여줄 인기 상품 |
| `search(SearchFilter filter)` | UC-05 | `List<Product>` — 3a [내 체형에 맞는 상품]은 회원 체형 정보로 filter 를 채워 호출 |
| `detail(Long productId)` | UC-06 | `Product` |
| `reviews(Long productId)` | UC-06 | `List<Review>` |

### RecommendController — UC-07, UC-08

| 메서드 | UC | 반환 |
|--------|----|------|
| `lookbooks(String occasion, String season)` | UC-07 | `List<Lookbook>` — null 이면 전체 |
| `lookbook(Long lookbookId)` | UC-07 | `Lookbook` |
| `trendingItems()` | UC-08 | 이번 시즌 유행 아이템 |
| `recommendedForMe()` | UC-08 | 맞춤 상품, 비회원이면 빈 목록 |

[코디 전체 담기](UC-07 5단계)는 `CartController.addAll`을 씁니다.

### CartController — UC-10 (로그인 필요)

| 메서드 | UC | 반환 | 실패 |
|--------|----|------|------|
| `cart()` | UC-10 | `List<CartItem>` | |
| `total()` | UC-10 | 총액 (원) | |
| `add(OrderLine line)` | UC-06 [장바구니] | | 품절 |
| `addAll(List<OrderLine> lines)` | UC-07 [코디 전체 담기] | | 품절 포함(6a) |
| `update(Long cartItemId, Long optionId, int quantity)` | UC-10 3단계 | | 재고 초과(3a) |
| `remove(Long cartItemId)` | UC-10 3단계 | | |
| `toggleWish(Long productId)` | UC-10 1a | 찜 상태면 `true` | |
| `wishList()` | UC-10 1a | `List<Product>` | |

`OrderLine(productId, optionId, quantity)` — 장바구니 담기와 주문에 같이 씁니다.

### OrderController — UC-11~13 (로그인 필요, 담당: 김대원)

| 메서드 | UC | 반환 | 실패 |
|--------|----|------|------|
| `prepareFromCart()` | UC-10 [주문하기] → UC-11 | `OrderSheet` | 품절 |
| `prepareDirect(OrderLine line)` | UC-06 [바로 구매] → UC-11 | `OrderSheet` | 품절 |
| `checkout(CheckoutRequest request)` | UC-11, UC-12 | 완료된 `Order` | 결제 실패(6a), 재고 소진(7a, 결제 자동 취소) |
| `myOrders()` | UC-13 | `List<Order>` 최신순 | |
| `orderDetail(Long orderId)` | UC-13 | `Order` | 남의 주문 |
| `cancel(Long orderId, String reason)` | UC-13 4~5 | `Order` (취소됨) | 배송 중(4a) |
| `requestExchangeOrReturn(Long orderId, OrderStatus requested, String reason)` | UC-13 6 | `Order` | |

- `OrderSheet(lines, defaultAddress, totalPrice)` — 주문서 화면에 그대로 표시
- `CheckoutRequest(lines, shippingAddress, paymentMethod, giftInfo)` — [선물로 보내기]를 고르면 `giftInfo`를 채움 (UC-12), 아니면 `null`

### ReviewController — UC-14 (로그인 필요)

| 메서드 | 반환 | 실패 |
|--------|------|------|
| `write(Long orderId, Long productId, int rating, String content, List<String> photoPaths)` | `Review` | 별점 없음(3a, rating=0), 배송 완료 전 |

### TryOnController — UC-09 (로그인 필요, 담당: 김대원)

| 메서드 | 반환 | 실패 |
|--------|------|------|
| `backgroundTemplates()` | 템플릿 id 목록 | |
| `generate(TryOnRequest request)` | 합성 이미지 (PNG 바이트) | 동의 안 함(2a), 형식·용량(3a), 인물 인식 실패(7a), 시간 초과·오류(7b → [다시 시도] 표시) |
| `saveToLocker(Long productId, byte[] image)` | `TryOnImage` | |
| `locker()` | `List<TryOnImage>` | |
| `deleteFromLocker(Long tryOnImageId)` | | |

- `TryOnRequest(productId, personImage, personFileName, backgroundTemplateId, backgroundImage, photoUseAgreed)`
  - 배경은 `backgroundTemplateId`와 `backgroundImage` 중 하나만 채웁니다.
- 업로드 가능: JPG·PNG, 최대 10MB (`TryOnController.ALLOWED_EXTENSIONS`, `MAX_IMAGE_BYTES`)

### AdminController — UC-16~18 (관리자 필요, 담당: 김대원)

| 메서드 | UC | 반환 | 실패 |
|--------|----|------|------|
| `products()` | UC-16 | 판매 중지 포함 전체 | |
| `saveProduct(Product product)` | UC-16 | 저장된 `Product` (id 가 null 이면 등록) | 필수 항목 누락(5a) |
| `hasActiveOrders(Long productId)` | UC-16 3a | `true`면 "판매 중지로 바꿀까요?" 질문 | |
| `suspendProduct(Long productId)` | UC-16 3a | | |
| `deleteProduct(Long productId)` | UC-16 | | |
| `saveLookbook(Lookbook lookbook)` | UC-17 | 저장된 `Lookbook` | 판매 중지 상품 포함(2a) |
| `setTrending(Long productId, boolean trending)` | UC-17 | | |
| `orders(OrderStatus status)` | UC-18 | 상태별 목록, null 이면 전체 | |
| `updateOrderStatus(Long orderId, OrderStatus status, String trackingNumber)` | UC-18 3 | `Order` — SHIPPING 이면 송장번호 필수 | 잘못된 상태 변경 |
| `approveRequest(Long orderId)` | UC-18 4~5 | `Order` — 반품이면 환불 후 반영 | 환불 실패(5a, 재시도 안내) |
| `rejectRequest(Long orderId, String reason)` | UC-18 4 | `Order` | |

## 3. Controller 가 Model 에 요구하는 것

Controller 가 부르는 Model 메서드의 시그니처를 각 Service 에 정해 두었습니다. 본문은 `TODO`이고 Model 담당이 구현합니다.

- 실패는 `AppException`(사용자에게 보여줄 메시지)으로 알려 주세요.
- `Member`의 `getId()·getRole()·getDefaultAddress()`, `Order`의 `getId()·getMemberId()·getPaymentId()·getStatus()`는 Controller 가 쓰므로 이름을 유지해 주세요. 나머지 접근자·생성자는 자유롭게 추가하면 됩니다.
- 관리자 화면이 `Product`, `Lookbook`을 직접 만들어 저장하므로 두 클래스에 생성자나 setter 가 필요합니다 (정우진).

| Service | 담당 | Controller 가 쓰는 메서드 |
|---------|------|---------------------------|
| `MemberService` | 백예원 | `signUp(email, rawPassword, name, phone, bodyProfile)`, `login`, `sendPasswordResetLink`, `findById`, `updateProfile(memberId, phone, defaultAddress, bodyProfile)` |
| `CartService` | 백예원 | `getCart`, `getTotal`, `add(memberId, lines)`, `updateItem`, `removeItem`, `toggleWish`, `getWishList` |
| `OrderService` | 백예원 | `calculateTotal`, `placeOrder(memberId, lines, address, giftInfo, paymentId)`, `clearOrderedFromCart`, `findByMember`, `findById`, `cancel`, `requestChange`, `findByStatus`, `updateStatus`, `approveChange`, `rejectChange` |
| `ReviewService` | 백예원 | `write(memberId, orderId, productId, rating, content, photoPaths)`, `findByProduct` |
| `ProductService` | 정우진 | `findByCategory`, `findPopular`, `search`, `findById`, `findAll`, `save`, `hasActiveOrders`, `suspend`, `delete` |
| `RecommendService` | 정우진 | `findLookbooks`, `findLookbook`, `findTrendingItems`, `recommendFor`, `saveLookbook`, `setTrending` |
| `TryOnService` | 정우진 | `findBackgroundTemplates`, `loadBackgroundTemplate`, `save`, `findByMember`, `delete` |

## 4. 구현 현황

- 구현됨: 로그인·관리자 확인(`Session.requireLogin/requireAdmin`), Model 로 그대로 넘기는 메서드
- 김대원이 구현 예정 (`TODO ... (계획 N단계)`):
  - 결제 흐름: `OrderController.prepare*/checkout`
  - 주문 조회·취소: `myOrders/orderDetail/cancel/requestExchangeOrReturn`
  - AI 합성: `TryOnController.generate`
  - 반품 승인·환불: `AdminController.approveRequest`
