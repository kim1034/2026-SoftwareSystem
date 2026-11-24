package com.sadamda.view;

import com.sadamda.controller.AppContext;
import com.sadamda.model.member.Member;
import com.sadamda.controller.OrderSheet;
import com.sadamda.view.admin.AdminPanel;
import com.sadamda.view.cart.CartPanel;
import com.sadamda.view.member.KeepLogin;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.member.MyInfoPanel;
import com.sadamda.view.member.SignUpPanel;
import com.sadamda.view.order.OrderHistoryPanel;
import com.sadamda.view.order.OrderPanel;
import com.sadamda.view.product.ProductDetailPanel;
import com.sadamda.view.product.ProductListPanel;
import com.sadamda.view.product.ShopUi;
import com.sadamda.view.recommend.HomePanel;
import com.sadamda.view.review.ReviewWritePanel;
import com.sadamda.view.recommend.LookbookPanel;
import com.sadamda.view.tryon.TryOnPanel;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.im.InputContext;
import java.util.ArrayList;
import java.util.List;

/** 메인 창. 상단 메뉴와 CardLayout 으로 화면을 전환한다. 담당: 강준서 */
public class MainFrame extends JFrame {
    static {
        AppFonts.install(); // 원티드 산스. 메뉴 등 필드를 만들기 전에 글꼴부터 바꾼다
    }

    private final AppContext context;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    /**
     * 상단 메뉴 줄의 글씨 크기. [글씨 크기]와 상관없이 "보통"(본문 18px) 기준으로 고정해, 글씨를 키워도 메뉴가 한 줄에 들어가게 한다.
     */
    private static final float MENU_FONT_SIZE = FontSize.NORMAL.px();
    private final JPanel menuBar = new JPanel(new BorderLayout());

