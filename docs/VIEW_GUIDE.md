# View 작업 안내: 회원가입·로그인 화면

작성: 강준서 (View 담당) · 2026-11-13 · 브랜치 `Junes`

회원가입(UC-01)과 로그인(UC-02) 화면을 만들었습니다. 이 문서에는 화면 구조, 공통 디자인(`Theme`) 사용법, 그리고 **Model·Controller 쪽에 부탁드릴 일**을 정리했습니다.

---

## 1. 바로 보기

IDE에서 `com.sadamda.App`의 `main`을 실행한 뒤, 상단 메뉴에서 **[회원가입]** 또는 **[로그인]**을 누르면 됩니다. 나머지 메뉴는 아직 "TODO" 문구만 나옵니다.

| 회원가입 | 로그인 |
|---|---|
| 흰 카드 안에 제목과 입력칸이 있고, 이름표 옆에 빨간 오류 문구가 뜹니다. 아래에 [가입하기]·[가입취소] 버튼이 있습니다. | 이메일·비밀번호, 로그인 상태 유지, 전체 폭 [로그인] 버튼, 그 아래 비밀번호 찾기·회원가입 링크가 있습니다. |

---

## 2. 바뀐 파일

| 파일 | 내용 |
|---|---|
| `view/Theme.java` (새 파일) | 모든 화면이 함께 쓰는 디자인: 색, 둥근 입력칸, 버튼, 카드 |
| `view/ScreenPanel.java` | `setContent()`, `hideTitle()` 추가 (아래 설명) |
| `view/member/SignUpPanel.java` | 회원가입 화면 |
| `view/member/LoginPanel.java` | 로그인 화면 |

Model·Controller 코드는 **수정하지 않았습니다.**

---

## 3. 화면이 동작하는 방식

### 회원가입 (`SignUpPanel`)

| 구분 | 항목 |
|---|---|
| 필수 | 이메일, 비밀번호(8자 이상), 비밀번호 확인, 이름, 연락처 |
| 선택 | 연령대, 키(cm), 평소 사이즈, 체형 |
| 기타 | 약관 동의 체크박스 |

[가입하기]를 누르면 화면에서 아래 항목을 먼저 확인하고, 틀린 칸은 **모두 한꺼번에** 이름표 옆에 이유를 보여줍니다 (UC-01 4b).

| 항목 | 규칙 | 오류 문구 |
|---|---|---|
| 이메일 | 비어 있지 않고 `아이디@도메인.xx` 형식 | 이메일 형식이 맞지 않습니다 |
| 비밀번호 | 8자 이상 | 8자 이상 입력해 주세요 |
| 비밀번호 확인 | 비밀번호와 같음 | 비밀번호가 서로 다릅니다 |
| 이름 | 비어 있지 않음 | 이름을 입력해 주세요 |
| 연락처 | `010-1234-5678` 또는 `01012345678` | 연락처 형식이 맞지 않습니다 |
| 키 | 비어 있으면 통과, 입력하면 100~250 사이 숫자 | 숫자로 입력해 주세요 |
| 약관 | 체크 필수 | 약관에 동의해 주세요 |

> **현재 상태:** 검사를 통과하면 "가입 처리는 아직 연결되지 않았습니다" 안내만 뜹니다. `Member`에 생성자가 없어서 `context.member.signUp(...)`을 아직 호출하지 못합니다. (5번 항목 참고)

### 로그인 (`LoginPanel`)

- **[로그인]** 또는 비밀번호 칸에서 Enter → `context.member.login(email, password)` 호출. **이미 Controller에 연결되어 있습니다.**
  - `MemberService`가 아직 `UnsupportedOperationException`을 던지면 "아직 연결되지 않았습니다" 안내를 띄웁니다.
  - 그 밖의 예외가 나면 버튼 위에 "이메일 또는 비밀번호가 맞지 않습니다"를 표시합니다 (UC-02 3a).
  - 성공하면 홈 화면으로 이동합니다.
