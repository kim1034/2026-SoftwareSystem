package com.sadamda.view;

import com.sadamda.controller.AppContext;
import com.sadamda.view.admin.AdminPanel;
import com.sadamda.view.cart.CartPanel;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.member.MyInfoPanel;
import com.sadamda.view.member.SignUpPanel;
import com.sadamda.view.order.OrderHistoryPanel;
import com.sadamda.view.order.OrderPanel;
import com.sadamda.view.product.ProductDetailPanel;
import com.sadamda.view.product.ProductListPanel;
import com.sadamda.view.recommend.HomePanel;
import com.sadamda.view.recommend.LookbookPanel;
import com.sadamda.view.tryon.TryOnPanel;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;

/** 메인 창. 상단 메뉴와 CardLayout 으로 화면을 전환한다. 담당: 강준서 */
public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    public MainFrame(AppContext context) {
        super("사담다");
        FontSize.load().apply();

        addScreen(HomePanel.NAME, new HomePanel(context, this));
        addScreen(ProductListPanel.NAME, new ProductListPanel(context, this));
        addScreen(ProductDetailPanel.NAME, new ProductDetailPanel(context, this));
        addScreen(LookbookPanel.NAME, new LookbookPanel(context, this));
        addScreen(TryOnPanel.NAME, new TryOnPanel(context, this));
        addScreen(CartPanel.NAME, new CartPanel(context, this));
        addScreen(OrderPanel.NAME, new OrderPanel(context, this));
        addScreen(OrderHistoryPanel.NAME, new OrderHistoryPanel(context, this));
        addScreen(SignUpPanel.NAME, new SignUpPanel(context, this));
        addScreen(LoginPanel.NAME, new LoginPanel(context, this));
        addScreen(MyInfoPanel.NAME, new MyInfoPanel(context, this));
        addScreen(AdminPanel.NAME, new AdminPanel(context, this));

        add(createMenuBar(), BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 860);
        setLocationRelativeTo(null);
    }

    /** 화면 전환. name 은 각 화면 클래스의 NAME 상수. */
    public void navigate(String name) {
        cards.show(content, name);
    }

    private void addScreen(String name, JPanel panel) {
        content.add(panel, name);
    }

    // TODO: 버튼 최소 48x48px (NFR-02), 로그인 상태·관리자 여부에 따라 메뉴 바꾸기
    private JPanel createMenuBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.add(menuButton("홈", HomePanel.NAME));
        bar.add(menuButton("상품", ProductListPanel.NAME));
        bar.add(menuButton("룩북", LookbookPanel.NAME));
        bar.add(menuButton("장바구니", CartPanel.NAME));
        bar.add(menuButton("주문 내역", OrderHistoryPanel.NAME));
        bar.add(menuButton("내 정보", MyInfoPanel.NAME));
        bar.add(menuButton("로그인", LoginPanel.NAME));
        bar.add(menuButton("회원가입", SignUpPanel.NAME));
        bar.add(menuButton("관리자", AdminPanel.NAME));

        JButton fontButton = new JButton("글씨 크기"); // UC-15: 화면 상단 고정
        fontButton.addActionListener(e -> chooseFontSize());
        bar.add(fontButton);
        return bar;
    }

    private JButton menuButton(String label, String screenName) {
        JButton button = new JButton(label);
        button.addActionListener(e -> navigate(screenName));
        return button;
    }

    /** UC-15 1~4단계 */
    private void chooseFontSize() {
        FontSize selected = (FontSize) JOptionPane.showInputDialog(this, "글씨 크기를 고르세요", "글씨 크기",
                JOptionPane.PLAIN_MESSAGE, null, FontSize.values(), FontSize.load());
        if (selected != null) {
            selected.apply();
        }
    }
}
