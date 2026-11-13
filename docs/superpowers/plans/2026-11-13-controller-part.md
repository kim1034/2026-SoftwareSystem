# 김대원 파트 (Controller — 외부 연동·관리자) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 결제(UC-11·12), AI 착용 이미지(UC-09), 주문 조회·취소(UC-13), 관리자 기능(UC-16~18)의 Controller 로직과 관리자 화면을 구현한다.

**Architecture:** Controller 는 `docs/controller-spec.md`에 정한 시그니처 그대로 구현한다. 외부 서비스(`PaymentGateway`, `AiImageClient`)는 Mock 구현으로 먼저 개발하고, Model Service 는 테스트에서 Mockito 목으로 대신한다. 관리자 화면은 `view/admin`의 Swing 탭 3개로 만든다.

**Tech Stack:** Java 17, Swing, Maven, JUnit 5.11.3, Mockito 5.14.2 (테스트 전용)

**Spec:** `사담다_Requirement_UseCase.pdf` (UC-09, 11, 12, 13, 16, 17, 18 / FR-13~15, 19~22, 25~27 / NFR-04, 05, 08, 09), `docs/controller-spec.md`

**전제:** PR #2 (Controller 명세)가 main 에 머지된 상태에서 시작한다.

## Global Constraints

- Java 17, Swing. 런타임 의존성 추가 금지 (테스트 의존성만 허용).
- 사용자에게 보이는 메시지는 쉬운 한국어, 외래어 대신 우리말 (NFR-03). `AppException` 메시지는 화면에 그대로 표시된다.
- 본문 최소 18px, 버튼 글씨 20px 이상, 버튼 최소 48×48px (NFR-01, NFR-02) — 관리자 화면에도 적용.
- AI 합성 결과는 30초 안에 제공, 실패 시 재시도 버튼 (NFR-05).
- AI 합성·결제 장애가 다른 기능에 퍼지지 않는다: 외부 호출의 어떤 예외도 Controller 밖으로 `AppException` 이외의 형태로 나가지 않는다 (NFR-09).
- 업로드한 인물 사진은 합성 목적으로만 쓰고 저장하지 않는다 (NFR-08).
- 다른 담당자 파일(`model/**`)은 이 계획에 적힌 getter 추가 외에는 수정하지 않는다.

## 테스트 실행 환경

이 PC 에는 JDK·Maven 이 설치되어 있지 않아 임시 폴더에 받아서 쓴다 (Task 1). 이후 모든 명령은 Git Bash 기준이다.

```bash
export JAVA_HOME="$TEMP/claude/jdk17/jdk-17.0.21+9"
MVN="$TEMP/claude/maven/apache-maven-3.9.9/bin/mvn"
```

## 브랜치

| 브랜치 | Task | PR 제목 |
|--------|------|---------|
| `feat/order-payment` | 1, 2, 3 | 주문·결제 (UC-11, UC-12) |
| `feat/tryon` | 4 | AI 착용 이미지 생성 (UC-09) |
| `feat/order-manage` | 5, 6 | 주문 조회·취소, 관리자 주문 처리 (UC-13, UC-18) |
| `feat/admin-ui` | 7, 8, 9 | 관리자 화면 (UC-16~18) |

각 PR 은 직전 PR 이 머지된 main 에서 브랜치를 딴다.

## Review Focus

1. **결제는 됐는데 주문 저장이 실패** — `placeOrder`가 품절 외의 이유(`RuntimeException`)로 실패해도 결제를 취소해야 한다. 취소마저 실패하면 사용자에게 "결제 취소에 실패했습니다" 안내가 나가야 한다. → Task 3 `placeOrderFailsWithAnyException_cancelsPayment`, `cancelAlsoFails_tellsUserToContactUs`
2. **[결제하기] 두 번 누름** — 결제가 진행 중일 때 다시 호출하면 두 번째 결제를 하지 않는다. → Task 3 `secondCheckoutWhileInProgress_isRejected`
3. **AI 서비스가 응답 없이 멈춤** — 30초(테스트에서는 짧게) 뒤 재시도 안내가 나가고, 다음 시도는 정상 진행된다. → Task 4 `clientHangs_timesOutWithRetryableException`
4. **결제 시스템이 거절 대신 예외를 던짐** (네트워크 오류) — 거절과 같이 처리하고 주문을 만들지 않는다. → Task 3 `gatewayThrows_noOrderCreated`
5. **사진 파일 이름** — `PHOTO.JPG`(대문자)는 통과, `photo.jpg.exe`·확장자 없음·`null`은 거절. → Task 4 `fileNameCases`

