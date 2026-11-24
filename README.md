# 사담다

40~60대 중장년 여성을 위한 의류 쇼핑 애플리케이션 (2026 소프트웨어시스템 Team 2)

큰 글씨와 큰 버튼으로 상품을 찾아보고(카테고리·검색·필터), 연령대·체형에 맞는 추천과 룩북 코디를 보고, 캐릭터에 옷을 입혀 본 뒤 장바구니·주문·선물하기·리뷰까지 할 수 있습니다. 관리자는 상품·룩북·주문을 관리합니다.

- 기술 스택: Java 17, Swing, Maven
- 구조: MVC (Model / View / Controller)

## 실행

```bash
mvn package
java -jar target/sadamda-0.1.0-SNAPSHOT.jar
```

`pom.xml`의 `maven-jar-plugin`에 `Main-Class: com.sadamda.App`이 들어 있고, 실행할 때 필요한 외부 라이브러리가 없어서(JUnit·Mockito는 테스트용) 위 jar 파일 하나로 바로 실행됩니다.

IDE에서는 `com.sadamda.App`의 `main`을 실행하면 됩니다.

- 상품 사진은 쇼핑몰 이미지 주소에서 받아 오므로 **인터넷 연결**이 필요합니다. 연결이 없으면 "사진 없음"으로 나오고 나머지는 동작합니다.
- 데이터는 **메모리에만** 저장됩니다. 앱을 끄면 가입한 회원, 장바구니, 주문, 리뷰, 관리자가 바꾼 상품·룩북이 모두 사라집니다. 상품은 켤 때마다 `src/main/resources/data/products.tsv`(370개)에서 다시 읽습니다.
- 결제는 늘 승인하는 가짜 결제 시스템(`MockPaymentGateway`)을 씁니다.
- 옷 입혀보기의 AI 그림은 이용 열쇠(API 키)를 넣어야 동작합니다. 방법은 [docs/product-recommend.md](docs/product-recommend.md)를 보세요.

## 시연용 계정

앱을 켤 때마다 만들어집니다.

| 계정 | 이메일 | 비밀번호 | 비고 |
|------|--------|----------|------|
| 일반 회원 | `demo@sadamda.com` | `demo1234` | 김사담, 50대·160cm·M (66)·보통 체형, 기본 배송지 있음 (`controller/DemoData`) |
| 가족(딸) | `daughter@sadamda.com` | `demo1234` | 김지은. 엄마(demo)가 [가족에게 물어보기]로 보낸 옷에 답하거나 대신 결제 (`controller/DemoData`) |
| 관리자 | `admin@sadamda.com` | `admin1234` | 로그인하면 관리자 화면으로 이동, 상단에 [관리자] 메뉴 (`MemberService`) |

일반 회원이 처음 로그인하면 주문 내역에 시연용 예전 주문 2건(배송중 1건, 배송완료 1건)이 들어갑니다. 배송완료 주문으로 리뷰 쓰기와 교환·반품을 바로 해 볼 수 있습니다.

## 사담다만의 기능: 가족에게 물어보기

중장년 회원은 옷을 고른 뒤 딸·아들에게 "이거 어때?" 하고 물어보고, 결제도 가족이 대신 해 주는 경우가 많습니다. 사담다는 이 과정을 서비스 안으로 가져왔습니다.

1. **엄마**: 상품 상세(사이즈·색상 선택 후)나 장바구니에서 **[가족에게 물어보기]** → 가족 이메일과 하고 싶은 말
2. **딸**: 로그인하면 "가족 쇼핑에 새 소식" 알림 → **[가족 쇼핑]**에서 상품을 보고 **[좋아요]·[다른 걸 추천해요]** + 한마디, 또는 **[대신 결제해 드리기]** (엄마의 기본 배송지로 선물 주문, UC-12 재사용)
3. **엄마**: 상단 "가족 쇼핑 n" → 딸의 답 확인, 좋다고 한 옷은 **[장바구니에 담기]**, 대신 결제했으면 "김지은님이 대신 결제해 줬어요!"

시연: `demo@sadamda.com`으로 물어보기 → 로그아웃 → `daughter@sadamda.com`으로 답·결제 → 다시 `demo`로 확인. 자세한 내용은 [docs/family.md](docs/family.md).

## Use Case 구현 현황

