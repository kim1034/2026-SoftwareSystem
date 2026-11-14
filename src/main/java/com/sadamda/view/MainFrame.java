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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** 메인 창. 상단 메뉴와 CardLayout 으로 화면을 전환한다. 담당: 강준서 */
public class MainFrame extends JFrame {
    private final AppContext context;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel menuBar = new JPanel(new BorderLayout());

    /** 화면 이름이 있는 메뉴. 지금 화면이면 밑줄로 표시한다. */
    private final List<MenuItem> screenMenus = new ArrayList<>();
    // 로그인 상태에 따라 보이거나 숨기는 메뉴
    private final MenuItem myInfoMenu = new MenuItem("내 정보", MyInfoPanel.NAME);
    private final MenuItem loginMenu = new MenuItem("로그인", LoginPanel.NAME);
    private final MenuItem signUpMenu = new MenuItem("회원가입", SignUpPanel.NAME);
    private final MenuItem logoutMenu = new MenuItem("로그아웃", null);
    private final MenuItem adminMenu = new MenuItem("관리자", AdminPanel.NAME);

    private String currentScreen;
    /** 로그인 화면으로 오기 직전의 화면. 로그인 후 돌아갈 곳 (UC-02 4단계). */
    private String screenBeforeLogin;

    /** 상품 상세 화면. 어떤 상품을 보여줄지 openProduct 로 알려준다. */
    private final ProductDetailPanel productDetail;

    public MainFrame(AppContext context) {
        super("사담다");
        this.context = context;
        FontSize.load().apply();
        productDetail = new ProductDetailPanel(context, this);

        addScreen(HomePanel.NAME, new HomePanel(context, this));
        addScreen(ProductListPanel.NAME, new ProductListPanel(context, this));
        addScreen(ProductDetailPanel.NAME, productDetail);
        addScreen(LookbookPanel.NAME, new LookbookPanel(context, this));
        addScreen(TryOnPanel.NAME, new TryOnPanel(context, this));
        addScreen(CartPanel.NAME, new CartPanel(context, this));
        addScreen(OrderPanel.NAME, new OrderPanel(context, this));
        addScreen(OrderHistoryPanel.NAME, new OrderHistoryPanel(context, this));
        addScreen(SignUpPanel.NAME, new SignUpPanel(context, this));
        addScreen(LoginPanel.NAME, new LoginPanel(context, this));
        addScreen(MyInfoPanel.NAME, new MyInfoPanel(context, this));
        addScreen(AdminPanel.NAME, new AdminPanel(context, this));

        createMenuBar();
        add(menuBar, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 860);
        setLocationRelativeTo(null);
        navigate(HomePanel.NAME);
    }

    /**
     * 화면 전환. name 은 각 화면 클래스의 NAME 상수.
     * 로그인·로그아웃 뒤에도 이 메서드로 이동하므로, 여기서 메뉴를 로그인 상태에 맞춘다.
     *
     * 로그인 화면으로 갈 때는 직전 화면을 기억해 두었다가, 로그인 후 returnAfterLogin 이 그 화면으로 돌려보낸다 (UC-02 4단계).
     * 로그인이 필요한 화면은 LoginRequiredException 을 받으면 navigate(LoginPanel.NAME) 만 부르면 된다 (docs/controller-spec.md).
     */
    public void navigate(String name) {
        boolean toLoginFlow = name.equals(LoginPanel.NAME) || name.equals(SignUpPanel.NAME);
        if (toLoginFlow && !isLoginFlow(currentScreen)) {
            screenBeforeLogin = currentScreen; // 회원가입 링크를 거쳐도 처음 보던 화면을 유지한다
        }
        cards.show(content, name);
        currentScreen = name;
        updateMenu(name);
    }

    /** 상품 목록·룩북·추천에서 상품을 고르면 그 상품의 상세 화면을 연다 (UC-06 1단계). */
    public void openProduct(Long productId) {
        productDetail.showProduct(productId);
        navigate(ProductDetailPanel.NAME);
    }

    /** 로그인 성공 후 일반 회원을 로그인 전에 보던 화면으로 돌려보낸다. 기억한 화면이 없으면 홈. */
    public void returnAfterLogin() {
        String target = screenBeforeLogin == null ? HomePanel.NAME : screenBeforeLogin;
        screenBeforeLogin = null;
        navigate(target);
    }

    private static boolean isLoginFlow(String screen) {
        return LoginPanel.NAME.equals(screen) || SignUpPanel.NAME.equals(screen);
    }

    private void addScreen(String name, JPanel panel) {
        content.add(panel, name);
    }