---

### Task 1: 테스트 환경 구성

**Files:**
- Modify: `pom.xml`
- Create: `src/test/java/com/sadamda/controller/SessionTest.java`

**Interfaces:**
- Produces: `mvn test`로 JUnit 5 + Mockito 테스트 실행 가능

- [ ] **Step 1: 임시 JDK·Maven 준비**

```bash
[ -x "$TEMP/claude/jdk17/jdk-17.0.21+9/bin/java" ] || { mkdir -p "$TEMP/claude/jdk17" && cd "$TEMP/claude/jdk17" && curl -sSL -o jdk.zip "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" && unzip -q jdk.zip && rm jdk.zip; }
mkdir -p "$TEMP/claude/maven" && cd "$TEMP/claude/maven" && curl -sSL -o mvn.zip "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" && unzip -q -o mvn.zip && rm mvn.zip
```

Expected: `"$MVN" -v` 가 `Apache Maven 3.9.9`, `Java version: 17` 출력

- [ ] **Step 2: `pom.xml`에 테스트 의존성 추가**

`<dependencies>`: `org.junit.jupiter:junit-jupiter:5.11.3` (test), `org.mockito:mockito-core:5.14.2` (test).
`<plugins>`: `maven-surefire-plugin` 3.5.2.

- [ ] **Step 3: `SessionTest` 작성**

```java
@Test void guest_requireLogin_throws() { assertThrows(LoginRequiredException.class, () -> new Session().requireLogin()); }
@Test void member_requireAdmin_throwsAccessDenied() { /* role MEMBER 인 mock Member 로 login → requireAdmin → AccessDeniedException */ }
@Test void admin_requireAdmin_returnsMember() { /* role ADMIN → 같은 Member 반환, isAdmin() == true */ }
```

`Member`는 `mock(Member.class)`에 `getId()`, `getRole()`을 stub 해서 만든다 (이후 모든 Task 동일).

- [ ] **Step 4: 실행**

Run: `"$MVN" -q test` — Expected: `Tests run: 3, Failures: 0`

- [ ] **Step 5: Commit** — `git commit -m "test: add JUnit 5 and Mockito test setup"`

---

### Task 2: 주문서 만들기 (UC-11 1~2단계)

**Files:**
- Modify: `src/main/java/com/sadamda/controller/OrderSheet.java`, `CheckoutRequest.java`, `OrderController.java`, `docs/controller-spec.md`
- Test: `src/test/java/com/sadamda/controller/OrderControllerPrepareTest.java`

**Interfaces:**
- Consumes: `CartService.getCart(Long) -> List<CartItem>`, `OrderService.calculateTotal(List<OrderLine>) -> int`, `MemberService.findById(Long) -> Member`
- Produces:
  - `record OrderSheet(List<OrderLine> lines, String defaultAddress, int totalPrice, boolean fromCart)`
  - `record CheckoutRequest(List<OrderLine> lines, String shippingAddress, String paymentMethod, GiftInfo giftInfo, boolean fromCart)`
  - `OrderController.prepareFromCart() -> OrderSheet`, `prepareDirect(OrderLine) -> OrderSheet`

`fromCart`를 추가하는 이유: 결제 후 장바구니에서 비울지 정해야 한다. [바로 구매]한 상품을 장바구니에서 지우면 안 된다.

- [ ] **Step 1: 실패하는 테스트**

```java
@Test void fromCart_buildsSheetWithCartLinesTotalAndDefaultAddress()
    // CartItem(1L,1L,10L,100L,2) → lines == [OrderLine(10L,100L,2)], totalPrice == calculateTotal 반환값,
    // defaultAddress == findById(..).getDefaultAddress(), fromCart == true
@Test void fromCart_emptyCart_throws()      // AppException "장바구니가 비어 있습니다."
@Test void direct_buildsSheetWithOneLine()  // fromCart == false
@Test void direct_quantityBelowOne_throws() // AppException "수량을 1개 이상 골라 주세요."
@Test void guest_throwsLoginRequired()
```

