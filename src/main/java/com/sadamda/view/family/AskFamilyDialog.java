package com.sadamda.view.family;

import com.sadamda.common.AppException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.family.AskRequest;
import com.sadamda.view.MainFrame;
import com.sadamda.view.Theme;
import com.sadamda.view.Toast;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.function.BiFunction;

/**
 * [가족에게 물어보기] 창: 가족의 이메일과 하고 싶은 말을 적고 [물어보기].
 * 보내면 화면 아래에 "김지은님에게 물어봤어요 · 가족 쇼핑 보기" 알림을 띄운다.
 */
public final class AskFamilyDialog extends JDialog {
    private final JTextField email = Theme.textField("가족의 사담다 이메일 (예: daughter@sadamda.com)");
    private final JTextArea message = Theme.textArea("예: 다음 달 결혼식에 입으려는데 어때? 색은 괜찮니?", 3);
    private final JLabel error = Theme.errorLabel();

    private AskFamilyDialog(JComponent anchor, AppContext context, MainFrame frame, String what,
                            BiFunction<String, String, AskRequest> send) {
        super(SwingUtilities.getWindowAncestor(anchor), "가족에게 물어보기", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Theme.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        Theme.addToCard(content, Theme.label("가족에게 물어보기", 1.3f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(content, Theme.label("<html>" + what + "을(를) 가족에게 보내 \"어때?\" 하고 물어봐요.<br>"
                + "가족은 의견을 남기거나 대신 결제해 줄 수 있어요.</html>", 1f, Font.PLAIN, Theme.HINT), 8);
        email.setText(context.family.lastFamilyEmail());
        Theme.addToCard(content, Theme.field("가족 이메일", null, email), 20);
        JScrollPane messageScroll = new JScrollPane(message);
        messageScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        Theme.addToCard(content, Theme.field("하고 싶은 말 (선택)", null, messageScroll), 16);
        Theme.addToCard(content, error, 12);

        JButton cancel = Theme.secondaryButton("닫기");
        cancel.addActionListener(e -> dispose());
        JButton ok = Theme.primaryButton("물어보기");
        ok.addActionListener(e -> {
            try {
                AskRequest request = send.apply(email.getText(), message.getText());
                dispose();
                frame.refreshFamilyBadge();
                String name = context.family.nameOf(request.getResponderId());
                Toast.show(anchor, name + "님에게 물어봤어요. 답이 오면 알려 드려요", "가족 쇼핑 보기",
                        () -> frame.navigate(FamilyPanel.NAME));
            } catch (AppException ex) {
                error.setText("<html>" + ex.getMessage() + "</html>");
                pack();
            }
        });
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);
        Theme.addToCard(content, buttons, 8);
        setContentPane(content);
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setSize(Math.max(getWidth(), 640), getHeight());
        setLocationRelativeTo(anchor);
    }

    /**
     * 창을 연다. what 은 "이 상품"·"장바구니 상품 3개" 같은 설명, send 는 (이메일, 하고 싶은 말) → 보낸 요청.
     */
    public static void open(JComponent anchor, AppContext context, MainFrame frame, String what,
                            BiFunction<String, String, AskRequest> send) {
        new AskFamilyDialog(anchor, context, frame, what, send).setVisible(true);
    }
}
