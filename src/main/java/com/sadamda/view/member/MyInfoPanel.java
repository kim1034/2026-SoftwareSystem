package com.sadamda.view.member;

import com.sadamda.common.AppException;
import com.sadamda.common.LoginRequiredException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import static com.sadamda.view.member.SignUpPanel.AGE_GROUPS;
import static com.sadamda.view.member.SignUpPanel.BODY_TYPES;
import static com.sadamda.view.member.SignUpPanel.NOT_SELECTED;
import static com.sadamda.view.member.SignUpPanel.SIZES;

/** 연락처·기본 배송지·연령대·체형 정보 수정. 이름·이메일은 보기만 한다. */
public class MyInfoPanel extends ScreenPanel {
    public static final String NAME = "myInfo";

    private final JLabel nameLabel = Theme.label(" ", 1.6f, Font.BOLD, Theme.TEXT);
    private final JLabel emailLabel = Theme.label(" ", 1f, Font.PLAIN, Theme.HINT);

    private final JTextField phoneField = Theme.textField("예: 010-1234-5678");
    private final JTextField addressField = Theme.textField("예: 서울시 종로구 세종대로 1, 101동 101호");
    private final JComboBox<String> ageGroupBox = Theme.comboBox(AGE_GROUPS);
    private final JTextField heightField = Theme.textField("예: 160");
    private final JComboBox<String> sizeBox = Theme.comboBox(SIZES);
    private final JComboBox<String> bodyTypeBox = Theme.comboBox(BODY_TYPES);

    // UC-03 4a: 형식이 틀린 항목은 입력란 이름 옆에 이유를 표시한다.
    private final JLabel phoneError = Theme.errorLabel();
    private final JLabel heightError = Theme.errorLabel();
    /** [저장하기] 실패 시 맨 위에 보여주는 오류 요약 또는 Model 이 보낸 메시지. */
    private final JLabel summaryError = Theme.errorLabel();