- [ ] **Step 2: 실행해서 실패 확인** — `"$MVN" -q test -Dtest=OrderControllerPrepareTest` → FAIL (UnsupportedOperationException)

- [ ] **Step 3: 구현**
  - record 두 개에 `fromCart` 필드를 추가한다.
  - `prepareFromCart`, `prepareDirect`를 구현한다. 기본 배송지는 세션에 있는 객체가 아니라 `memberService.findById`로 새로 읽는다.
  - `controller-spec.md`의 OrderSheet·CheckoutRequest 설명을 수정한다.

- [ ] **Step 4: 실행해서 통과 확인** — 같은 명령 → PASS

- [ ] **Step 5: Commit** — `feat: build order sheet from cart or direct purchase (UC-11)`

---

### Task 3: 결제 (UC-11 4~8단계, UC-12 선물하기)

**Files:**
- Modify: `src/main/java/com/sadamda/controller/OrderController.java`
- Test: `src/test/java/com/sadamda/controller/OrderControllerCheckoutTest.java`

**Interfaces:**
- Consumes: `PaymentGateway.approve(long, String) -> PaymentResult`, `PaymentGateway.cancel(String) -> PaymentResult`, `OrderService.calculateTotal`, `OrderService.placeOrder(Long, List<OrderLine>, String, GiftInfo, String) -> Order`, `OrderService.clearOrderedFromCart(Long, List<OrderLine>)`
- Produces: `OrderController.checkout(CheckoutRequest) -> Order`

**흐름** (이 순서를 지킨다):
1. 로그인 확인
2. 입력 검증
3. `calculateTotal`
4. `approve`
5. `placeOrder` — 실패하면 `cancel`
6. `fromCart`이면 `clearOrderedFromCart`
7. 주문 반환

배송지: `giftInfo`가 있으면 `giftInfo.recipientAddress()`를, 없으면 `shippingAddress`를 `placeOrder`에 넘긴다.

**메시지** (`OrderController`의 상수로 둔다):

| 상황 | 메시지 |
|------|--------|
| 상품 없음 | "주문할 상품이 없습니다." |
| 배송지 비어 있음 | "배송지를 입력해 주세요." |
| 결제 수단 없음 | "결제 수단을 골라 주세요." |
| 선물 수령인 정보 비어 있음 | "받는 분의 이름, 연락처, 주소를 모두 입력해 주세요." |
| 선물 연락처 형식 오류 | "받는 분 연락처를 숫자로 정확히 입력해 주세요." — 숫자와 `-`만 허용, 숫자 10~11자리 |
| 결제 거절 (6a) | `"결제가 완료되지 않았습니다. " + result.message()` |
| 결제 시스템 예외 | "결제 시스템에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요." |
| 저장 실패 후 결제 취소 성공 (7a) | `placeOrder`가 던진 `AppException`이면 그 메시지 + " 결제는 취소되었습니다.", 그 밖의 예외면 "주문을 저장하지 못해 결제를 취소했습니다." |
| 저장 실패 후 결제 취소도 실패 | "결제 취소에 실패했습니다. 고객센터로 연락해 주세요." |
| 결제 진행 중 재호출 | "결제가 진행 중입니다. 잠시만 기다려 주세요." |

- [ ] **Step 1: 실패하는 테스트**

