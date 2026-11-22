package com.sadamda.view.recommend;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.member.MyInfoPanel;
import com.sadamda.view.product.CategoryBar;
import com.sadamda.view.product.ProductGrid;
import com.sadamda.view.product.ProductListPanel;
import com.sadamda.view.product.ShopUi;
import com.sadamda.view.tryon.TryOnPanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * 메인 화면 (프로그램을 켜면 처음 보이는 화면).
 * 검색창, 카테고리로 찾기 (UC-04), 나에게 어울리는 상품 (UC-08 3단계), 이번 시즌 유행 아이템 (UC-08 2단계),
 * 추천 코디 (UC-07), 새로 들어온 상품.
 */
public class HomePanel extends ScreenPanel {
    public static final String NAME = "home";

    /** 홈에서 처음 보여주는 유행 아이템 수. [모두 보기]를 누르면 전부 보여준다. */
    private static final int TRENDING_PREVIEW = 8;
    private static final int LOOKBOOK_PREVIEW = 3;
    private static final int NEWEST_PREVIEW = 8;

    /** 스크롤되는 내용 전체: 맨 위 어두운 띠 + 아래 섹션들. */
    private final JPanel body = new JPanel(new BorderLayout());
    /** 띠 아래 섹션들 (나에게 어울리는 상품, 유행 아이템, …). */
    private final JPanel sections = new JPanel();

    // 맨 위 진보라 띠. 이 배경에서 흰 글씨 대비 5.8:1 (NFR-01).
    private static final Color BAND = new Color(0x6A4BD8);
    /** 띠 위의 연한 글씨 (고르지 않은 탭). 띠 배경 대비 4.9:1 (NFR-01). */
    private static final Color BAND_SOFT_TEXT = new Color(0xEEEAFF);

    private final JTextField searchField = bandSearchField("상품 이름이나 종류를 입력해 보세요");
    private final CategoryBar categoryBar;
    /** 화면을 내렸을 때 맨 위에 고정해 보여주는 같은 탭 줄. */
    private final CategoryBar pinnedBar;
    private final JPanel forMe = new JPanel(new BorderLayout(0, 12));
    private final ProductGrid trendingGrid;
    private final JButton trendingMore = textLink("유행 아이템 모두 보기");
    private final JPanel lookbookRow = new JPanel(new GridLayout(1, LOOKBOOK_PREVIEW, 16, 0));
    private final ProductGrid newestGrid;
    private boolean trendingExpanded;
    private JScrollPane scroll;

    public HomePanel(AppContext context, MainFrame frame) {
        super(context, frame, "홈", "UC-08 유행·맞춤 아이템 추천 보기");
        hideTitle();
        setBackground(ShopUi.CARD);
        setBorder(BorderFactory.createEmptyBorder()); // 맨 위 띠를 창 끝까지 붙인다
        trendingGrid = new ProductGrid(this::openProduct, context, frame);
        newestGrid = new ProductGrid(this::openProduct, context, frame);
        categoryBar = new CategoryBar(context, true, this::openCategory);
        pinnedBar = new CategoryBar(context, true, this::openCategory);
        categoryBar.setColors(BAND_SOFT_TEXT, Color.WHITE, Color.WHITE);
        pinnedBar.setColors(BAND_SOFT_TEXT, Color.WHITE, Color.WHITE);
        setContent(build());
        ShopUi.whenShown(this, this::refresh);
        refresh();
    }