    /**
     * 왼쪽: 로고·둘러보기 메뉴, 오른쪽: 내 쇼핑 메뉴와 [글씨 크기].
     * 메뉴 깊이는 1단계 (NFR-03), 누르는 영역은 48px 이상 (NFR-02).
     */
    private void createMenuBar() {
        menuBar.setBackground(Theme.CARD);
        menuBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(4, 24, 4, 24)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        left.add(logo());
        left.add(Box.createHorizontalStrut(24));
        left.add(screenMenu("홈", HomePanel.NAME));
        left.add(screenMenu("상품", ProductListPanel.NAME));
        left.add(screenMenu("룩북", LookbookPanel.NAME));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.add(screenMenu("장바구니", CartPanel.NAME));
        right.add(screenMenu("주문 내역", OrderHistoryPanel.NAME));
        right.add(screenMenu(myInfoMenu));
        right.add(screenMenu(loginMenu));
        right.add(screenMenu(signUpMenu));
        logoutMenu.addActionListener(e -> logout());
        right.add(logoutMenu);
        right.add(screenMenu(adminMenu));
        right.add(Box.createHorizontalStrut(12));

        JButton fontButton = Theme.secondaryButton("글씨 크기"); // UC-15: 화면 상단 고정
        fontButton.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        fontButton.addActionListener(e -> chooseFontSize());
        right.add(fontButton);

        menuBar.add(left, BorderLayout.WEST);
        menuBar.add(right, BorderLayout.EAST);
    }

    /** 왼쪽 끝 로고. 누르면 홈으로 간다. */
    private JButton logo() {
        JButton logo = new JButton("사담다") {
            @Override
            public void updateUI() {
                setUI(new BasicButtonUI());
                Font base = UIManager.getFont("Button.font");
                setFont(base.deriveFont(Font.BOLD, base.getSize2D() * 1.5f));
            }
        };
        logo.setForeground(Theme.PRIMARY);
        logo.setContentAreaFilled(false);
        logo.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logo.setToolTipText("홈으로");
        logo.addActionListener(e -> navigate(HomePanel.NAME));
        return logo;
    }

    private MenuItem screenMenu(String label, String screenName) {
        return screenMenu(new MenuItem(label, screenName));
    }

    private MenuItem screenMenu(MenuItem item) {
        item.addActionListener(e -> navigate(item.screenName));
        screenMenus.add(item);
        return item;
    }

    /** 로그인 상태·관리자 여부에 맞춰 메뉴를 보이거나 숨기고, 지금 화면을 표시한다. */
    private void updateMenu(String currentScreen) {
        boolean loggedIn = context.session.isLoggedIn();
        myInfoMenu.setVisible(loggedIn);
        logoutMenu.setVisible(loggedIn);
        loginMenu.setVisible(!loggedIn);
        signUpMenu.setVisible(!loggedIn);
        adminMenu.setVisible(context.session.isAdmin());

        for (MenuItem item : screenMenus) {
            item.setCurrent(item.screenName.equals(currentScreen));
        }
        menuBar.revalidate();
        menuBar.repaint();
    }

    private void logout() {
        context.member.logout();
        screenBeforeLogin = null;
        navigate(HomePanel.NAME);
        JOptionPane.showMessageDialog(this, "로그아웃되었습니다."); // 조용히 끝나지 않도록 결과를 알린다
    }

    /** UC-15 1~4단계 */
    private void chooseFontSize() {
        FontSize selected = (FontSize) JOptionPane.showInputDialog(this, "글씨 크기를 고르세요", "글씨 크기",
                JOptionPane.PLAIN_MESSAGE, null, FontSize.values(), FontSize.load());
        if (selected != null) {
            selected.apply();
        }
    }

    /**
     * 글자로 된 메뉴. 지금 화면이면 굵은 글씨에 밑줄을 긋는다 (현재 위치를 알려 준다).
     * 글씨는 버튼 기준(본문 +2px, NFR-01), 키보드 포커스가 오면 테두리를 그린다.
     */
    private static final class MenuItem extends JButton {
        private final String screenName; // 로그아웃처럼 화면이 없는 메뉴는 null
        private boolean current;

        MenuItem(String label, String screenName) {
            super(label);
            this.screenName = screenName;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12)); // 높이 48px 이상
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(Theme.TEXT);
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
            Font base = UIManager.getFont("Button.font");
            setFont(base.deriveFont(current ? Font.BOLD : Font.PLAIN, base.getSize2D() + 2));
        }

        void setCurrent(boolean current) {
            this.current = current;
            setForeground(current ? Theme.PRIMARY : Theme.TEXT);
            updateUI(); // 굵기 반영
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (current) {
                g2.setColor(Theme.PRIMARY);
                g2.fillRect(12, getHeight() - 6, getWidth() - 24, 3);
            }
            if (isFocusOwner()) {
                g2.setColor(Theme.PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 10, 10));
            }
            g2.dispose();
        }
    }
}