```java
@Test void success_returnsOrder_andClearsCartWhenFromCart()      // approve(총액, "카드") 호출, placeOrder 에 paymentId 전달, clearOrderedFromCart 호출
@Test void success_direct_doesNotClearCart()                     // fromCart=false → clearOrderedFromCart 호출 안 함
@Test void gift_shipsToRecipientAddress()                        // placeOrder 의 주소 인자 == giftInfo.recipientAddress()
@Test void gift_badPhone_throwsBeforePayment()                   // "010-12" → AppException, approve 호출 안 함
@Test void declined_throwsWithReason_noOrderCreated()            // approved=false, message "한도 초과" → 메시지에 "한도 초과" 포함, placeOrder 호출 안 함
@Test void gatewayThrows_noOrderCreated()                        // approve 가 RuntimeException → AppException, placeOrder 호출 안 함
@Test void soldOutDuringPayment_cancelsPayment()                 // placeOrder 가 AppException("품절") → cancel(paymentId) 호출, 메시지 끝이 " 결제는 취소되었습니다."
@Test void placeOrderFailsWithAnyException_cancelsPayment()      // placeOrder 가 IllegalStateException → cancel 호출
@Test void cancelAlsoFails_tellsUserToContactUs()                // cancel 이 approved=false → "결제 취소에 실패했습니다. 고객센터로 연락해 주세요."
@Test void secondCheckoutWhileInProgress_isRejected()            // approve stub 안에서 controller.checkout(같은 요청) 을 다시 부르고 그 AppException 메시지를 기록 → "결제가 진행 중입니다. 잠시만 기다려 주세요."
@Test void afterFailure_canCheckoutAgain()                       // 거절 뒤 다시 호출하면 approve 가 다시 불린다 (진행 중 표시가 풀렸는지)
```

- [ ] **Step 2: 실행해서 실패 확인** — `"$MVN" -q test -Dtest=OrderControllerCheckoutTest` → FAIL

- [ ] **Step 3: 구현**
  - 중복 결제 방지는 `AtomicBoolean checkoutInProgress`로 한다. `compareAndSet(false, true)`가 실패하면 거절하고, `finally`에서 해제한다.
  - 결제 취소 실패는 `System.getLogger(OrderController.class.getName())`에 ERROR 로 기록한다. 기록에는 paymentId 와 회원 id 를 남긴다.

- [ ] **Step 4: 실행해서 통과 확인** — 같은 명령 → PASS, 이어서 `"$MVN" -q test` 전체 PASS

- [ ] **Step 5: Commit + PR** — `feat: checkout with payment approval and rollback (UC-11, UC-12)`. `feat/order-payment` PR 생성

---

### Task 4: AI 착용 이미지 생성 (UC-09)

**Files:**
- Create: `src/main/java/com/sadamda/common/RetryableException.java`
- Modify: `src/main/java/com/sadamda/external/AiImageClient.java`, `src/main/java/com/sadamda/controller/TryOnController.java`, `src/main/java/com/sadamda/controller/AppContext.java`, `src/main/java/com/sadamda/model/product/Product.java` (getter 1개), `docs/controller-spec.md`
- Test: `src/test/java/com/sadamda/controller/TryOnControllerTest.java`

**Interfaces:**
- Consumes:
  - `ProductService.findById(Long) -> Product`
  - `TryOnService.loadBackgroundTemplate(String) -> byte[]`
  - `AiImageClient.synthesize(byte[], byte[], String) -> byte[]`
- Produces:
  - `class RetryableException extends AppException`: View 가 [다시 시도] 버튼을 보여주는 예외. Task 5, 6 에서도 쓴다.
  - `AiImageClient.AiImageException`에 `enum Reason { PERSON_NOT_FOUND, FAILED }`와 생성자 `AiImageException(Reason, String)`, `reason()`을 추가한다.
  - `Product.getMainImageUrl() -> String`: `imageUrls`의 첫 번째, 없으면 `null`.
  - `TryOnController(Session, AiImageClient, TryOnService, ProductService, Duration timeout)`
    - `AppContext`는 `Duration.ofSeconds(AiImageClient.TIMEOUT_SECONDS)`를 넘긴다.
  - `TryOnController.generate(TryOnRequest) -> byte[]`

**검증과 메시지** (요청 검증은 이 순서로, AI 호출 결과는 마지막 세 줄):

