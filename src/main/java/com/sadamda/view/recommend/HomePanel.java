package com.sadamda.view.recommend;

import com.sadamda.controller.AppContext;
import com.sadamda.model.member.Member;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.member.MyInfoPanel;
import com.sadamda.view.product.CategoryBar;
import com.sadamda.view.product.ProductGrid;
import com.sadamda.view.product.ProductListPanel;
import com.sadamda.view.product.ShopUi;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
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

    private final JPanel body = new JPanel();
    private final JTextField searchField = new JTextField();
    private final CategoryBar categoryBar;
    private final JPanel greeting = new JPanel(new GridLayout(0, 1, 0, 4));
    private final JPanel forMe = new JPanel(new BorderLayout(0, 12));
    private final ProductGrid trendingGrid;
    private final JButton trendingMore = ShopUi.secondaryButton("유행 아이템 모두 보기");
    private final JPanel lookbookRow = new JPanel(new GridLayout(1, LOOKBOOK_PREVIEW, 16, 0));
    private final ProductGrid newestGrid;
    private boolean trendingExpanded;

    public HomePanel(AppContext context, MainFrame frame) {
        super(context, frame, "홈", "UC-08 유행·맞춤 아이템 추천 보기");
        hideTitle();
        setBackground(ShopUi.PAGE);
        trendingGrid = new ProductGrid(this::openProduct);
        newestGrid = new ProductGrid(this::openProduct);
        categoryBar = new CategoryBar(context, false, this::openCategory);
        setContent(build());
        ShopUi.whenShown(this, this::refresh);
        refresh();
    }

    private JComponent build() {
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ShopUi.PAGE);
        body.setBorder(BorderFactory.createEmptyBorder(8, 8, 32, 8));

        block(hero(), 0);

        block(ShopUi.sectionHeader("카테고리로 찾기", "보고 싶은 종류를 누르세요. 숫자는 판매 중인 상품 수입니다.",
                linkButton("전체 상품 보기", () -> openCategory(null))), 32);
        block(categoryBar, 12);

        forMe.setOpaque(false);
        block(forMe, 40);

        trendingMore.addActionListener(e -> {
            trendingExpanded = !trendingExpanded;
            renderTrending();
        });
        block(ShopUi.sectionHeader("이번 시즌 유행 아이템", "지금 많이 찾는 스타일을 골랐어요.", trendingMore), 40);
        block(trendingGrid, 12);

        block(ShopUi.sectionHeader("추천 코디", "상황·계절별로 어울리는 옷을 한 벌로 묶었어요.",
                linkButton("코디 모두 보기", () -> frame.navigate(LookbookPanel.NAME))), 40);
        lookbookRow.setOpaque(false);
        block(lookbookRow, 12);

        block(ShopUi.sectionHeader("새로 들어온 상품", "최근에 등록된 상품이에요.",
                linkButton("상품 더 보기", () -> openCategory(null))), 40);
        block(newestGrid, 12);
        return ShopUi.verticalScroll(body);
    }

    /** 맨 위: 인사말과 큰 검색창. */
    private JComponent hero() {
        JPanel card = ShopUi.card(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        greeting.setOpaque(false);
        card.add(greeting, BorderLayout.NORTH);

        searchField.setPreferredSize(new Dimension(560, 56));
        searchField.getAccessibleContext().setAccessibleName("상품 검색");
        searchField.setToolTipText("예: 캐시미어, 로퍼, 버킷햇, 브랜드 이름");
        searchField.addActionListener(e -> search());
        JButton search = ShopUi.primaryButton("검색");
        search.setPreferredSize(new Dimension(120, 56));
        search.addActionListener(e -> search());
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(search, BorderLayout.EAST);
        card.add(searchRow, BorderLayout.CENTER);
        return card;
    }

    private void block(JComponent component, int topGap) {
        if (topGap > 0) {
            JPanel gap = new JPanel();
            gap.setOpaque(false);
            gap.setPreferredSize(new Dimension(1, topGap));
            gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, topGap));
            gap.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(gap);
        }
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(component);
    }

    private static JButton linkButton(String text, Runnable action) {
        JButton button = ShopUi.secondaryButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    /** 화면이 보일 때마다 로그인 상태·관리자 변경을 반영해 다시 그린다. */
    private void refresh() {
        renderGreeting();
        categoryBar.refreshCounts();
        renderForMe();
        renderTrending();
        renderLookbooks();
        newestGrid.setProducts(context.product.search(new SearchFilter(null, null, null, null, null, null, null,
                ProductService.SORT_NEWEST)).stream().limit(NEWEST_PREVIEW).toList());
        body.revalidate();
        body.repaint();
    }

    private void renderGreeting() {
        greeting.removeAll();
        Member member = context.member.currentMember();
        String hello = member == null || member.getName() == null ? "어서 오세요, 사담다입니다."
                : member.getName() + "님, 반가워요.";
        greeting.add(ShopUi.label(hello, 1.6f, Font.BOLD, ShopUi.TEXT));
        greeting.add(ShopUi.label("오늘은 어떤 옷을 찾으세요? 상품 이름이나 종류를 입력해 보세요.", 1.05f, Font.PLAIN, ShopUi.HINT));
    }

    /**
     * UC-08 3단계 나에게 어울리는 상품.
     * 비회원: 로그인 안내 / 체형 정보 없음(3a): 연령대 인기 상품과 체형 입력 안내 / 그 외: 연령대·체형 맞춤 상품.
     */
    private void renderForMe() {
        forMe.removeAll();
        if (!context.session.isLoggedIn()) {
            forMe.add(ShopUi.sectionHeader("나에게 어울리는 상품",
                    "로그인하면 연령대와 체형에 맞는 상품을 골라 드려요.",
                    linkButton("로그인하기", () -> frame.navigate(LoginPanel.NAME))), BorderLayout.NORTH);
            return;
        }
        boolean hasBody = context.recommend.hasBodyType();
        JComponent action = hasBody ? null : linkButton("체형 정보 입력하기", () -> frame.navigate(MyInfoPanel.NAME));
        String description = hasBody ? "등록하신 연령대·체형에 잘 맞는 상품이에요."
                : "체형 정보를 입력하면 더 잘 맞는 상품을 골라 드려요. 지금은 같은 연령대의 인기 상품이에요.";
        forMe.add(ShopUi.sectionHeader("나에게 어울리는 상품", description, action), BorderLayout.NORTH);
        ProductGrid grid = new ProductGrid(this::openProduct);
        grid.setProducts(context.recommend.recommendedForMe());
        forMe.add(grid, BorderLayout.CENTER);
    }

    private void renderTrending() {
        List<Product> trending = context.recommend.trendingItems();
        trendingGrid.setProducts(trendingExpanded ? trending : trending.stream().limit(TRENDING_PREVIEW).toList());
        trendingMore.setVisible(trending.size() > TRENDING_PREVIEW);
        trendingMore.setText(trendingExpanded ? "줄여 보기" : "유행 아이템 모두 보기 (" + trending.size() + "개)");
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
        ProductListPanel list = ShopUi.findScreen(frame, ProductListPanel.class);
        if (list != null) {
            list.showCategory(category);
        }
        categoryBar.setSelected(null); // 홈으로 돌아왔을 때 눌린 채로 남지 않게
        frame.navigate(ProductListPanel.NAME);
    }

    /** UC-05 1단계: 검색어 → 상품 목록 화면의 검색 결과. */
    private void search() {
        ProductListPanel list = ShopUi.findScreen(frame, ProductListPanel.class);
        if (list != null) {
            list.showSearch(searchField.getText());
        }
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