    private JComponent build() {
        body.setBackground(ShopUi.CARD);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setOpaque(false);
        sections.setBorder(BorderFactory.createEmptyBorder(8, 32, 40, 32));
        body.add(band(), BorderLayout.NORTH);
        body.add(sections, BorderLayout.CENTER);

        forMe.setOpaque(false);
        block(forMe, 24);

        trendingMore.addActionListener(e -> {
            trendingExpanded = !trendingExpanded; // [모두 보기] ↔ [줄여 보기]
            renderTrending();
        });
        block(ShopUi.sectionHeader("이번 시즌 유행 아이템", null, null), 40);
        block(textLinkRow(trendingMore), 0); // 안내 문구 바로 아래 (나에게 어울리는 상품의 [로그인하기]와 같은 자리)
        block(trendingGrid, 12);

        block(ShopUi.sectionHeader("추천 코디", "상황·계절별로 어울리는 옷을 한 벌로 묶었어요.", null), 40);
        lookbookRow.setOpaque(false);
        block(lookbookRow, 12);

        // 옷 입혀보기 메뉴는 상단 메뉴(MainFrame, 강준서 담당)에 아직 없어서 홈에서 들어가게 한다.
        block(ShopUi.sectionHeader("옷 입혀보기", "내 체형의 캐릭터에 상품을 입혀 미리 볼 수 있어요.", null), 40);
        JButton tryOn = textLink("옷 입혀보기"); // 안내 문구 바로 아래 밑줄 글자 링크
        tryOn.addActionListener(e -> frame.navigate(TryOnPanel.NAME));
        block(textLinkRow(tryOn), 0);

        block(ShopUi.sectionHeader("새로 들어온 상품", "최근에 등록된 상품이에요.", null), 40);
        block(newestGrid, 12);
        scroll = ShopUi.verticalScroll(body, ShopUi.CARD);
        return withPinnedCategories(scroll);
    }

    /** 맨 위(검색창)로 올린다. 홈에서 상단의 [사담다] 로고를 다시 누를 때 쓴다. */
    public void scrollToTop() {
        scroll.getViewport().setViewPosition(new java.awt.Point(0, 0));
    }

