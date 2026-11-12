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
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
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

    public SignUpPanel(AppContext context, MainFrame frame) {
        super(context, frame, "회원가입", "UC-01 회원가입");
        hideTitle(); // 제목은 카드 안에 그린다.
        setBackground(Theme.PAGE);
        heightField.setColumns(6); // 두 칸짜리 줄이 카드 폭을 넓히지 않도록 짧게

        JPanel card = Theme.card();

        Theme.addToCard(card, Theme.label("회원가입", 1.6f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(card, Theme.label("회원이 되어 나에게 맞는 옷을 추천받아 보세요!", 1f, Font.PLAIN, Theme.HINT), 8);

        Theme.addToCard(card, Theme.field("이메일", emailError, emailField), 32);
        Theme.addToCard(card, Theme.field("비밀번호", passwordError, passwordField), 20);
        Theme.addToCard(card, Theme.field("비밀번호 확인", passwordConfirmError, passwordConfirmField), 20);
        Theme.addToCard(card, Theme.field("이름", nameError, nameField), 20);
        Theme.addToCard(card, Theme.field("연락처", phoneError, phoneField), 20);

        Theme.addToCard(card, Theme.label("선택 입력", 1.2f, Font.BOLD, Theme.TEXT), 40);
        Theme.addToCard(card, Theme.label("입력하시면 체형에 맞는 옷을 추천해 드려요", 1f, Font.PLAIN, Theme.HINT), 4);
        Theme.addToCard(card, twoColumns(Theme.field("연령대", null, ageGroupBox), Theme.field("키 (cm)", heightError, heightField)), 20);
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
    }

    /** UC-01 3~5단계 */
    private void submit() {
        char[] password = passwordField.getPassword();
        char[] passwordConfirm = passwordConfirmField.getPassword();

        // & 를 써서 모든 항목을 한 번에 검사해 오류를 함께 보여준다.
        boolean valid = show(emailError, emailMessage(emailField.getText().trim()))
                & show(passwordError, password.length < 8 ? "8자 이상 입력해 주세요" : null)
                & show(passwordConfirmError, passwordConfirmMessage(password, passwordConfirm))
                & show(nameError, nameField.getText().isBlank() ? "이름을 입력해 주세요" : null)
                & show(phoneError, phoneMessage(phoneField.getText().trim()))
                & show(heightError, heightMessage(heightField.getText().trim()))
                & show(agreeError, agreeBox.isSelected() ? null : "약관에 동의해 주세요");
        if (!valid) {
            return;
        }

        // TODO UC-01 4~5단계: Member 에 생성자가 생기면(백예원) 입력값으로 Member 를 만들어 연결한다.
        //   context.member.signUp(member, password) 성공 → 가입 완료 안내 후 frame.navigate(HomePanel.NAME)
        //   이미 가입된 이메일(4a) → emailError 에 "이미 가입된 이메일입니다" 표시
        //   "선택 안 함" 은 null 로 넘긴다.
        JOptionPane.showMessageDialog(this, "입력 확인이 끝났습니다.\n가입 처리는 아직 연결되지 않았습니다.");
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

    /** 오류 문구를 표시하거나 지운다. 오류가 없으면 true. */
    private static boolean show(JLabel errorLabel, String message) {
        errorLabel.setText(message == null ? "" : message);
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