| 상황 | 예외 | 메시지 |
|------|------|--------|
| 사진 이용 동의 안 함 (2a) | `AppException` | "사진 이용에 동의해야 AI 입어보기를 쓸 수 있습니다." |
| 인물 사진 비어 있음 | `AppException` | "인물 사진을 올려 주세요." |
| 형식 오류 (3a) | `AppException` | "JPG 또는 PNG 사진만 올릴 수 있습니다." — 마지막 `.` 뒤 확장자를 소문자로 바꿔 `ALLOWED_EXTENSIONS`에 있는지 확인, 이름이 `null`이거나 `.`이 없으면 거절 |
| 용량 초과 (3a) | `AppException` | "사진 용량은 10MB 이하여야 합니다." — 인물 사진, 직접 올린 배경 사진 모두 확인 |
| 배경 미선택 | `AppException` | "배경을 골라 주세요." — 템플릿 id 와 배경 사진이 둘 다 없음 |
| 인물 인식 실패 (7a) | `AppException` | "사진에서 사람을 찾지 못했습니다. 전신이 나온 다른 사진을 올려 주세요." — `Reason.PERSON_NOT_FOUND` |
| 시간 초과·기타 오류 (7b) | `RetryableException` | "이미지를 만들지 못했습니다. 다시 시도해 주세요." — 그 밖의 모든 예외(`RuntimeException` 포함) |
| 결과가 `null` | `RetryableException` | 위와 같은 메시지 |

- [ ] **Step 1: 실패하는 테스트**

```java
@Test void success_passesPersonTemplateAndClothingUrl()   // loadBackgroundTemplate("beach") 결과와 product.getMainImageUrl() 이 synthesize 에 전달, 결과 반환
@Test void uploadedBackground_usedInsteadOfTemplate()     // 배경 사진을 올리면 loadBackgroundTemplate 호출 안 함
@Test void notAgreed_throws_andAiNotCalled()
@ParameterizedTest @CsvSource({"PHOTO.JPG,true","a.png,true","photo.jpg.exe,false","photo,false"})
void fileNameCases(String name, boolean ok)
@Test void nullFileName_rejected()
@Test void tooLarge_rejected()                            // MAX_IMAGE_BYTES + 1 바이트
@Test void noBackground_rejected()
@Test void personNotFound_mapsTo7aMessage()
@Test void clientThrowsRuntime_retryable()                // NFR-09
@Test void clientHangs_timesOutWithRetryableException()   // timeout 100ms, client 는 5초 sleep → 1초 안에 RetryableException
@Test void afterTimeout_nextCallWorks()                   // 위 직후 정상 client 응답으로 다시 호출하면 성공
@Test void guest_throwsLoginRequired()
```

- [ ] **Step 2: 실행해서 실패 확인** — `"$MVN" -q test -Dtest=TryOnControllerTest` → FAIL (컴파일 오류 포함)

- [ ] **Step 3: 구현**
  - AI 호출은 `Executors.newCachedThreadPool`(데몬 스레드)에 제출하고 `future.get(timeout)`으로 기다린다. 시간이 넘으면 `future.cancel(true)`.
  - 인물 사진은 필드·파일에 남기지 않는다 (NFR-08).
  - `controller-spec.md`의 오류 처리 표에 `RetryableException`을 추가한다.

- [ ] **Step 4: 실행해서 통과 확인** — 같은 명령 → PASS, 전체 `"$MVN" -q test` PASS

- [ ] **Step 5: Commit + PR** — `feat: AI try-on generation with timeout and retry (UC-09)`. PR 본문에 정우진 리뷰 요청: `Product.getMainImageUrl`

---

### Task 5: 주문 조회·취소·교환·반품 (UC-13)

**Files:**
- Modify: `src/main/java/com/sadamda/common/AccessDeniedException.java` (메시지를 받는 생성자 추가), `src/main/java/com/sadamda/controller/OrderController.java`
- Test: `src/test/java/com/sadamda/controller/OrderControllerManageTest.java`

**Interfaces:**
- Consumes: `RetryableException` (Task 4), `OrderService.findByMember / findById / cancel / requestChange`, `PaymentGateway.cancel`
- Produces: `AccessDeniedException(String message)`, `OrderController.myOrders / orderDetail / cancel / requestExchangeOrReturn` (시그니처는 controller-spec 그대로)

