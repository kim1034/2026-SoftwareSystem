package com.sadamda.view.member;

import com.sadamda.common.AppException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.member.Member;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.admin.AdminPanel;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/** 이메일·비밀번호, [비밀번호 찾기]. 관리자는 로그인 후 관리자 화면으로 이동 */
public class LoginPanel extends ScreenPanel {
    public static final String NAME = "login";

    private final JTextField emailField = Theme.textField("가입한 이메일 입력");
    private final JPasswordField passwordField = Theme.passwordField("비밀번호 입력");
    private final JCheckBox keepLoginBox = Theme.checkBox("로그인 상태 유지");

    private final JLabel emailError = Theme.errorLabel();
    private final JLabel passwordError = Theme.errorLabel();
    private final JLabel loginError = Theme.errorLabel(); // UC-02 3a

    public LoginPanel(AppContext context, MainFrame frame) {
        super(context, frame, "로그인", "UC-02 로그인");
        hideTitle(); // 제목은 카드 안에 그린다.
        setBackground(Theme.PAGE);

        JPanel card = Theme.card();
        Theme.addToCard(card, Theme.label("로그인", 1.6f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(card, Theme.label("사담다에 오신 것을 환영합니다", 1f, Font.PLAIN, Theme.HINT), 8);

        Theme.addToCard(card, Theme.field("이메일", emailError, emailField), 32);
        Theme.addToCard(card, Theme.field("비밀번호", passwordError, Theme.passwordWithToggle(passwordField)), 20);
        Theme.addToCard(card, keepLoginBox, 16);

        Theme.addToCard(card, loginError, 16);
        JButton loginButton = Theme.primaryButton("로그인");
        loginButton.addActionListener(e -> login());
        passwordField.addActionListener(e -> login()); // 비밀번호 칸에서 Enter 로도 로그인
        Theme.addToCard(card, loginButton, 8);

        JButton findPasswordButton = Theme.linkButton("비밀번호 찾기");
        findPasswordButton.addActionListener(e -> requestPasswordReset());
        JButton signUpButton = Theme.linkButton("회원가입");
        signUpButton.addActionListener(e -> frame.navigate(SignUpPanel.NAME));
        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        links.setOpaque(false);
        links.add(findPasswordButton);
        links.add(Theme.label("|", 1f, Font.PLAIN, Theme.LINE));
        links.add(signUpButton);
        Theme.addToCard(card, links, 16);

        setContent(Theme.centeredPage(card));

        // 화면이 열리면 바로 입력할 수 있게 이메일 칸에 커서를 둔다.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                emailField.requestFocusInWindow();
            }
        });
    }

    /** UC-02 2~4단계 */
    private void login() {
        String email = emailField.getText().trim();
        char[] password = passwordField.getPassword();

        boolean valid = show(emailError, emailField, email.isEmpty() ? "이메일을 입력해 주세요" : null)
                & show(passwordError, passwordField, password.length == 0 ? "비밀번호를 입력해 주세요" : null);
        loginError.setText("");
        if (!valid) {
            return;
        }

        Member member;
        try {
            member = context.member.login(email, new String(password));
        } catch (UnsupportedOperationException e) {
            JOptionPane.showMessageDialog(this, "로그인 기능은 아직 연결되지 않았습니다.");
            return;
        } catch (AppException e) {
            // UC-02 3a 계정 불일치 등. 메시지는 사용자에게 보여줄 문장으로 온다 (docs/controller-spec.md).
            loginError.setText(e.getMessage());
            return;
        }

        passwordField.setText("");
        // TODO FR-02: keepLoginBox 가 체크되면 로그인 상태 유지
        // UC-02 4단계: 관리자는 관리자 화면으로, 일반 회원은 로그인 전에 보던 화면으로.
        if (member.getRole() == Member.Role.ADMIN) {
            frame.navigate(AdminPanel.NAME);
        } else {
            frame.returnAfterLogin();
        }
    }

    /** UC-02 2a: 가입 이메일로 비밀번호 재설정 링크를 보낸다. */
    private void requestPasswordReset() {
        String email = emailField.getText().trim();
        if (!show(emailError, emailField, email.isEmpty() ? "이메일을 입력하고 눌러 주세요" : null)) {
            return;
        }
        try {
            context.member.requestPasswordReset(email);
            JOptionPane.showMessageDialog(this, "비밀번호 재설정 링크를 이메일로 보냈습니다.\n메일함을 확인해 주세요.");
        } catch (UnsupportedOperationException e) {
            JOptionPane.showMessageDialog(this, "비밀번호 찾기 기능은 아직 연결되지 않았습니다.");
        } catch (AppException e) {
            loginError.setText(e.getMessage());
        }
    }

    /** 오류 문구와 빨간 테두리를 표시하거나 지운다. 오류가 없으면 true. */
    private static boolean show(JLabel errorLabel, JComponent input, String message) {
        errorLabel.setText(message == null ? "" : message);
        Theme.setInvalid(input, message != null);
        return message == null;
    }
}