| UC | 이름 | View | Controller | Model | 상태 |
|----|------|------|------------|-------|------|
| UC-01 | 회원가입 | `SignUpPanel` | `MemberController` | `MemberService` | 완료 |
| UC-02 | 로그인 | `LoginPanel` | `MemberController` | `MemberService` | 부분 — "로그인 상태 유지"(FR-02) 체크박스가 아직 동작하지 않음 |
| UC-03 | 회원·체형 정보 관리 | `MyInfoPanel` | `MemberController` | `MemberService` | 완료 |
| UC-04 | 카테고리별 상품 조회 | `ProductListPanel`, `HomePanel` | `ProductController` | `ProductService` | 완료 |
| UC-05 | 상품 검색 및 필터 | `ProductListPanel` | `ProductController` | `ProductService` | 완료 — 결과 없음(2a)의 검색어 제안은 화면에 정해 둔 목록 |
| UC-06 | 상품 상세 조회 | `ProductDetailPanel` | `ProductController` | `ProductService`, `ReviewService` | 완료 — 사이즈표는 모든 상품 공통 |
| UC-07 | 룩북 코디 추천 보기 | `LookbookPanel` | `RecommendController` | `RecommendService` | 완료 |
| UC-08 | 유행·맞춤 아이템 추천 보기 | `HomePanel` | `RecommendController` | `RecommendService` | 완료 |
| UC-09 | AI 착용 이미지 생성 | `TryOnPanel`, `PhotoUploadDialog`, `ItemPickerDialog` | `DressUpController`, `TryOnController` | `model/tryon`, `TryOnService` | 완료 — 캐릭터 4종 또는 **내 사진**에 옷 14칸을 입히고 배경 20장 중 하나로 바꿈 (`DressUpController`, 정우진). `TryOnController.generate`(김대원)는 TODO로 남아 있고 화면은 쓰지 않음, PR #4(`feat/tryon`)는 결정 전 |
| UC-10 | 장바구니·찜 관리 | `CartPanel`, `ProductDetailPanel` | `CartController` | `CartService` | 완료 (찜 1a 포함) |
| UC-11 | 주문 및 결제 | `OrderPanel` | `OrderController` | `OrderService`, `PaymentGateway` | 완료 — 결제는 `MockPaymentGateway` |
| UC-12 | 선물하기 | `OrderPanel` | `OrderController` | `OrderService` (`GiftInfo`) | 완료 |
| UC-13 | 주문·배송 조회 및 취소·교환·반품 | `OrderHistoryPanel` | `OrderController` | `OrderService`, `PaymentGateway` | 완료 — 주문 상세에 결제 방법이 없음 (`Order`에 필드 없음) |
| UC-14 | 리뷰 작성 | `ReviewWritePanel` | `ReviewController` | `ReviewService` | 완료 — 체형별 리뷰 보기(FR-23)는 나중에 |
| UC-15 | 글씨 크기 조절 | `MainFrame`, `FontSize` | `MemberController.saveFontSize` | `Member.fontSize` | 완료 (회원이면 계정에도 저장) |
| UC-16 | 상품 관리 | `AdminPanel` (`ProductAdminTab`) | `AdminController` | `ProductService` | 완료 |
| UC-17 | 룩북·유행 아이템 관리 | `AdminPanel` (`LookbookAdminTab`), `LookbookPanel` | `AdminController` | `RecommendService` | 완료 |
| UC-18 | 주문 관리 | `AdminPanel` (`OrderAdminTab`) | `AdminController` | `OrderService`, `PaymentGateway` | 완료 |

### 아직 남은 것

- `TryOnController.generate`가 `UnsupportedOperationException("TODO UC-09 (계획 4단계)")`를 던집니다. 사진 합성 화면은 `DressUpController.dressUpPhoto`로 동작하므로 이 메서드를 지울지, `dressUpPhoto`를 부르게 할지 정해야 합니다.
- FR-02 로그인 상태 유지 (`LoginPanel`의 TODO).
- `Order`에 결제 방법 필드가 없어 주문 상세에 결제 방법이 나오지 않습니다.
- 예전 목업에 있던 데모 리뷰는 없어졌습니다. 상품 상세에는 리뷰 쓰기로 쓴 리뷰만 보입니다.
- 외부 연동: 결제는 가짜 `MockPaymentGateway`입니다. 옷 입혀보기·내 사진 합성은 이용 열쇠를 넣으면 실제 AI(타임리 등)로 동작하고, `TryOnController`용 `MockAiImageClient`는 가짜 그대로입니다.
- Model Service 단위 테스트가 없습니다. 통합한 PC에는 Maven이 없어 JUnit 테스트를 돌려 보지 못했습니다.