**규칙:**
- 본인 주문만 볼 수 있다. 남의 주문이면 `AccessDeniedException("본인 주문만 볼 수 있습니다.")`.
- 취소 가능 상태는 `PAID`, `PREPARING`.
  - `SHIPPING`·`DELIVERED`이면 "이미 배송이 시작되어 취소할 수 없습니다. 배송 완료 후 반품을 신청해 주세요." (4a)
  - 그 밖의 상태이면 "취소할 수 없는 주문입니다."
- 취소 순서: `paymentGateway.cancel(paymentId)` → 성공하면 `orderService.cancel`.
  - 결제 취소가 거절되거나 예외가 나면 `RetryableException("결제 취소에 실패했습니다. 잠시 후 다시 시도해 주세요.")`. 이때 `orderService.cancel`은 호출하지 않는다.
- 교환·반품 신청:
  - `requested`가 `EXCHANGE_REQUESTED`·`RETURN_REQUESTED`가 아니면 `IllegalArgumentException` (View 의 프로그래밍 오류).
  - 상태가 `DELIVERED`가 아니면 "배송 완료된 주문만 교환·반품을 신청할 수 있습니다."
  - 사유가 비어 있으면 "사유를 골라 주세요."

- [ ] **Step 1: 실패하는 테스트**

```java
@Test void myOrders_returnsServiceResultForCurrentMember()
@Test void detail_otherMembersOrder_accessDenied()
@Test void cancel_paid_cancelsPaymentThenOrder()            // InOrder 로 순서 검증
@Test void cancel_shipping_throws4aMessage_noPaymentCall()
@Test void cancel_alreadyCanceled_throws()
@Test void cancel_paymentCancelFails_retryable_orderUntouched()
@Test void cancel_gatewayThrows_retryable()
@Test void returnRequest_delivered_ok()
@Test void returnRequest_notDelivered_throws()
@Test void returnRequest_blankReason_throws()
```

- [ ] **Step 2: 실행해서 실패 확인** — `"$MVN" -q test -Dtest=OrderControllerManageTest` → FAIL
- [ ] **Step 3: 구현** — 위 규칙대로. 본인 주문 확인은 private 메서드 `ownedOrder(Long orderId) -> Order` 하나로 모은다.
- [ ] **Step 4: 실행해서 통과 확인** — 같은 명령 → PASS
- [ ] **Step 5: Commit** — `feat: order history, cancel and exchange/return requests (UC-13)`

---

### Task 6: 관리자 주문 처리 (UC-18)

**Files:**
- Modify: `src/main/java/com/sadamda/controller/AdminController.java`
- Test: `src/test/java/com/sadamda/controller/AdminControllerOrderTest.java`

**Interfaces:**
- Consumes: `RetryableException`, `OrderService.findById / updateStatus / approveChange / rejectChange`, `PaymentGateway.cancel`
- Produces: `AdminController.updateOrderStatus`, `approveRequest`, `rejectRequest` (시그니처는 controller-spec 그대로)

**규칙:**
- `updateOrderStatus`: `SHIPPING`인데 송장번호가 비어 있으면 "송장번호를 입력해 주세요." 상태 전이 검증은 Model 이 맡는다.
- `approveRequest`:
  - `RETURN_REQUESTED`이면 환불(`paymentGateway.cancel`) → 성공하면 `approveChange`. 실패·예외면 ERROR 로그를 남기고 `RetryableException("환불 요청에 실패했습니다. 다시 시도해 주세요.")` (5a).
  - `EXCHANGE_REQUESTED`이면 바로 `approveChange`.
  - 그 밖의 상태이면 "처리할 교환·반품 요청이 없습니다."
- `rejectRequest`: 사유가 비어 있으면 "거절 사유를 입력해 주세요."
- 모든 메서드는 비관리자 호출 시 `AccessDeniedException`.

- [ ] **Step 1: 실패하는 테스트**

```java
@Test void shipping_withoutTracking_throws()
@Test void approveReturn_refundsThenApproves()          // InOrder
@Test void approveReturn_refundFails_retryable_notApproved()
@Test void approveExchange_noRefund()
@Test void approveOtherStatus_throws()
@Test void reject_blankReason_throws()
@Test void nonAdmin_accessDenied()
```