- **[비밀번호 찾기]** → `context.member.requestPasswordReset(email)` 호출 (UC-02 2a)
- **[회원가입]** → 회원가입 화면으로 이동

**`MemberService`를 구현하면 화면 코드를 고치지 않아도 로그인이 바로 동작합니다.**

---

## 4. 공통 디자인 `Theme` 사용법

> **화면을 만드는 분(김대원 님: 관리자 화면)만 읽으시면 됩니다.** Model 담당은 건너뛰셔도 됩니다.

`Theme`은 **미리 디자인해 둔 화면 부품 모음**입니다. Swing 기본 부품은 회색 네모 모양이고, 글씨를 키우면 글자가 잘리기도 해서 둥근 입력칸, 파란·노란 버튼, 빨간 오류 문구 등을 `Theme.java` 한 파일에 만들어 두었습니다. 화면마다 새로 만들 필요 없이 꺼내 쓰면 됩니다.

```java
JButton button = new JButton("저장");          // Swing 기본: 회색 네모 버튼
JButton button = Theme.primaryButton("저장");  // Theme: 파란 둥근 버튼, 크기·대비 기준 자동 충족
```

이렇게 쓰면 모든 화면이 같은 모양이 되고, 아래 접근성 기준도 자동으로 지켜집니다.

### 지켜지는 기준

| 기준 | 내용 |
|---|---|
| NFR-01 | 글자와 배경의 명도 대비 4.5:1 이상. 버튼 글씨는 본문보다 2px 커서, 기본 설정에서 20px |
| NFR-02 | 버튼과 링크의 누르는 영역 높이 48px 이상 |
| UC-15 | [글씨 크기]를 바꾸면 입력칸, 버튼, 체크박스, 제목이 모두 함께 커짐 |

### 사용 예시

```java
public class MyScreen extends ScreenPanel {
    private final JTextField nameField = Theme.textField("이름 입력");   // 회색 안내 문구
    private final JLabel nameError = Theme.errorLabel();                // 빨간 오류 문구

    public MyScreen(AppContext context, MainFrame frame) {
        super(context, frame, "제목", "UC-xx");
        hideTitle();                       // 제목을 카드 안에 그릴 때
        setBackground(Theme.PAGE);

        JPanel card = Theme.card();        // 흰 카드
        Theme.addToCard(card, Theme.label("제목", 1.6f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(card, Theme.field("이름", nameError, nameField), 32);   // 이름표 + 입력칸
        Theme.addToCard(card, Theme.primaryButton("저장"), 32);

        setContent(Theme.centeredPage(card));   // 회색 배경 가운데에 카드, 스크롤 포함
    }
}
```

### 기능 목록

| 메서드 | 용도 |
|---|---|
| `card()` / `addToCard(card, 컴포넌트, 위쪽 간격)` | 흰 카드를 만들고, 내용을 위에서 아래로 한 줄씩 쌓음 |
| `centeredPage(card)` | 회색 배경 가운데 위쪽에 카드를 놓음. 화면이 넘치면 스크롤 |
| `field(이름표, 오류라벨, 입력칸)` | "이름표 + 오류 문구"를 위에, 입력칸을 아래에 배치. 오류가 필요 없으면 `null` |
| `textField(안내문구)` / `passwordField(안내문구)` | 둥근 입력칸. 비어 있으면 회색 안내 문구, 누르면 파란 테두리 |
| `comboBox(항목...)` | 선택 상자 (글씨 크기에 맞춰 커짐) |
| `checkBox(문구)` | 큰 체크박스 |
| `primaryButton` / `secondaryButton` / `linkButton` | 파란 버튼 / 노란 버튼 / 파란 글자 링크 |
| `label(문구, 배율, 굵기, 색)` | 글씨 크기 조절을 따라가는 라벨. 제목은 배율 `1.6f` 정도 |
| `errorLabel()` | 빨간 오류 문구. 빈 문자열이면 보이지 않음 |
| 색 상수 | `PAGE`(배경), `TEXT`, `HINT`(회색 글씨), `PRIMARY`(파랑), `SECONDARY`(노랑), `ERROR`(빨강), `LINE`(테두리) |