    /** 화면 이름이 있는 메뉴. 지금 화면이면 표시한다. */
    private final List<NavButton> screenMenus = new ArrayList<>();
    // 로그인 상태에 따라 보이거나 숨기는 메뉴
    private final IconMenu myInfoMenu = new IconMenu("내 정보", MyInfoPanel.NAME, MenuIcon.Shape.PERSON);
    private final IconMenu loginMenu = new IconMenu("로그인", LoginPanel.NAME, MenuIcon.Shape.PERSON);
    private final IconMenu signUpMenu = new IconMenu("회원가입", SignUpPanel.NAME, MenuIcon.Shape.PERSON_PLUS);
    private final IconMenu logoutMenu = new IconMenu("로그아웃", null, MenuIcon.Shape.LOGOUT);
    private final IconMenu cartMenu = new IconMenu("장바구니", CartPanel.NAME, MenuIcon.Shape.CART);
    private final IconMenu adminMenu = new IconMenu("관리자", AdminPanel.NAME, MenuIcon.Shape.GEAR);
    private final JTextField searchField = new JTextField(16) {
        @Override
        public void updateUI() {
            super.updateUI();
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            setFont(UIManager.getFont("TextField.font").deriveFont(Font.PLAIN, MENU_FONT_SIZE)); // 글씨 크기와 상관없이 고정
            setForeground(Theme.TEXT);
            setCaretColor(Theme.TEXT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getDocument().getLength() == 0) {
                Graphics2D g2 = Theme.smooth(g);
                g2.setColor(Theme.HINT);
                g2.setFont(getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString("상품 검색", 0, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    };

    private String currentScreen;
    /** 로그인 화면으로 오기 직전의 화면. 로그인 후 돌아갈 곳 (UC-02 4단계). */
    private String screenBeforeLogin;

    /** 상품 상세 화면. 어떤 상품을 보여줄지 openProduct 로 알려준다. */
    private final ProductDetailPanel productDetail;
    /** 주문서 화면. 어떤 주문서를 보여줄지 openOrder 로 알려준다. */
    private final OrderPanel orderPanel;
    /** 리뷰 쓰기 화면. 어떤 주문의 리뷰를 쓸지 openReview 로 알려준다. */
    private final ReviewWritePanel reviewWrite;

    public MainFrame(AppContext context) {
        super("사담다");
        this.context = context;
        FontSize.applyPx(FontSize.currentPx());
        installInputMethodGuard();
        productDetail = new ProductDetailPanel(context, this);
        orderPanel = new OrderPanel(context, this);
        reviewWrite = new ReviewWritePanel(context, this);

        addScreen(HomePanel.NAME, new HomePanel(context, this));
        addScreen(ProductListPanel.NAME, new ProductListPanel(context, this));
        addScreen(ProductDetailPanel.NAME, productDetail);
        addScreen(LookbookPanel.NAME, new LookbookPanel(context, this));
        addScreen(TryOnPanel.NAME, new TryOnPanel(context, this));
        addScreen(CartPanel.NAME, new CartPanel(context, this));
        addScreen(OrderPanel.NAME, orderPanel);
        addScreen(OrderHistoryPanel.NAME, new OrderHistoryPanel(context, this));
        addScreen(SignUpPanel.NAME, new SignUpPanel(context, this));
        addScreen(LoginPanel.NAME, new LoginPanel(context, this));
        addScreen(MyInfoPanel.NAME, new MyInfoPanel(context, this));
        addScreen(ReviewWritePanel.NAME, reviewWrite);
        addScreen(AdminPanel.NAME, new AdminPanel(context, this));

        createMenuBar();
        add(menuBar, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 860);
        setLocationRelativeTo(null);
        restoreKeptLogin();
        navigate(HomePanel.NAME);
    }

    /**
     * FR-02 로그인 상태 유지: 지난번에 저장해 둔 회원으로 로그인한다. 쓸 수 없는 저장값이면 지운다.
     * 앱을 닫을 때는 그동안 바뀐 내 정보·글씨 크기를 다시 저장한다.
     */
    private void restoreKeptLogin() {
        Member saved = KeepLogin.saved();
        if (saved != null && context.member.restoreLogin(saved) == null) {
            KeepLogin.forget();
        }
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                Member kept = KeepLogin.saved();
                Member current = context.session.currentMember();
                if (kept != null && current != null && current.getEmail().equals(kept.getEmail())) {
                    KeepLogin.remember(current);
                }
            }
        });
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

    private static boolean inputMethodGuardInstalled;

    /**
     * 한글 입력 중이던 입력칸이 포커스를 잃으면, 그 순간 입력기의 조합(마지막 글자가 아직 확정 안 된 상태)을 끝낸다.
     *
     * macOS 에서 조합 중인 입력칸이 포커스를 잃으면 Java 는 입력기와의 연결만 끊고 macOS 쪽 "조합 중" 상태는 그대로 둔다.
     * 그 상태에서 안내 창(모달)이 떴다 닫혀 메인 창이 다시 활성화되면, 입력기가 마우스 누름 신호를 Java 로 넘기지 않아
     * 모든 버튼이 눌리지 않게 된다 (떼는 신호만 도착함. 한/영 키로 입력 언어를 바꾸면 풀림).
     * Java 가 연결을 끊기 전에 끝내야 해서, 입력기 처리보다 먼저 도는 AWT 이벤트 감시자를 쓴다 (앱 전체에 한 번만).
     */
    private static void installInputMethodGuard() {
        if (inputMethodGuardInstalled) {
            return;
        }
        inputMethodGuardInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof FocusEvent focus && focus.getID() == FocusEvent.FOCUS_LOST && !focus.isTemporary()
                    && focus.getComponent() instanceof JTextComponent text) {
                InputContext input = text.getInputContext();
                if (input != null) {
                    input.endComposition();
                }
            }
        }, AWTEvent.FOCUS_EVENT_MASK);
    }

    /** 상품 목록·룩북·추천에서 상품을 고르면 그 상품의 상세 화면을 연다 (UC-06 1단계). */
    public void openProduct(Long productId) {
        productDetail.showProduct(productId);
        navigate(ProductDetailPanel.NAME);
    }

    /** 장바구니 [주문하기]·상세 [바로 구매]로 만든 주문서를 연다 (UC-11 2단계). */
    public void openOrder(OrderSheet sheet) {
        orderPanel.showSheet(sheet);
        navigate(OrderPanel.NAME);
    }

    /** 주문 내역 [리뷰 쓰기]로 이 주문의 리뷰 쓰기 화면을 연다 (UC-14 1단계). */
    public void openReview(Long orderId) {
        reviewWrite.showOrder(orderId);
        navigate(ReviewWritePanel.NAME);
    }

    /** 리뷰를 등록한 뒤 그 상품의 상세 화면을 열고 리뷰가 보이게 내린다 (UC-14 4단계). */
    public void openProductReviews(Long productId) {
        openProduct(productId);
        productDetail.scrollToReviews();
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
     * 위 줄: 로고 · 가운데 검색창 · 오른쪽 아이콘 메뉴(장바구니·주문내역·내 정보 등).
     * 아래 줄: 가운데 화면 메뉴(홈·전체 상품·추천 룩북·AI 옷 입혀보기), 오른쪽 [글씨 크기].
     * 메뉴 깊이는 1단계 (NFR-03), 누르는 영역은 48px 이상 (NFR-02).
     */
    private void createMenuBar() {
        menuBar.setBackground(Theme.CARD);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE));

        JPanel top = new JPanel(new BorderLayout(24, 0));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(8, 32, 0, 24));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(logo());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        right.setOpaque(false);
        right.add(screenMenu(cartMenu));
        right.add(screenMenu(new IconMenu("주문내역", OrderHistoryPanel.NAME, MenuIcon.Shape.RECEIPT)));
        right.add(screenMenu(myInfoMenu));
        right.add(screenMenu(loginMenu));
        right.add(screenMenu(signUpMenu));
        logoutMenu.addActionListener(e -> logout());
        right.add(logoutMenu);
        right.add(screenMenu(adminMenu));

        JPanel center = new JPanel(new java.awt.GridBagLayout());
        center.setOpaque(false);
        center.add(searchBox());

        top.add(left, BorderLayout.WEST);
        top.add(center, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        tabs.setOpaque(false);
        tabs.add(screenMenu(new TabMenu("홈", HomePanel.NAME)));
        tabs.add(screenMenu(new TabMenu("전체 상품", ProductListPanel.NAME)));
        tabs.add(screenMenu(new TabMenu("추천 룩북", LookbookPanel.NAME)));
        tabs.add(screenMenu(new TabMenu("AI 옷 입혀보기", TryOnPanel.NAME)));

        // 화면 상단 고정. 좌우로 당기면 1px 씩, 누르면 단계 고르기 (UC-15)
        FontSizeSlider fontButton = new FontSizeSlider(this::stepFontSize, FontSize::currentPx,
                this::chooseFontSize, this::saveFontSize);

        // 화면 메뉴는 창 가운데, [글씨 크기]는 오른쪽 끝
        JPanel bottom = new JPanel(null) {
            @Override
            public void doLayout() {
                Dimension t = tabs.getPreferredSize();
                Dimension f = fontButton.getPreferredSize();
                tabs.setBounds((getWidth() - t.width) / 2, 0, t.width, getHeight());
                fontButton.setBounds(getWidth() - f.width - 32, (getHeight() - f.height) / 2, f.width, f.height);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension t = tabs.getPreferredSize();
                return new Dimension(t.width + fontButton.getPreferredSize().width * 2 + 64, t.height);
            }
        };
        bottom.setOpaque(false);
        bottom.add(tabs);
        bottom.add(fontButton);

        menuBar.add(top, BorderLayout.NORTH);
        menuBar.add(bottom, BorderLayout.SOUTH);
    }

    /** 위 줄 가운데의 둥근 골드 테두리 검색창. Enter 나 돋보기 버튼으로 상품 목록 화면의 검색 결과를 연다 (UC-05). */
    private JPanel searchBox() {
        JButton icon = new JButton(new MenuIcon(MenuIcon.Shape.SEARCH, 20, Theme.TEXT)) {
            @Override
            public void updateUI() {
                setUI(new BasicButtonUI());
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.smooth(g);
                g2.setColor(getModel().isRollover() ? Theme.GOLD_BORDER : Theme.GOLD);
                int d = Math.min(getWidth(), getHeight()) - 6;
                g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setContentAreaFilled(false);
        icon.setBorderPainted(false);
        icon.setFocusable(false);
        icon.setRolloverEnabled(true);
        icon.setPreferredSize(new Dimension(44, 44));
        icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        icon.setToolTipText("검색");
        icon.getAccessibleContext().setAccessibleName("검색");
        icon.addActionListener(e -> search());
        searchField.addActionListener(e -> search());
        searchField.getAccessibleContext().setAccessibleName("상품 검색");

        JPanel box = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.smooth(g);
                java.awt.geom.RoundRectangle2D pill = new java.awt.geom.RoundRectangle2D.Float(1, 1,
                        getWidth() - 3, getHeight() - 3, getHeight() - 3, getHeight() - 3);
                g2.setColor(Theme.CARD);
                g2.fill(pill);
                g2.setColor(Theme.GOLD_BORDER);
                g2.setStroke(new java.awt.BasicStroke(searchField.hasFocus() ? 2.4f : 1.6f));
                g2.draw(pill);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(460, super.getPreferredSize().height);
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(2, 24, 2, 4));
        box.add(searchField, BorderLayout.CENTER);
        box.add(icon, BorderLayout.EAST);
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                box.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                box.repaint();
            }
        });
        return box;
    }

    private void search() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            searchField.requestFocusInWindow();
            return;
        }
        ProductListPanel list = ShopUi.findScreen(this, ProductListPanel.class);
        if (list != null) {
            list.openSearch(keyword);
        }
        searchField.setText("");
        navigate(ProductListPanel.NAME);
    }

    /** 왼쪽 끝 로고. 누르면 홈으로 간다. 글씨 크기와 상관없이 고정된 크기로 그린다. */
    private JButton logo() {
        SadamdaLogo logo = new SadamdaLogo(MENU_FONT_SIZE * 1.7f);
        logo.setToolTipText("홈으로");
        logo.setFocusable(false); // 메뉴와 같이 포커스를 받지 않아 테두리가 생기지 않는다
        logo.addActionListener(e -> {
            navigate(HomePanel.NAME);
            HomePanel home = ShopUi.findScreen(this, HomePanel.class);
            if (home != null) {
                home.scrollToTop(); // 이미 홈이면 맨 위로 올린다
            }
        });
        return logo;
    }

    private <T extends NavButton> T screenMenu(T item) {
        item.addActionListener(e -> navigate(item.screenName));
        screenMenus.add(item);
        return item;
    }

    /** 로그인 상태·관리자 여부에 맞춰 메뉴를 보이거나 숨기고, 지금 화면을 표시한다. */
    /** 글씨 크기를 맞춘 회원 (applyAccountFontSize). */
    private Long fontAppliedFor;

    private void updateMenu(String currentScreen) {
        boolean loggedIn = context.session.isLoggedIn();
        applyAccountFontSize();
        myInfoMenu.setVisible(loggedIn);
        logoutMenu.setVisible(loggedIn);
        logoutMenu.setCurrent(false); // 다른 메뉴와 같은 글씨 크기로 (화면이 없는 메뉴라 따로 맞춘다)
        refreshCartBadge();
        loginMenu.setVisible(!loggedIn);
        signUpMenu.setVisible(!loggedIn);
        adminMenu.setVisible(context.session.isAdmin());

        for (NavButton item : screenMenus) {
            item.setCurrent(item.screenName.equals(currentScreen));
        }
        menuBar.revalidate();
        menuBar.repaint();
    }

    /** 로그인한 회원이 바뀌면, 그 계정에 저장해 둔 글씨 크기로 맞춘다 (UC-15 4단계, 다른 PC 에서도 같은 크기). */
    private void applyAccountFontSize() {
        Long memberId = context.session.isLoggedIn() ? context.session.currentMember().getId() : null;
        if (java.util.Objects.equals(memberId, fontAppliedFor)) {
            return;
        }
        fontAppliedFor = memberId;
        String saved = memberId == null ? null : context.member.myInfo().getFontSize();
        int savedPx = FontSize.parse(saved);
        if (savedPx > 0 && savedPx != FontSize.currentPx()) {
            javax.swing.SwingUtilities.invokeLater(() -> FontSize.applyPx(savedPx));
        }
    }

    /** 상단 [장바구니] 아이콘에 담긴 상품 수를 골드 동그라미로 보여 준다. 비회원이거나 비어 있으면 숫자 없이. */
    public void refreshCartBadge() {
        int count = context.session.isLoggedIn() ? context.cart.cart().size() : 0;
        cartMenu.setBadge(count);
        cartMenu.getAccessibleContext().setAccessibleName(count == 0 ? "장바구니" : "장바구니, 상품 " + count + "개");
    }

    private void logout() {
        context.member.logout();
        KeepLogin.forget(); // 로그아웃하면 다음에 자동으로 로그인하지 않는다 (FR-02)
        screenBeforeLogin = null;
        navigate(HomePanel.NAME);
        JOptionPane.showMessageDialog(this, "로그아웃되었습니다."); // 조용히 끝나지 않도록 결과를 알린다
    }

    /** [글씨 크기] 손잡이를 당기는 동안 1px 씩 바꾼다. 18px 밑으로, 32px 위로는 가지 않는다. */
    private void stepFontSize(int delta) {
        int current = FontSize.currentPx();
        int next = FontSize.clamp(current + delta);
        if (next != current) {
            FontSize.applyPx(next);
        }
    }

    /** UC-15 4단계: 회원이면 지금 글씨 크기를 계정에도 저장한다 (다른 PC 에서도 같은 크기). */
    private void saveFontSize() {
        context.member.saveFontSize(String.valueOf(FontSize.currentPx()));
    }

    /** UC-15 1~4단계 */
    private void chooseFontSize() {
        FontSize selected = (FontSize) JOptionPane.showInputDialog(this, "글씨 크기를 고르세요", "글씨 크기",
                JOptionPane.PLAIN_MESSAGE, null, FontSize.values(), FontSize.load());
        if (selected != null) {
            selected.apply();
            saveFontSize();
        }
    }

    /**
     * 상단 메뉴 버튼의 공통 부분. 지금 화면이면 current 로 표시한다.
     * 상단 메뉴는 포커스를 받지 않는다. 화면이 바뀔 때 포커스가 저절로 메뉴로 옮겨 와 테두리가 생기지 않게 한다.
     */
    private abstract static class NavButton extends JButton {
        final String screenName; // 로그아웃처럼 화면이 없는 메뉴는 null
        boolean current;

        NavButton(String label, String screenName) {
            super(label);
            this.screenName = screenName;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(Theme.TEXT);
        }

        void setCurrent(boolean current) {
            this.current = current;
            updateUI(); // 굵기 반영
            repaint();
        }
    }

    /** 아래 줄의 화면 메뉴. 지금 화면이면 굵은 글씨에 골드 밑줄 (현재 위치를 알려 준다). */
    private static final class TabMenu extends NavButton {
        TabMenu(String label, String screenName) {
            super(label, screenName);
            setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14)); // 높이 48px 이상
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
            Font base = UIManager.getFont("Button.font");
            setFont(base.deriveFont(current ? Font.BOLD : Font.PLAIN, MENU_FONT_SIZE + 1)); // [글씨 크기]와 상관없이 고정
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (current) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.GOLD_BORDER);
                g2.fillRect(10, getHeight() - 3, getWidth() - 20, 3);
                g2.dispose();
            }
        }
    }

    /** 위 줄 오른쪽의 아이콘 메뉴: 선 아이콘 아래 작은 이름. 장바구니는 담긴 수를 골드 동그라미로 보여 준다. */
    private static final class IconMenu extends NavButton {
        private int badge;

        IconMenu(String label, String screenName, MenuIcon.Shape shape) {
            super(label, screenName);
            setIcon(new MenuIcon(shape, 24, Theme.TEXT));
            setVerticalTextPosition(BOTTOM);
            setHorizontalTextPosition(CENTER);
            setIconTextGap(4);
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
            Font base = UIManager.getFont("Button.font");
            setFont(base.deriveFont(current ? Font.BOLD : Font.PLAIN, MENU_FONT_SIZE - 3)); // 16px 고정
        }

        void setBadge(int count) {
            badge = count;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (badge > 0) {
                Graphics2D g2 = Theme.smooth(g);
                String text = badge > 99 ? "99+" : String.valueOf(badge);
                g2.setFont(getFont().deriveFont(Font.BOLD, MENU_FONT_SIZE - 5));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int h = fm.getHeight() + 2;
                int w = Math.max(h, fm.stringWidth(text) + 10);
                int x = getWidth() / 2 + 6;
                int y = 2;
                g2.setColor(Theme.GOLD);
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.setColor(Theme.TEXT);
                g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }
}