- [ ] **Step 2: 실행해서 실패 확인** — `"$MVN" -q test -Dtest=AdminControllerOrderTest` → FAIL
- [ ] **Step 3: 구현**
- [ ] **Step 4: 실행해서 통과 확인** — 같은 명령, 이어서 전체 → PASS
- [ ] **Step 5: Commit + PR** — `feat: admin order status and exchange/return handling (UC-18)`. `feat/order-manage` PR 생성

---

### 관리자 화면 공통 (Task 7~9)

**선행 조건:** 정우진이 `Product`·`Lookbook`에 아래 접근자를 추가해야 시작할 수 있다. 없으면 이 이름으로 추가해 달라고 요청한다.
- `Product`: `getId, getName/setName, getCategory/setCategory, getPrice/setPrice, getMaterial/setMaterial, getOptions/setOptions, getImageUrls/setImageUrls, getAgeGroupTags/setAgeGroupTags, getBodyTypeTags/setBodyTypeTags, getStatus, isTrending`
- `Lookbook`: `getId, getTitle/setTitle, getImageUrl/setImageUrl, getOccasion/setOccasion, getSeason/setSeason, getProductIds/setProductIds`

**화면 규칙:**
- 글씨는 `FontSize`를 따르고, 버튼은 `setPreferredSize` 최소 48×48 (NFR-01, 02).
- `AppException`은 `JOptionPane.showMessageDialog`로 메시지를 그대로 보여준다.
- `RetryableException`이면 [다시 시도]·[닫기] 두 버튼을 보여준다.
- 결제 시스템을 부르는 `approveRequest`만 `SwingWorker`로 호출한다.

**검증:** Swing 화면은 자동 테스트 대신 수동 점검표로 확인한다. Model 이 아직 TODO 면 점검을 미루고 PR 에 그렇게 적는다.

### Task 7: 상품 관리 탭 (UC-16)

**Files:**
- Modify: `src/main/java/com/sadamda/view/admin/AdminPanel.java` (`JTabbedPane`에 탭 3개)
- Create: `src/main/java/com/sadamda/view/admin/ProductAdminTab.java`, `src/main/java/com/sadamda/view/admin/ProductFormDialog.java`

**Interfaces:**
- Consumes: `AdminController.products / saveProduct / hasActiveOrders / suspendProduct / deleteProduct`
- Produces: `AdminPanel`이 `ProductAdminTab`, `LookbookAdminTab`(Task 8), `OrderAdminTab`(Task 9)을 탭으로 보여준다. Task 8·9 전에는 그 자리에 빈 `JPanel`을 둔다.

**화면 구성:**
- 목록: `JTable` (이름, 카테고리, 가격, 상태)
- 버튼: [상품 등록] [수정] [삭제] [새로 고침]
- [삭제] 동작:
  1. `hasActiveOrders`가 true 이면 "진행 중인 주문이 있습니다. 판매 중지로 바꿀까요?" 확인 창을 띄운다.
  2. 예 → `suspendProduct`
  3. 주문이 없으면 "정말 삭제할까요?" 확인 창 → `deleteProduct`
- 등록·수정 폼 (`ProductFormDialog`) 입력 항목:
  - 상품명, 카테고리(`Category` 콤보), 가격, 소재
  - 이미지 경로 (`JFileChooser`, 여러 개)
  - 연령대·체형 태그 (쉼표 구분)
  - 옵션 표 (사이즈·색상·재고)

- [ ] **Step 1: 탭과 폼 구현**
- [ ] **Step 2: 컴파일** — `"$MVN" -q compile` → 성공
- [ ] **Step 3: 수동 점검** (`"$MVN" -q package -DskipTests` 후 `"$JAVA_HOME/bin/java" -jar target/sadamda-0.1.0-SNAPSHOT.jar`, 또는 IDE 에서 `App` 실행)
  - [ ] 관리자 로그인 후 상품 목록이 보인다
  - [ ] 등록한 상품이 목록과 사용자 상품 화면에 나온다
  - [ ] 필수 항목을 비우면 Model 메시지가 뜬다 (5a)
  - [ ] 주문이 있는 상품을 삭제하면 판매 중지를 묻는다 (3a)
  - [ ] 글씨 크기를 '아주 크게'로 바꿔도 표가 잘리지 않는다
