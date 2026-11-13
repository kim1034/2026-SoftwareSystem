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

## 데모 데이터

`src/main/resources/data/products.tsv` 에 데모용 상품 300개(모자·상의·하의·신발·양말·주얼리)가 들어 있고, 프로그램을 켤 때 `ProductService` 가 읽습니다.
DB 스키마가 정해지기 전까지 상품·룩북은 메모리에만 두므로, 관리자 화면에서 바꾼 내용은 프로그램을 끄면 사라집니다.
데모 데이터에 없는 값(옵션·재고, 연령대·체형 태그, 유행 아이템, 룩북 6개)은 `ProductSeedLoader`, `RecommendService` 의 주석에 적은 규칙으로 임시로 채웁니다.

## 옷 입혀보기 (AI 이미지 합성)

상단 메뉴 [옷 입혀보기]에서 여성 캐릭터(실사·디즈니 캐릭터·3D 게임 캐릭터·애니메이션 캐릭터)에 모자·상의·하의·주얼리·신발을 입혀 봅니다.
이용 열쇠(API 키)는 코드나 저장소에 넣지 않고 환경 변수로 줍니다.

| 환경 변수 | 뜻 | 기본값 |
|-----------|----|--------|
| `GEMINI_API_KEY` | Google Gemini API 키. 없으면 외부로 아무것도 보내지 않는 미리보기 합성기를 씁니다 | (없음) |
| `SADAMDA_AI_MODEL` | 이미지 생성 모델 이름 | `gemini-3.1-flash-image` |
| `SADAMDA_AI_ENDPOINT` | API 주소 | Gemini Interactions API |
| `SADAMDA_AI_TIMEOUT` | 기다리는 시간(초) | 30 (NFR-05) |

```bash
GEMINI_API_KEY=발급받은_키 java -jar target/sadamda-0.1.0-SNAPSHOT.jar
```

관련 코드: `view/tryon/TryOnPanel`, `controller/TryOnController.dressUp`, `external/DressUpImageClient`(`GeminiDressUpClient`, `LocalDressUpClient`), `model/tryon`(`CharacterStyle`, `CharacterSpec`, `DressUpSlot`, `Outfit`, `DressUpPrompt`).

## 점검

상품·추천·보관함 도메인과 옷 입혀보기 연동을 한 번에 확인하는 실행형 검사입니다 (인터넷·API 키 불필요).

```bash
mvn test-compile
java -cp target/classes:target/test-classes com.sadamda.DomainSelfCheck
```

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

상품·추천 도메인(`model/product`, `model/recommend`, `model/tryon`)은 구현되어 있습니다.
그 밖에 아직 구현되지 않은 메서드는 `UnsupportedOperationException("TODO UC-xx")`를 던집니다.
결제 시스템과 AI 합성 서비스는 실제 연동 전까지 `MockPaymentGateway`, `MockAiImageClient`를 씁니다. (인물 사진 업로드 방식의 UC-09 흐름은 그대로 남아 있고, 옷 입혀보기는 그와 별개의 `DressUpImageClient`를 씁니다.)
