package com.sadamda.view.member;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.recommend.HomePanel;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** 필수: 이메일·비밀번호·이름·연락처 / 선택: 연령대·키·평소 사이즈·체형 */
public class SignUpPanel extends ScreenPanel {
    public static final String NAME = "signUp";

    private static final String NOT_SELECTED = "선택 안 함";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^01[016789]-?\\d{3,4}-?\\d{4}$");

    // 필수
    private final JTextField emailField = Theme.textField("예: hong@naver.com");
    private final JPasswordField passwordField = Theme.passwordField("8자 이상 입력");
    private final JPasswordField passwordConfirmField = Theme.passwordField("비밀번호를 한 번 더 입력");
    private final JTextField nameField = Theme.textField("이름 입력");
    private final JTextField phoneField = Theme.textField("예: 010-1234-5678");
    // 선택 (TODO: 선택지는 팀과 확정 후 수정)
    private final JComboBox<String> ageGroupBox = Theme.comboBox(NOT_SELECTED, "40대", "50대", "60대");
    private final JTextField heightField = Theme.textField("예: 160");
    private final JComboBox<String> sizeBox = Theme.comboBox(NOT_SELECTED, "S (55)", "M (66)", "L (77)", "XL (88)", "XXL (99)");
    private final JComboBox<String> bodyTypeBox = Theme.comboBox(NOT_SELECTED, "마른 체형", "보통 체형", "통통한 체형", "하체 발달형", "상체 발달형");
    private final JCheckBox agreeBox = Theme.checkBox("이용약관과 개인정보 수집에 동의합니다 (필수)");

    // UC-01 4b: 입력란 이름 옆에 이유를 표시한다.
    private final JLabel emailError = Theme.errorLabel();
    private final JLabel passwordError = Theme.errorLabel();
    private final JLabel passwordConfirmError = Theme.errorLabel();
    private final JLabel nameError = Theme.errorLabel();
    private final JLabel phoneError = Theme.errorLabel();
    private final JLabel heightError = Theme.errorLabel();
    private final JLabel agreeError = Theme.errorLabel();
    /** [가입하기] 실패 시 맨 위에 보여주는 오류 요약. */
    private final JLabel summaryError = Theme.errorLabel();

    /** 오류 문구 → 그 오류가 난 칸. 화면 위에서 아래 순서. */
    private final Map<JLabel, JComponent> errorTargets = new LinkedHashMap<>();