- [ ] **Step 4: Commit** — `feat: admin product management tab (UC-16)`

### Task 8: 룩북·유행 아이템 탭 (UC-17)

**Files:**
- Create: `src/main/java/com/sadamda/view/admin/LookbookAdminTab.java`
- Modify: `src/main/java/com/sadamda/view/admin/AdminPanel.java`

**Interfaces:**
- Consumes: `AdminController.saveLookbook / setTrending / products`, `RecommendController.lookbooks(null, null)`

**화면 구성:**
- 위쪽 — 코디 등록:
  - 입력: 제목, 이미지(`JFileChooser`), 상황(모임/여행/일상 콤보), 계절(봄/여름/가을/겨울 콤보), 구성 상품(상품 목록 다중 선택)
  - [저장] 버튼
  - 아래에 등록된 코디 목록
- 아래쪽 — 유행 아이템: 상품 표에 '유행' 체크박스 열을 두고, 체크를 바꾸면 바로 `setTrending`

- [ ] **Step 1: 구현**
- [ ] **Step 2: 컴파일** — `"$MVN" -q compile` → 성공
- [ ] **Step 3: 수동 점검**
  - [ ] 저장한 코디가 룩북 화면에 나온다
  - [ ] 판매 중지 상품을 넣으면 경고가 뜬다 (2a)
  - [ ] 유행 지정한 상품이 홈 화면 '이번 시즌 유행 아이템'에 나온다
- [ ] **Step 4: Commit** — `feat: admin lookbook and trending tab (UC-17)`

### Task 9: 주문 관리 탭 (UC-18)

**Files:**
- Create: `src/main/java/com/sadamda/view/admin/OrderAdminTab.java`
- Modify: `src/main/java/com/sadamda/view/admin/AdminPanel.java`

**Interfaces:**
- Consumes: `AdminController.orders / updateOrderStatus / approveRequest / rejectRequest` (Task 6)

**화면 구성:**
- 맨 위 상태 필터 콤보: 전체 + `OrderStatus` 값
- 주문 표: 주문번호, 주문일, 금액, 상태, 선물 여부
- 버튼:
  - [배송 준비] [배송 시작]: 배송 시작은 송장번호 입력 창을 먼저 띄운다
  - [배송 완료]
  - [요청 승인] [요청 거절]: 거절은 사유 입력 창을 먼저 띄운다
- 선택한 주문 상태에 맞지 않는 버튼은 비활성화한다.

- [ ] **Step 1: 구현**
- [ ] **Step 2: 컴파일** — `"$MVN" -q compile` → 성공
- [ ] **Step 3: 수동 점검**
  - [ ] 배송 시작 후 회원의 주문 내역에 '배송중'과 송장번호가 보인다 (UC-18 사후조건)
  - [ ] 반품 승인 시 Mock 결제 취소가 호출되고 상태가 바뀐다
  - [ ] 환불 실패(Mock 을 실패하도록 잠시 바꿔서) 시 [다시 시도]가 뜬다
- [ ] **Step 4: Commit + PR** — `feat: admin order management tab (UC-18)`. `feat/admin-ui` PR 생성

---

## 이 계획에 포함하지 않은 것

- **실제 결제 시스템·AI API 연동**: 어떤 서비스를 쓸지 팀이 정한 뒤 별도 계획으로 진행한다. `MockPaymentGateway`, `MockAiImageClient`를 실제 구현으로 바꾸는 일이다.
- **관리자 로그인 후 관리자 화면으로 이동, 관리자 메뉴 표시** (UC-02 4단계): `MainFrame` 담당(강준서). `Session.isAdmin()`을 쓰면 된다고 전달한다.
- **AI 입어보기·주문서·주문 내역 화면** (UC-09, 11~13 의 View): 강준서 담당. 이 계획의 Controller 를 호출한다.
- **마이페이지 화면 인수** (UC-03, 13): 일정이 밀릴 때만 받기로 한 조건부 작업이다.
- **응답 시간 측정** (NFR-04, 3초·2초): 이 계획의 Controller 는 외부 호출 외에 무거운 작업이 없다. 측정은 통합 테스트(책임: 정우진)에서 한다.