    public MyInfoPanel(AppContext context, MainFrame frame) {
        super(context, frame, "내 정보", "UC-03 회원·체형 정보 관리");
        hideTitle(); // 제목 대신 카드 맨 위에 회원 이름을 크게 보여준다.
        setBackground(Theme.PAGE);
        heightField.setColumns(6); // 두 칸짜리 줄이 카드 폭을 넓히지 않도록 짧게
        // 키 옆 오류 문구가 들어갈 만큼 카드를 미리 넓혀 둔다. (오류가 뜰 때 카드 폭이 바뀌지 않게)
        // 글자 수 기준이라 글씨 크기를 바꿔도 함께 늘어난다.
        addressField.setColumns(38);

        JPanel card = Theme.card();
        Theme.addToCard(card, nameLabel, 0);
        Theme.addToCard(card, emailLabel, 8);
        Theme.addToCard(card, summaryError, 16);

        Theme.addToCard(card, Theme.label("연락처와 배송지", 1.2f, Font.BOLD, Theme.TEXT), 16);
        Theme.addToCard(card, Theme.field("연락처", phoneError, phoneField), 16);
        Theme.addToCard(card, Theme.field("기본 배송지", null, addressField), 20);

        Theme.addToCard(card, Theme.label("체형 정보", 1.2f, Font.BOLD, Theme.TEXT), 40);
        Theme.addToCard(card, Theme.label("저장하면 추천 옷에 바로 반영돼요", 1f, Font.PLAIN, Theme.HINT), 4);
        Theme.addToCard(card, SignUpPanel.twoColumns(Theme.field("연령대", null, ageGroupBox),
                Theme.field("키 (cm)", heightError, heightField)), 16);
        Theme.addToCard(card, SignUpPanel.twoColumns(Theme.field("평소 사이즈", null, sizeBox),
                Theme.field("체형", null, bodyTypeBox)), 20);

        JButton saveButton = Theme.primaryButton("저장하기");
        saveButton.addActionListener(e -> save());
        JButton revertButton = Theme.secondaryButton("되돌리기");
        revertButton.addActionListener(e -> load()); // 바꾼 내용을 버리고 저장된 값으로 되돌린다.
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.setOpaque(false);
        buttons.add(saveButton);
        buttons.add(revertButton);
        Theme.addToCard(card, buttons, 40);

        setContent(Theme.centeredPage(card));

        // 칸을 벗어나면 그 칸만 바로 검사한다.
        checkOnLeave(phoneField, this::checkPhone);
        checkOnLeave(heightField, this::checkHeight);

        // UC-03 2단계: 화면을 열 때마다 저장된 정보를 다시 불러온다.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                load();
            }
        });
    }

    /** 저장된 회원 정보로 화면을 채우고 오류 표시를 지운다. 로그인 전이면 로그인 화면으로 보낸다. */
    private void load() {
        Member member;
        try {
            member = context.member.myInfo();
        } catch (LoginRequiredException e) {
            frame.navigate(LoginPanel.NAME); // 로그인하면 이 화면으로 돌아온다.
            return;
        }

        nameLabel.setText(member.getName() + " 님");
        emailLabel.setText(member.getEmail());
        phoneField.setText(nullToEmpty(member.getPhone()));
        addressField.setText(nullToEmpty(member.getDefaultAddress()));

        BodyProfile body = member.getBodyProfile();
        select(ageGroupBox, body == null ? null : body.ageGroup());
        heightField.setText(body == null || body.heightCm() == null ? "" : String.valueOf(body.heightCm()));
        select(sizeBox, body == null ? null : body.usualSize());
        select(bodyTypeBox, body == null ? null : body.bodyType());

        SignUpPanel.show(phoneError, phoneField, null);
        SignUpPanel.show(heightError, heightField, null);
        summaryError.setText("");
    }

    /** UC-03 3~4단계 */
    private void save() {
        boolean phoneOk = checkPhone();
        boolean heightOk = checkHeight();
        if (!phoneOk || !heightOk) {
            showSummary();
            return;
        }
        summaryError.setText("");

        try {
            context.member.updateProfile(phoneField.getText().trim(), addressField.getText().trim(), bodyProfile());
        } catch (LoginRequiredException e) {
            frame.navigate(LoginPanel.NAME);
            return;
        } catch (AppException e) {
            // Model 이 보낸 형식 오류(4a) 등. 메시지는 사용자에게 보여줄 문장으로 온다.
            summaryError.setText(e.getMessage());
            summaryError.scrollRectToVisible(new Rectangle(summaryError.getSize()));
            return;
        }

        JOptionPane.showMessageDialog(this, "내 정보를 저장했습니다.");
        load(); // Model 이 다듬은 값(예: 연락처 하이픈)을 화면에 보여준다.
    }

    /** 체형 정보 칸을 묶는다. "선택 안 함"·빈칸은 null, 넷 다 비었으면 null. */
    private BodyProfile bodyProfile() {
        String ageGroup = SignUpPanel.selected(ageGroupBox);
        String height = heightField.getText().trim();
        Integer heightCm = height.isEmpty() ? null : Integer.valueOf(height); // checkHeight 를 통과한 값
        String usualSize = SignUpPanel.selected(sizeBox);
        String bodyType = SignUpPanel.selected(bodyTypeBox);
        if (ageGroup == null && heightCm == null && usualSize == null && bodyType == null) {
            return null;
        }
        return new BodyProfile(ageGroup, heightCm, usualSize, bodyType);
    }

    /** 맨 위에 틀린 곳 개수를 알리고, 첫 번째 틀린 칸으로 화면을 옮겨 커서를 둔다. */
    private void showSummary() {
        summaryError.setText(" "); // updateSummary 가 갱신하도록 표시만 켠다.
        updateSummary();
        JTextField first = phoneError.getText().isEmpty() ? heightField : phoneField;
        // 칸 위의 이름표·오류 문구까지 보이도록 칸 높이만큼 위쪽을 더 보여준다.
        // (JTextField 는 스크롤 요청을 자기 글자 스크롤로만 써서, 칸 대신 카드에 요청한다)
        JPanel card = (JPanel) summaryError.getParent();
        Rectangle area = new Rectangle(first.getX(), first.getY() - first.getHeight(), first.getWidth(), first.getHeight() * 2);
        card.scrollRectToVisible(SwingUtilities.convertRectangle(first.getParent(), area, card));
        first.requestFocusInWindow();
    }

    /** 요약이 떠 있으면 남은 오류 개수로 고치고, 다 고쳤으면 지운다. */
    private void updateSummary() {
        if (summaryError.getText().isEmpty()) {
            return;
        }
        int count = (phoneError.getText().isEmpty() ? 0 : 1) + (heightError.getText().isEmpty() ? 0 : 1);
        summaryError.setText(count == 0 ? "" : "입력 내용을 확인해 주세요 (" + count + "곳)");
    }

    private boolean checkPhone() {
        return SignUpPanel.show(phoneError, phoneField, SignUpPanel.phoneMessage(phoneField.getText().trim()));
    }

    private boolean checkHeight() {
        return SignUpPanel.show(heightError, heightField, SignUpPanel.heightMessage(heightField.getText().trim()));
    }

    /** 칸에서 포커스가 나가면 그 칸을 검사하고 요약 개수를 고친다. */
    private void checkOnLeave(JTextComponent field, Runnable check) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                check.run();
                updateSummary();
            }
        });
    }

    /** 저장된 값을 고른다. 없거나 선택지에 없는 값이면 "선택 안 함". */
    private static void select(JComboBox<String> box, String value) {
        box.setSelectedItem(NOT_SELECTED);
        if (value != null) {
            box.setSelectedItem(value);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
