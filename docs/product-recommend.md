# 상품·추천 도메인과 옷 입혀보기

담당: 정우진 (Model — 상품·추천 도메인). 팀 공용 설명은 [README](../README.md)를 봅니다.

## 구현 범위

| 기능 | 코드 | 관련 요구사항 |
|------|------|---------------|
| 상품·카테고리 데이터, 카테고리별 조회, [더 보기] | `model/product/ProductService`, `ProductSeedLoader` | FR-05, UC-04 |
| 검색·필터·정렬 (키워드, 연령대, 체형, 사이즈, 가격대, 색상 / 인기·신상품·가격순) | `ProductService.search`, `SearchFilter` | FR-06, FR-07, UC-05 |
| 룩북 코디 조회 (상황·계절), 코디 등록·수정·삭제 | `model/recommend/RecommendService`, `Lookbook` | FR-09, FR-26, UC-07, UC-17 |
| 유행 아이템, 연령대·체형 맞춤 추천 | `RecommendService.findTrendingItems`, `recommendFor` | FR-11, FR-12, UC-08 |
| 합성 이미지 보관함 (저장·목록·삭제) | `model/tryon/TryOnService`, `view/tryon/LockerDialog` | FR-16, NFR-08 |
| 옷 입혀보기 (캐릭터에 상품을 AI로 입히기) | `view/tryon/TryOnPanel`, `controller/TryOnController`, `external/*DressUp*`, `model/tryon` | UC-09 확장 |

## 데모 데이터

- `src/main/resources/data/products.tsv`: 팀 구글 시트의 테스트 데이터 300개입니다. 모자 30, 상의 30, 하의 30, 신발 90, 양말 30, 주얼리 90개가 들어 있습니다.
- 시트의 "현지표시가"와 "이미지 속성" 열은 쓰지 않습니다. 가격은 숫자로 바꿔 넣습니다.
- 데이터에 없는 값은 임시 규칙으로 채웁니다 (`ProductSeedLoader`, `RecommendService` 주석).
  - 옵션·재고: 카테고리별 기본 사이즈 × 소재 문구의 색상, 재고 10
  - 유행 아이템: 세부 카테고리마다 앞의 2개 (`-01`, `-02`), 모두 20개
  - 룩북: 나라별 데모 코디 6개
- DB가 정해지기 전까지 상품·룩북은 메모리에만 둡니다. 관리자 화면에서 바꾼 내용은 프로그램을 끄면 사라집니다.

## 옷 입혀보기

상단 메뉴 [옷 입혀보기]에서 캐릭터(실사·디즈니 캐릭터·3D 게임 캐릭터·애니메이션 캐릭터)에 모자·상의·하의·주얼리·신발을 입혀 봅니다.

- [캐릭터 신체 스펙 정함]에서 키·몸무게·체형·연령대를 바꾸면 캐릭터 모습이 바로 바뀝니다.
- [입혀보기]를 누르면 AI가 캐릭터에 고른 상품을 입힌 그림을 만듭니다. 결과는 [보관함에 저장], [내 컴퓨터에 저장], [장바구니 담기]로 처리합니다.
- 상품 사진을 구한 상품은 AI가 **사진만** 보고 그립니다. 상품명·색상 글은 보내지 않습니다. 데이터에서 이름과 사진이 다른 상품이 있어서, 글을 함께 보내면 화면의 사진과 다른 결과가 나왔기 때문입니다. 사진을 구하지 못한 상품만 글로 설명합니다.
- [내 보관함]에서 저장한 그림을 보고, 고른 그림을 지울 수 있습니다. 지운 그림은 이 PC의 보관함 폴더(`사용자 홈/.sadamda/tryon/locker`)에서도 사라집니다. 보관함은 로그인한 회원만 쓸 수 있습니다.

## API 키 생성 및 입력 방법

AI 합성에는 이용 열쇠(API 키)가 필요합니다. 아래는 학교에서 쓰는 타임리 AI의 연동 키를 쓰는 방법입니다.

**1. 타임리 AI에서 연동 키 관리 화면을 엽니다.**
설정 화면의 "보안" 아래 [연동 키 관리]를 누릅니다.

![연동 키 관리 화면](images/api-key-01-menu.webp)

**2. 연동 키(API Key)와 OpenAI 호환 주소(baseURL)를 복사합니다.**
- 연동 키는 `tgpt_s`로 시작합니다. 키가 없으면 [재발급]을 누릅니다. 새 키를 받으면 전에 쓰던 키는 더 이상 쓸 수 없습니다.
- OpenAI 호환 주소는 오른쪽 복사 버튼으로 복사합니다.