### `ScreenPanel`에 추가된 메서드

- `setContent(컴포넌트)`: 화면 가운데의 "TODO" 문구를 실제 내용으로 바꿉니다. 아직 구현하지 않은 화면은 그대로 둡니다.
- `hideTitle()`: 위쪽 공통 제목을 숨깁니다. 카드 안에 제목을 직접 넣을 때 씁니다.

### 주의할 점

- 색이나 글꼴을 `setFont()`로 직접 지정하면 **[글씨 크기]를 바꿔도 따라 커지지 않습니다.** 크기가 다른 글씨가 필요하면 `Theme.label(...)`을 쓰세요.
- macOS 기본 콤보박스, 체크박스, 버튼은 글씨를 키우면 글자가 잘리거나 너무 작게 남아서 `Theme`에서 모양을 바꿔 두었습니다. 직접 `new JComboBox<>()`를 쓰지 말고 `Theme.comboBox(...)`를 쓰세요.

---

## 5. Model·Controller 담당자께 부탁드릴 일

### 백예원 님 (회원)

1. **`Member` 생성자와 getter**
   - 회원가입 화면에서 입력값을 `Member`로 만들어 `signUp`에 넘겨야 합니다. 생성자나 setter가 필요합니다.
   - 로그인 후 관리자인지 확인하려면 `getRole()`이 필요합니다 (UC-02 4단계).
   - 화면에서 넘길 값:

     | 화면 입력 | `Member` / `BodyProfile` 필드 | 비고 |
     |---|---|---|
     | 이메일, 이름, 연락처 | `email`, `name`, `phone` | |
     | 비밀번호 | `signUp(member, rawPassword)`의 두 번째 인자 | 해시는 Model에서 (NFR-07) |
     | 연령대, 평소 사이즈, 체형 | `BodyProfile.ageGroup`, `usualSize`, `bodyType` | "선택 안 함"이면 `null` |
     | 키 | `BodyProfile.heightCm` | 비어 있으면 `null` |

2. **예외 종류 정하기.** 화면이 상황별로 다른 문구를 띄우려면 구분할 수 있는 예외가 필요합니다.
   - 이미 가입된 이메일 (UC-01 4a) → 이메일 칸 옆에 "이미 가입된 이메일입니다"
   - 로그인 실패 (UC-02 3a) → "이메일 또는 비밀번호가 맞지 않습니다"
   - 지금은 로그인에서 `RuntimeException`을 모두 "로그인 실패"로 처리하고 있습니다.

### 정우진 님 (상품·추천)

- 다음 작업이 상품 목록, 상세, 홈, 룩북 화면입니다. 화면에 상품명, 가격, 이미지를 보여주려면 **`Product`, `Lookbook`의 getter**가 필요합니다. 그 전까지는 화면 쪽 목업 데이터로 개발하겠습니다.

### 김대원 님 (Controller)

- 회원가입과 로그인은 `MemberController`의 기존 메서드(`signUp`, `login`, `requestPasswordReset`)만 썼습니다. 추가로 필요한 것은 없습니다.
- 관리자 화면도 `Theme`을 쓰시면 디자인을 맞출 수 있습니다 (4번 참고).

---

## 6. 팀에서 함께 정할 것

아래 값은 제가 **임시로** 정했습니다. 의견 주시면 바꾸겠습니다.

| 항목 | 지금 값 |
|---|---|
| 비밀번호 규칙 | 8자 이상 (명세에 기준 없음) |
| 연령대 선택지 | 40대 / 50대 / 60대 |
| 평소 사이즈 선택지 | S(55) / M(66) / L(77) / XL(88) / XXL(99) |
| 체형 선택지 | 마른 / 보통 / 통통한 / 하체 발달형 / 상체 발달형 |
| 비밀번호 확인 칸 | 명세에는 없지만 오타 방지를 위해 추가함 |