## 패키지 구조

```
src/main/java/com/sadamda
├── App.java                  진입점
├── model/                    Model: 도메인 객체와 로직 (메모리 저장)
│   ├── member/               회원·체형 정보·비밀번호 해시   (백예원)
│   ├── cart/                 장바구니·찜                     (백예원)
│   ├── order/                주문·선물·주문 상태             (백예원)
│   ├── review/               리뷰                            (백예원)
│   ├── product/              상품·카테고리·검색 조건·데모 데이터 읽기 (정우진)
│   ├── recommend/            룩북·유행·맞춤 추천             (정우진)
│   └── tryon/                옷 입혀보기 캐릭터·코디, 합성 이미지 보관함 (정우진)
├── common/                   공통 예외 (AppException, LoginRequiredException 등)
├── external/                 외부 시스템 연동: 결제(PG), AI 사진 합성 (김대원), 옷 입혀보기 AI (정우진)
├── controller/               Controller: 화면 요청 처리, Model·외부 연동 조합 (김대원)
│   ├── AppContext.java       객체 생성·연결
│   ├── Session.java          로그인 상태
│   ├── DressUpController.java 옷 입혀보기 (정우진)
│   └── DemoData.java         시연용 회원·주문
└── view/                     View: Swing 화면 (강준서, 홈·룩북·옷 입혀보기는 정우진, 관리자 화면은 김대원)
    ├── MainFrame.java        메인 창, 상단 메뉴, 화면 전환
    ├── ScreenPanel.java      화면 공통 부모
    ├── Theme.java            공통 디자인 (색, 입력칸, 버튼, 카드)
    ├── FontSize.java         글씨 크기 조절 (UC-15)
    ├── member/               회원가입·로그인·내 정보
    ├── product/              상품 목록·검색·상세
    ├── recommend/            홈·룩북
    ├── tryon/                옷 입혀보기·보관함
    ├── cart/                 장바구니·찜
    ├── order/                주문서·주문 완료·주문 내역
    ├── review/               리뷰 쓰기
    └── admin/                관리자 (상품·룩북·주문 관리 탭)

src/main/resources
├── data/products.tsv         데모 상품 370개
├── data/backgrounds.tsv      내 사진으로 입어보기 배경 20장 (구글 시트 "background")
└── characters/               옷 입혀보기 캐릭터 그림

src/test/java/com/sadamda
├── controller/               OrderController·AdminController·Session JUnit 테스트
└── DomainSelfCheck.java      상품·추천·보관함·옷 입혀보기 실행형 점검
```

## 문서

| 문서 | 내용 |
|------|------|
| [docs/family.md](docs/family.md) | 사담다 차별 기능: 가족에게 물어보기 (화면 흐름, Controller·Model, 시연 순서) |
| [docs/controller-spec.md](docs/controller-spec.md) | Controller 메서드, 오류 처리 규칙, 구현 현황 |
| [docs/integration.md](docs/integration.md) | 화면을 실제 Model로 연결한 통합, 관리자 화면(UC-16~18)·찜·글씨 크기 저장·시연용 계정 |
| [docs/auth-view.md](docs/auth-view.md) | 회원가입·로그인 화면, 상단 메뉴, 공통 디자인 `Theme`, 화면 이동 규칙 |
| [docs/myinfo-view.md](docs/myinfo-view.md) | 내 정보 화면 (UC-03) |
| [docs/product-view.md](docs/product-view.md) | 상품 목록·검색·상세 화면 (UC-04~06) |
| [docs/ui-data-update.md](docs/ui-data-update.md) | 화면 디자인 통일, 상품 370개 데이터 적용, 홈 새 디자인, 상품 화면·카드, 로고 (11-17~11-22 기록) |
| [docs/order-view.md](docs/order-view.md) | 장바구니·찜·주문서·주문 내역 화면 (UC-10~13) |
| [docs/review.md](docs/review.md) | 리뷰 쓰기 화면 (UC-14) |
| [docs/product-recommend.md](docs/product-recommend.md) | 상품·추천 도메인, 홈·룩북, 옷 입혀보기와 AI 키 설정 |