![연동 키와 baseURL 복사](images/api-key-02-copy.webp)

**3. 사담다에서 [옷 입혀보기] → [AI 연결 설정]을 누르고 값을 넣습니다.**
- 이용 열쇠 (API 키): 2단계에서 복사한 연동 키
- 서비스 주소: 2단계에서 복사한 OpenAI 호환 주소
- 모델 이름: 비워 둡니다. 비우면 기본 모델(`google/gemini-3.1-flash-image`)을 씁니다.
- [저장]을 누르면 화면 위쪽에 "그림 만드는 곳: 중계 서비스 (google/gemini-3.1-flash-image)"가 보입니다.

![사담다 AI 연결 설정](images/api-key-03-app-settings.webp)

> 중계 서비스를 쓸 때 서비스 주소를 비우면 OpenAI 공식 주소로 연결되어 실패합니다.
> 넣은 값은 이 PC의 사용자 설정(`java.util.prefs`)에만 저장됩니다. 코드, 저장소, jar 파일에는 들어가지 않습니다.
> 연동 키는 다른 사람에게 보여 주거나 저장소에 올리지 마세요.

### 환경 변수로 넣기

화면 대신 환경 변수(또는 같은 이름의 `-D` 옵션)로 줄 수도 있습니다. 환경 변수가 있으면 화면에서 넣은 값보다 먼저 씁니다.

| 환경 변수 | 뜻 | 기본값 |
|-----------|----|--------|
| `OPENAI_API_KEY` | OpenAI 공식 또는 호환 중계 서비스 키 | (없음) |
| `OPENAI_BASE_URL` | 중계 서비스 주소 | `https://api.openai.com/v1` |
| `GEMINI_API_KEY` | Google Gemini 키 (`AIza…`) | (없음) |
| `SADAMDA_AI_PROVIDER` | `openai` · `gemini` 로 강제 지정 | 키 모양으로 자동 선택 |
| `SADAMDA_AI_API` | OpenAI 형식의 보내는 방식 `images` · `chat` | 공식 주소는 `images`, 중계 주소는 `chat` |
| `SADAMDA_AI_MODEL` | 이미지 모델 이름 | images `gpt-image-2` / chat `google/gemini-3.1-flash-image` / Gemini `gemini-3.1-flash-image` |
| `SADAMDA_AI_QUALITY` | OpenAI 형식 품질 `low`·`medium`·`high`·`auto` | `medium` |
| `SADAMDA_AI_TIMEOUT` | 기다리는 시간(초) | 60 |

```powershell
$env:OPENAI_API_KEY="연동키"; $env:OPENAI_BASE_URL="https://hello.timelyai.io/api/v2/chat/bridge/openai"; java -jar target/sadamda-0.1.0-SNAPSHOT.jar
```

키가 하나도 없으면 외부로 아무것도 보내지 않고, 화면에 연결 설정 안내만 보여 줍니다 (`UnconnectedDressUpClient`).

## 점검

상품·추천·보관함 도메인과 옷 입혀보기 연동을 한 번에 확인하는 실행형 검사입니다 (인터넷·API 키 불필요, 107개).

```bash
mvn test-compile
# Mac/Linux
java -cp target/classes:target/test-classes com.sadamda.DomainSelfCheck
# Windows
java -cp "target/classes;target/test-classes" com.sadamda.DomainSelfCheck
```

## 아직 남은 것

- 연령대 태그: 모든 상품에 40·50·60대가 똑같이 붙어 있어서, 연령대 필터와 연령대 맞춤 추천이 결과를 바꾸지 못합니다. 체형 태그는 상품명·코디 문구로 추정한 임시 값입니다.
- 데이터 불일치: `HAT-KR-02`는 이름이 "블랙 버킷햇"인데 사진은 회색 니트 후드입니다. 다른 상품은 아직 확인하지 않았습니다.
- 업로드 사진 삭제: `TryOnService`의 업로드 사진 보관·삭제 메서드는 있지만, 인물 사진 업로드 방식(`TryOnController.generate`)이 아직 TODO여서 호출하는 곳이 없습니다.
- 로그인(회원 도메인)이 구현되기 전까지 보관함 저장·보기는 화면에서 쓸 수 없습니다.
- 홈·상품 목록·룩북·마이페이지 화면(View)은 아직 골격 상태라, 검색·추천은 Controller까지만 연결되어 있습니다.