    public SignUpPanel(AppContext context, MainFrame frame) {
        super(context, frame, "회원가입", "UC-01 회원가입");
        hideTitle(); // 제목은 카드 안에 그린다.
        setBackground(Theme.PAGE);
        heightField.setColumns(6); // 두 칸짜리 줄이 카드 폭을 넓히지 않도록 짧게

        errorTargets.put(emailError, emailField);
        errorTargets.put(passwordError, passwordField);
        errorTargets.put(passwordConfirmError, passwordConfirmField);
        errorTargets.put(nameError, nameField);
        errorTargets.put(phoneError, phoneField);
        errorTargets.put(heightError, heightField);
        errorTargets.put(agreeError, agreeBox);

        JPanel card = Theme.card();
        Theme.addToCard(card, Theme.label("회원가입", 1.6f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(card, Theme.label("회원이 되어 나에게 맞는 옷을 추천받아 보세요!", 1f, Font.PLAIN, Theme.HINT), 8);
        Theme.addToCard(card, summaryError, 16);

        Theme.addToCard(card, Theme.label("필수 입력", 1.2f, Font.BOLD, Theme.TEXT), 16);
        Theme.addToCard(card, Theme.field("이메일", emailError, emailField), 16);
        Theme.addToCard(card, Theme.field("비밀번호 (8자 이상)", passwordError, Theme.passwordWithToggle(passwordField)), 20);
        Theme.addToCard(card, Theme.field("비밀번호 확인", passwordConfirmError, Theme.passwordWithToggle(passwordConfirmField)), 20);
        Theme.addToCard(card, Theme.field("이름", nameError, nameField), 20);
        Theme.addToCard(card, Theme.field("연락처", phoneError, phoneField), 20);

        Theme.addToCard(card, Theme.label("선택 입력", 1.2f, Font.BOLD, Theme.TEXT), 40);
        Theme.addToCard(card, Theme.label("입력하시면 체형에 맞는 옷을 추천해 드려요", 1f, Font.PLAIN, Theme.HINT), 4);
        Theme.addToCard(card, twoColumns(Theme.field("연령대", null, ageGroupBox), Theme.field("키 (cm)", heightError, heightField)), 16);
        Theme.addToCard(card, twoColumns(Theme.field("평소 사이즈", null, sizeBox), Theme.field("체형", null, bodyTypeBox)), 20);

        // 체크박스 문구가 길어서 오류는 아래 줄에 둔다. (옆에 두면 오류가 뜰 때 카드 폭이 바뀐다)
        JPanel agreeRow = new JPanel(new BorderLayout(0, 4));
        agreeRow.setOpaque(false);
        agreeRow.add(agreeBox, BorderLayout.NORTH);
        agreeRow.add(agreeError, BorderLayout.CENTER);
        Theme.addToCard(card, agreeRow, 32);

        JButton signUpButton = Theme.primaryButton("가입하기");
        signUpButton.addActionListener(e -> submit());
        JButton cancelButton = Theme.secondaryButton("가입취소");
        cancelButton.addActionListener(e -> frame.navigate(HomePanel.NAME));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.setOpaque(false);
        buttons.add(signUpButton);
        buttons.add(cancelButton);
        Theme.addToCard(card, buttons, 40);

        setContent(Theme.centeredPage(card));

        // 칸을 벗어나면 그 칸만 바로 검사한다.
        checkOnLeave(emailField, emailError, this::checkEmail);
        checkOnLeave(passwordField, passwordError, () -> {
            checkPassword();
            if (passwordConfirmField.getDocument().getLength() > 0) {
                checkPasswordConfirm(); // 비밀번호를 고치면 확인 칸도 다시 맞춰 본다.
            }
        });
        checkOnLeave(passwordConfirmField, passwordConfirmError, this::checkPasswordConfirm);
        checkOnLeave(nameField, nameError, this::checkName);
        checkOnLeave(phoneField, phoneError, this::checkPhone);
        checkOnLeave(heightField, heightError, this::checkHeight);

        // 화면이 열리면 바로 입력할 수 있게 이메일 칸에 커서를 둔다.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                emailField.requestFocusInWindow();
            }
        });
    }

    /** UC-01 3~5단계 */
    private void submit() {
        // & 를 써서 모든 항목을 한 번에 검사해 오류를 함께 보여준다.
        boolean valid = checkEmail() & checkPassword() & checkPasswordConfirm() & checkName()
                & checkPhone() & checkHeight() & checkAgree();
        if (!valid) {
            showSummary();
            return;
        }
        summaryError.setText("");

        // TODO UC-01 4~5단계: Member 에 생성자가 생기면(백예원) 입력값으로 Member 를 만들어 연결한다.
        //   context.member.signUp(member, password) 성공 → 가입 완료 안내 후 frame.navigate(HomePanel.NAME)
        //   이미 가입된 이메일(4a) → emailError 에 "이미 가입된 이메일입니다" 표시
        //   "선택 안 함" 은 null 로 넘긴다.
        JOptionPane.showMessageDialog(this, "입력 확인이 끝났습니다.\n가입 처리는 아직 연결되지 않았습니다.");
    }

    /** 맨 위에 틀린 곳 개수를 알리고, 첫 번째 틀린 칸으로 화면을 옮겨 커서를 둔다. */
    private void showSummary() {
        summaryError.setText(" "); // updateSummary 가 갱신하도록 표시만 켠다.
        updateSummary();

        JComponent first = errorTargets.entrySet().stream()
                .filter(entry -> !entry.getKey().getText().isEmpty())
                .map(Map.Entry::getValue)
                .findFirst().orElseThrow();
        // 칸 위의 이름표·오류 문구까지 보이도록 칸 높이만큼 위쪽을 더 보여준다.
        // 요약 문구와 함께 한 화면에 들어가면 요약까지 보이게 한다.
        // (JTextField 는 스크롤 요청을 자기 글자 스크롤로만 써서, 칸 대신 카드에 요청한다)
        JComponent card = (JComponent) summaryError.getParent();
        Rectangle area = new Rectangle(first.getX(), first.getY() - first.getHeight(), first.getWidth(), first.getHeight() * 2);
        Rectangle fieldArea = SwingUtilities.convertRectangle(first.getParent(), area, card);
        Rectangle withSummary = fieldArea.union(summaryError.getBounds());
        card.scrollRectToVisible(withSummary.height <= card.getVisibleRect().height ? withSummary : fieldArea);
        first.requestFocusInWindow();
    }

    /** 요약이 떠 있으면 남은 오류 개수로 고치고, 다 고쳤으면 지운다. */
    private void updateSummary() {
        if (summaryError.getText().isEmpty()) {
            return;
        }
        long count = errorTargets.keySet().stream().filter(label -> !label.getText().isEmpty()).count();
        summaryError.setText(count == 0 ? "" : "입력 내용을 확인해 주세요 (" + count + "곳)");
    }

    private boolean checkEmail() {
        return show(emailError, emailField, emailMessage(emailField.getText().trim()));
    }

    private boolean checkPassword() {
        return show(passwordError, passwordField, passwordField.getPassword().length < 8 ? "8자 이상 입력해 주세요" : null);
    }

    private boolean checkPasswordConfirm() {
        return show(passwordConfirmError, passwordConfirmField,
                passwordConfirmMessage(passwordField.getPassword(), passwordConfirmField.getPassword()));
    }

    private boolean checkName() {
        return show(nameError, nameField, nameField.getText().isBlank() ? "이름을 입력해 주세요" : null);
    }

    private boolean checkPhone() {
        return show(phoneError, phoneField, phoneMessage(phoneField.getText().trim()));
    }

    private boolean checkHeight() {
        return show(heightError, heightField, heightMessage(heightField.getText().trim()));
    }

    private boolean checkAgree() {
        return show(agreeError, agreeBox, agreeBox.isSelected() ? null : "약관에 동의해 주세요");
    }

    /**
     * 칸에서 포커스가 나가면 check 를 실행한다.
     * 아직 아무것도 안 쓴 빈칸은 지나가기만 해도 오류를 띄우지 않는다. (이미 오류가 떠 있으면 다시 검사해 지운다)
     */
    private void checkOnLeave(JTextComponent field, JLabel error, Runnable check) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getDocument().getLength() > 0 || !error.getText().isEmpty()) {
                    check.run();
                    updateSummary();
                }
            }
        });
    }

    private static String emailMessage(String email) {
        if (email.isEmpty()) {
            return "이메일을 입력해 주세요";
        }
        return EMAIL.matcher(email).matches() ? null : "이메일 형식이 맞지 않습니다";
    }

    private static String passwordConfirmMessage(char[] password, char[] confirm) {
        if (confirm.length == 0) {
            return "한 번 더 입력해 주세요";
        }
        return Arrays.equals(password, confirm) ? null : "비밀번호가 서로 다릅니다";
    }

    private static String phoneMessage(String phone) {
        if (phone.isEmpty()) {
            return "연락처를 입력해 주세요";
        }
        return PHONE.matcher(phone).matches() ? null : "연락처 형식이 맞지 않습니다";
    }

    private static String heightMessage(String height) {
        if (height.isEmpty()) {
            return null; // 선택 입력
        }
        try {
            int cm = Integer.parseInt(height);
            return cm >= 100 && cm <= 250 ? null : "100~250 사이로 입력해 주세요";
        } catch (NumberFormatException e) {
            return "숫자로 입력해 주세요";
        }
    }

    /** 오류 문구와 빨간 테두리를 표시하거나 지운다. 오류가 없으면 true. */
    private static boolean show(JLabel errorLabel, JComponent input, String message) {
        errorLabel.setText(message == null ? "" : message);
        Theme.setInvalid(input, message != null);
        return message == null;
    }

    private static JPanel twoColumns(JComponent left, JComponent right) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setOpaque(false);
        panel.add(left);
        panel.add(right);
        return panel;
    }
}