    /**
     * 화면을 내려 맨 위 띠의 카테고리 탭이 위로 사라지면, 맨 위에 같은 카테고리 탭 줄을 고정해 보여준다 (상품 화면 머리말처럼).
     * 내용 위에 겹쳐 그려서 줄이 나타날 때 목록이 밀리지 않는다.
     */
    private JComponent withPinnedCategories(JScrollPane scroll) {
        CategoryBar pinned = pinnedBar;
        pinned.setOpaque(true);
        pinned.setBackground(BAND);
        pinned.setBorder(BorderFactory.createEmptyBorder(4, 32, 0, 32));
        pinned.setVisible(false);

        JPanel screen = new JPanel(null) {
            @Override
            public void doLayout() {
                scroll.setBounds(0, 0, getWidth(), getHeight());
                JScrollBar bar = scroll.getVerticalScrollBar();
                int width = getWidth() - (bar.isVisible() ? bar.getWidth() : 0); // 스크롤바는 가리지 않는다
                pinned.setBounds(0, 0, width, pinned.getPreferredSize().height);
            }

            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false; // 고정 줄이 목록 위에 겹친다
            }
        };
        screen.setOpaque(false);
        screen.add(pinned); // 먼저 넣은 것이 위에 그려진다
        screen.add(scroll);
        scroll.getViewport().addChangeListener(e -> {
            int categoryBottom = SwingUtilities.convertPoint(categoryBar, 0, categoryBar.getHeight(), body).y;
            boolean show = categoryBar.getHeight() > 0 && scroll.getViewport().getViewPosition().y > categoryBottom;
            if (show != pinned.isVisible()) {
                pinned.setVisible(show);
                screen.doLayout();
            }
        });
        return screen;
    }

    /**
     * 맨 위 진보라 띠: 창 폭 가득한 흰 검색창, 그 아래 카테고리 탭 (UC-04, UC-05).
     */
    private JComponent band() {
        JPanel band = new JPanel(new BorderLayout(0, 14));
        band.setBackground(BAND);
        band.setBorder(BorderFactory.createEmptyBorder(20, 32, 0, 32));

        searchField.getAccessibleContext().setAccessibleName("상품 검색");
        searchField.setToolTipText("예: 캐시미어, 로퍼, 버킷햇, 브랜드 이름");
        searchField.addActionListener(e -> search());
        band.add(searchBox(), BorderLayout.CENTER);

        categoryBar.setBorder(BorderFactory.createEmptyBorder()); // 띠 색이 구분선 역할
        categoryBar.getAccessibleContext().setAccessibleName("카테고리로 찾기");
        band.add(categoryBar, BorderLayout.SOUTH);
        return band;
    }

    /** 흰 사각 검색창. 오른쪽 돋보기 버튼을 누르거나 Enter 로 검색한다. 포커스가 있으면 테두리가 굵어진다. */
    private JComponent searchBox() {
        JButton icon = new SearchIconButton();
        icon.addActionListener(e -> search());

        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 6, 6));
                if (searchField.hasFocus()) { // 키보드 사용자를 위한 테두리
                    g2.setColor(ShopUi.PRIMARY);
                    g2.setStroke(new BasicStroke(2f));
                    g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 6, 6));
                }
                g2.dispose();
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 4));
        box.add(searchField, BorderLayout.CENTER);
        box.add(icon, BorderLayout.EAST);
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                box.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                box.repaint();
            }
        });
        return box;
    }

    /**
     * 테두리 없는 검색 입력칸 (흰 상자 searchBox 안에 들어간다). 비어 있으면 회색 안내 문구.
     * 글씨 크기 조절(UC-15) 때 updateUI 가 다시 불려서 모양은 updateUI 에서 지정한다.
     */
    private static JTextField bandSearchField(String hint) {
        return new JTextField(20) {
            @Override
            public void updateUI() {
                super.updateUI();
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0)); // 높이 48px 이상 (NFR-02)
                setForeground(ShopUi.TEXT);
                setCaretColor(ShopUi.TEXT);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getDocument().getLength() > 0) {
                    return;
                }
                Graphics2D g2 = smooth(g);
                g2.setColor(ShopUi.HINT);
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int baseline = in.top + (getHeight() - in.top - in.bottom - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, in.left, baseline);
                g2.dispose();
            }
        };
    }

    /** 검색창 오른쪽 돋보기 버튼. 누르는 영역 48px 이상 (NFR-02). */
    private static final class SearchIconButton extends JButton {
        SearchIconButton() {
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder());
            setPreferredSize(new Dimension(52, 48));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("검색");
            getAccessibleContext().setAccessibleName("검색");
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float size = 20;
            float x = (getWidth() - size) / 2f;
            float y = (getHeight() - size) / 2f;
            g2.setColor(getModel().isRollover() || isFocusOwner() ? ShopUi.TEXT : ShopUi.HINT);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Ellipse2D.Float(x, y, size * 0.75f, size * 0.75f));
            g2.draw(new Line2D.Float(x + size * 0.66f, y + size * 0.66f, x + size, y + size));
            if (isFocusOwner()) { // 키보드 사용자를 위한 테두리
                g2.draw(new RoundRectangle2D.Float(4, 4, getWidth() - 8, getHeight() - 8, 6, 6));
            }
            g2.dispose();
        }
    }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    private void block(JComponent component, int topGap) {
        if (topGap > 0) {
            JPanel gap = new JPanel();
            gap.setOpaque(false);
            gap.setPreferredSize(new Dimension(1, topGap));
            gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, topGap));
            gap.setAlignmentX(Component.LEFT_ALIGNMENT);
            sections.add(gap);
        }
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        sections.add(component);
    }

    /** 화면이 보일 때마다 로그인 상태·관리자 변경을 반영해 다시 그린다. */
    private void refresh() {
        categoryBar.refreshCounts();
        pinnedBar.refreshCounts();
        renderForMe();
        renderTrending();
        renderLookbooks();
        newestGrid.setProducts(context.product.search(new SearchFilter(null, null, null, null, null, null, null,
                ProductService.SORT_NEWEST)).stream().limit(NEWEST_PREVIEW).toList());
        body.revalidate();
        body.repaint();
    }

    /**
     * UC-08 3단계 나에게 어울리는 상품.
     * 비회원: 로그인 안내 / 체형 정보 없음(3a): 연령대 인기 상품과 체형 입력 안내 / 그 외: 연령대·체형 맞춤 상품.
     */
    private void renderForMe() {
        forMe.removeAll();
        if (!context.session.isLoggedIn()) {
            forMe.add(ShopUi.sectionHeader("나에게 어울리는 상품", null, null), BorderLayout.NORTH);
            JButton login = textLink("로그인하기");
            login.addActionListener(e -> frame.navigate(LoginPanel.NAME));
            forMe.add(textLinkRow(login), BorderLayout.CENTER);
            return;
        }
        boolean hasBody = context.recommend.hasBodyType();
        String description = hasBody ? "등록하신 연령대·체형에 잘 맞는 상품이에요."
                : "체형 정보를 입력하면 더 잘 맞는 상품을 골라 드려요. 지금은 같은 연령대의 인기 상품이에요.";
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(ShopUi.sectionHeader("나에게 어울리는 상품", description, null), BorderLayout.NORTH);
        if (!hasBody) { // 3a: 안내 문구 바로 아래에 밑줄 글자 링크 ([로그인하기]와 같은 자리)
            JButton bodyInfo = textLink("체형 정보 입력하기");
            bodyInfo.addActionListener(e -> frame.navigate(MyInfoPanel.NAME));
            top.add(textLinkRow(bodyInfo), BorderLayout.CENTER);
        }
        forMe.add(top, BorderLayout.NORTH);
        ProductGrid grid = new ProductGrid(this::openProduct, context, frame);
        grid.setProducts(context.recommend.recommendedForMe());
        forMe.add(grid, BorderLayout.CENTER);
    }

    private void renderTrending() {
        List<Product> trending = context.recommend.trendingItems();
        trendingGrid.setProducts(trendingExpanded ? trending : trending.stream().limit(TRENDING_PREVIEW).toList());
        trendingMore.setVisible(trending.size() > TRENDING_PREVIEW);
        trendingMore.setText(trendingExpanded ? "줄여 보기" : "유행 아이템 모두 보기 (" + trending.size() + "개)");
    }

    /** 섹션 안내 문구 아래에 두는 밑줄 글자 링크. 누르는 영역은 48px 이상 (NFR-02). */
    private static JButton textLink(String text) {
        JButton link = Theme.linkButton(text);
        link.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 12)); // 왼쪽을 안내 문구와 맞춘다
        // 마우스로 눌렀을 때는 포커스를 가져가지 않아 테두리가 생기지 않는다. Tab 키로 오면 테두리가 보인다 (키보드 사용자).
        link.setRequestFocusEnabled(false);
        return link;
    }

    /** 링크를 왼쪽에 붙여 한 줄로 둔다. */
    private static JPanel textLinkRow(JButton link) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(link, BorderLayout.WEST);
        return row;
    }

    private void renderLookbooks() {
        lookbookRow.removeAll();
        List<Lookbook> lookbooks = context.recommend.lookbooks(null, null);
        for (int i = 0; i < LOOKBOOK_PREVIEW; i++) {
            if (i < lookbooks.size()) {
                Lookbook lookbook = lookbooks.get(i);
                lookbookRow.add(new LookbookCard(lookbook, context.recommend.lookbookProducts(lookbook.getId()),
                        this::openLookbook));
            } else {
                JPanel filler = new JPanel();
                filler.setOpaque(false);
                lookbookRow.add(filler);
            }
        }
    }

    // ---------- 다른 화면으로 ----------

    private void openProduct(Product product) {
        ShopUi.openProduct(frame, context, product);
    }

    /** UC-04 1단계: 카테고리 버튼 → 상품 목록 화면에서 그 카테고리를 보여준다. null 이면 전체 상품. */
    private void openCategory(Category category) {
        // 상품 목록 화면은 강준서 담당. 그 화면에 openCategory(Category) 가 있으면 고른 카테고리로 연다.
        ShopUi.callIfPresent(ShopUi.findScreen(frame, ProductListPanel.class), "openCategory", Category.class, category);
        categoryBar.clearSelection(); // 홈으로 돌아왔을 때 눌린 채로 남지 않게
        pinnedBar.clearSelection();
        frame.navigate(ProductListPanel.NAME);
    }

    /** UC-05 1단계: 검색어 → 상품 목록 화면의 검색 결과. */
    private void search() {
        // 상품 목록 화면에 openSearch(String) 가 있으면 검색 결과로 연다.
        ShopUi.callIfPresent(ShopUi.findScreen(frame, ProductListPanel.class), "openSearch", String.class,
                searchField.getText());
        searchField.setText("");
        frame.navigate(ProductListPanel.NAME);
    }

    private void openLookbook(Lookbook lookbook) {
        LookbookPanel panel = ShopUi.findScreen(frame, LookbookPanel.class);
        if (panel != null) {
            panel.showLookbook(lookbook.getId());
        }
        frame.navigate(LookbookPanel.NAME);
    }
}
