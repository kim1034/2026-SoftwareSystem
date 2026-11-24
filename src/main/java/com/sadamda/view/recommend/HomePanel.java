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
import com.sadamda.view.product.ProductListPanel;
import com.sadamda.view.product.ShopUi;
import com.sadamda.view.product.WishActions;
import com.sadamda.view.tryon.TryOnPanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 메인 화면 (프로그램을 켜면 처음 보이는 화면).
 * 큰 배너, 카테고리로 찾기 (UC-04), 체형 맞춤 추천·옷 입혀보기 바로가기, 지금 뜨는 베스트 (UC-08 2단계),
 * 나에게 어울리는 상품 (UC-08 3단계), 이달의 추천 코디 (UC-07), 새로 들어온 상품.
 */
public class HomePanel extends ScreenPanel {
    public static final String NAME = "home";

    /** 베스트에서 처음 보여주는 수. [더 보기]를 누르면 전부 보여준다. */
    private static final int BEST_PREVIEW = 4;
    private static final int CARD_COLUMNS = 4;
    /** 나에게 어울리는 상품에서 처음 보여주는 수. */
    private static final int FOR_ME_PREVIEW = 4;
    private static final int LOOKBOOK_PREVIEW = 3;
    private static final int NEWEST_PREVIEW = 8;
    /** 홈 카테고리 칸에 보여 주는 분류. */
    private static final List<Category> TILE_CATEGORIES = List.of(Category.TOP, Category.BOTTOM, Category.DRESS,
            Category.OUTER, Category.HAT, Category.SHOES, Category.BAG, Category.SCARF, Category.JEWELRY, Category.SOCKS);
    /** 베스트 분류 알약. 유행 아이템에 있는 분류만 보인다. */
    private static final List<Category> BEST_FILTERS = List.of(Category.TOP, Category.OUTER, Category.BOTTOM,
            Category.DRESS, Category.BAG, Category.SCARF, Category.SHOES, Category.HAT);

    private final JPanel sections = new JPanel();
    private final JPanel categoryRow = new JPanel(new GridLayout(1, TILE_CATEGORIES.size(), 12, 0));
    private final JPanel bestFilters = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    private final JPanel bestGrid = cardGrid();
    private final JButton bestMore = textLink("베스트 더 보기");
    private final JPanel forMe = new JPanel(new BorderLayout(0, 12));
    private final JPanel lookbookBanner = new JPanel(new BorderLayout());
    private final JPanel lookbookRow = new JPanel(new GridLayout(1, LOOKBOOK_PREVIEW, 16, 0));
    private final JPanel newestGrid = cardGrid();
    private final WishActions wish;
    private Category bestCategory;
    private boolean bestExpanded;
    private boolean forMeExpanded;
    private JScrollPane scroll;

    public HomePanel(AppContext context, MainFrame frame) {
        super(context, frame, "홈", "UC-08 유행·맞춤 아이템 추천 보기");
        hideTitle();
        setBackground(ShopUi.CARD);
        setBorder(BorderFactory.createEmptyBorder()); // 배너를 창 끝까지 붙인다
        wish = new WishActions(context, frame, this);
        setContent(build());
        ShopUi.whenShown(this, this::refresh);
        refresh();
    }

    private JComponent build() {
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setBackground(ShopUi.CARD);
        sections.setBorder(BorderFactory.createEmptyBorder(0, 0, 56, 0));

        full(hero());

        categoryRow.setOpaque(false);
        categoryRow.getAccessibleContext().setAccessibleName("카테고리로 찾기");
        inner(categoryRow, 28);
        inner(promoRow(), 32);

        JLabel bestTitle = Theme.label("지금 뜨는 베스트", 1.45f, Font.BOLD, Theme.TEXT);
        bestTitle.setHorizontalAlignment(SwingConstants.CENTER);
        inner(bestTitle, 56);
        bestFilters.setOpaque(false);
        inner(bestFilters, 16);
        inner(bestGrid, 24);
        bestMore.addActionListener(e -> {
            bestExpanded = !bestExpanded; // [더 보기] ↔ [줄여 보기]
            renderBest();
        });
        JPanel moreRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        moreRow.setOpaque(false);
        moreRow.add(bestMore);
        inner(moreRow, 8);

        forMe.setOpaque(false);
        inner(forMe, 40);

        inner(Theme.label("이달의 추천 코디", 1.45f, Font.BOLD, Theme.TEXT), 48);
        lookbookBanner.setOpaque(false);
        full(lookbookBanner, 16);
        lookbookRow.setOpaque(false);
        inner(lookbookRow, 24);

        inner(Theme.label("새로 들어온 상품", 1.45f, Font.BOLD, Theme.TEXT), 56);
        inner(newestGrid, 16);
        scroll = ShopUi.verticalScroll(sections, ShopUi.CARD);
        return scroll;
    }

    /** 맨 위로 올린다. 홈에서 상단의 [사담다] 로고를 다시 누를 때 쓴다. */
    public void scrollToTop() {
        scroll.getViewport().setViewPosition(new Point(0, 0));
    }

    /** 맨 위 큰 배너: 시즌 꼬리표, 큰 제목, [컬렉션 보기] → 전체 상품. */
    private JComponent hero() {
        JButton collection = Theme.primaryButton("컬렉션 보기");
        collection.addActionListener(e -> frame.navigate(ProductListPanel.NAME));
        JPanel text = HomeParts.stack(HomeParts.goldTag("2026 F/W"), Theme.label("사담다 클래식 컬렉션", 2.2f, Font.BOLD, Theme.TEXT),
                collection);
        return new HomeParts.Banner("hero.jpg", Theme.SOFT, 340, 0.85, text);
    }

    /** 체형 맞춤 추천 · AI 옷 입혀보기 바로가기 카드 두 장. */
    private JComponent promoRow() {
        JButton forMeCard = new HomeParts.PromoCard("체형 맞춤 추천", Theme.PROMO_ROSE, "promo_body.png");
        forMeCard.addActionListener(e -> openForMe());
        JButton tryOnCard = new HomeParts.PromoCard("AI 옷 입혀보기", Theme.PROMO_OLIVE, "promo_tryon.png");
        tryOnCard.addActionListener(e -> frame.navigate(TryOnPanel.NAME));
        JPanel row = new JPanel(new GridLayout(1, 2, 20, 0));
        row.setOpaque(false);
        row.add(forMeCard);
        row.add(tryOnCard);
        return row;
    }

    /** [체형 맞춤 추천]: 비회원은 로그인, 체형 정보가 없으면 내 정보, 있으면 아래 "나에게 어울리는 상품"으로 내린다. */
    private void openForMe() {
        if (!context.session.isLoggedIn()) {
            frame.navigate(LoginPanel.NAME);
        } else if (!context.recommend.hasBodyType()) {
            frame.navigate(MyInfoPanel.NAME);
        } else {
            Rectangle target = SwingUtilities.convertRectangle(forMe.getParent(), forMe.getBounds(), sections);
            scroll.getViewport().setViewPosition(new Point(0, Math.max(0, Math.min(target.y - 24,
                    sections.getHeight() - scroll.getViewport().getHeight()))));
        }
    }

    /** 창 폭 가득 붙이는 줄 (배너). */
    private void full(JComponent component) {
        full(component, 0);
    }

    private void full(JComponent component, int topGap) {
        gap(topGap);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        sections.add(component);
    }

    /** 양옆 여백을 둔 줄. */
    private void inner(JComponent component, int topGap) {
        gap(topGap);
        JPanel padded = new JPanel(new BorderLayout());
        padded.setOpaque(false);
        padded.setBorder(BorderFactory.createEmptyBorder(0, 56, 0, 56));
        padded.add(component, BorderLayout.CENTER);
        padded.setAlignmentX(Component.LEFT_ALIGNMENT);
        // 숨긴 줄(예: 비회원의 나에게 어울리는 상품)은 자리도 차지하지 않게 한다
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                padded.setVisible(true);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                padded.setVisible(false);
            }
        });
        sections.add(padded);
    }

    private void gap(int height) {
        if (height > 0) {
            JPanel gap = new JPanel();
            gap.setOpaque(false);
            gap.setPreferredSize(new Dimension(1, height));
            gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            gap.setAlignmentX(Component.LEFT_ALIGNMENT);
            sections.add(gap);
        }
    }

    /** 화면이 보일 때마다 로그인 상태·관리자 변경을 반영해 다시 그린다. */
    private void refresh() {
        renderCategories();
        renderBestFilters();
        renderBest();
        renderForMe();
        renderLookbooks();
        fillCards(newestGrid, context.product.search(new SearchFilter(null, null, null, null, null, null, null,
                ProductService.SORT_NEWEST)).stream().limit(NEWEST_PREVIEW).toList(), false);
        sections.revalidate();
        sections.repaint();
    }

    /** 카테고리 칸: 분류별 아이콘 그림 (icons/category_top.png 등). 상품이 없는 분류는 빼고 남은 칸을 비운다. */
    private void renderCategories() {
        categoryRow.removeAll();
        int shown = 0;
        for (Category category : TILE_CATEGORIES) {
            if (context.product.listByCategory(category, 0).isEmpty()) {
                continue;
            }
            HomeParts.CategoryTile tile = new HomeParts.CategoryTile(category.label(),
                    "category_" + category.name().toLowerCase(java.util.Locale.ROOT) + ".png");
            tile.addActionListener(e -> openCategory(category));
            categoryRow.add(tile);
            shown++;
        }
        for (; shown < TILE_CATEGORIES.size(); shown++) {
            JPanel filler = new JPanel();
            filler.setOpaque(false);
            categoryRow.add(filler);
        }
    }

    private void renderBestFilters() {
        bestFilters.removeAll();
        List<Product> trending = context.recommend.trendingItems();
        ButtonGroup group = new ButtonGroup();
        List<Category> options = new ArrayList<>();
        options.add(null);
        BEST_FILTERS.stream().filter(c -> trending.stream().anyMatch(p -> p.getCategory() == c)).forEach(options::add);
        if (!options.contains(bestCategory)) {
            bestCategory = null;
        }
        for (Category option : options) {
            JToggleButton pill = HomeParts.goldPill(option == null ? "전체" : option.label());
            pill.setSelected(option == bestCategory);
            pill.addActionListener(e -> {
                bestCategory = option;
                bestExpanded = false;
                renderBest();
            });
            group.add(pill);
            bestFilters.add(pill);
        }
    }

    /** UC-08 2단계 유행 아이템을 순위 카드로. 분류 알약으로 거른다. */
    private void renderBest() {
        List<Product> items = context.recommend.trendingItems().stream()
                .filter(p -> bestCategory == null || p.getCategory() == bestCategory).toList();
        fillCards(bestGrid, bestExpanded ? items : items.stream().limit(BEST_PREVIEW).toList(), true);
        bestMore.setVisible(items.size() > BEST_PREVIEW);
        bestMore.setText(bestExpanded ? "줄여 보기" : "베스트 더 보기 (" + items.size() + "개)");
    }

    /** 4열 상품 카드 판. 사진 칸을 정사각형으로 두려고, 폭이 바뀌면 높이를 다시 잰다. */
    private JPanel cardGrid() {
        JPanel grid = new JPanel(new GridLayout(0, CARD_COLUMNS, 20, 28));
        grid.setOpaque(false);
        grid.addComponentListener(new ComponentAdapter() {
            private int lastWidth = -1;

            @Override
            public void componentResized(ComponentEvent e) {
                if (grid.getWidth() != lastWidth) {
                    lastWidth = grid.getWidth();
                    SwingUtilities.invokeLater(() -> {
                        grid.invalidate();
                        sections.revalidate();
                    });
                }
            }
        });
        return grid;
    }

    /** 카드 판을 채운다. ranked 면 01, 02 … 순위 뱃지를 단다. */
    private void fillCards(JPanel grid, List<Product> products, boolean ranked) {
        grid.removeAll();
        for (int i = 0; i < products.size(); i++) {
            grid.add(new HomeParts.RankCard(products.get(i), ranked ? i + 1 : 0, this::openProduct, wish));
        }
        for (int i = products.size(); i % CARD_COLUMNS != 0; i++) { // 마지막 줄 칸 수를 맞춘다
            JPanel filler = new JPanel();
            filler.setOpaque(false);
            grid.add(filler);
        }
        grid.revalidate();
        grid.repaint();
    }

    /**
     * UC-08 3단계 나에게 어울리는 상품. 로그인한 회원에게만 보인다.
     * 체형 정보가 없으면(3a) 같은 연령대 인기 상품과 [체형 정보 입력하기].
     */
    private void renderForMe() {
        forMe.removeAll();
        boolean loggedIn = context.session.isLoggedIn();
        forMe.setVisible(loggedIn);
        if (!loggedIn) {
            return;
        }
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.label("나에게 어울리는 상품", 1.45f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        if (!context.recommend.hasBodyType()) {
            JButton bodyInfo = textLink("체형 정보 입력하기 ›");
            bodyInfo.addActionListener(e -> frame.navigate(MyInfoPanel.NAME));
            top.add(bodyInfo, BorderLayout.EAST);
        }
        List<Product> products = context.recommend.recommendedForMe();
        if (products.size() > FOR_ME_PREVIEW) {
            JButton more = textLink(forMeExpanded ? "줄여 보기" : "더 보기 (" + products.size() + "개)");
            more.addActionListener(e -> {
                forMeExpanded = !forMeExpanded;
                renderForMe();
                sections.revalidate();
            });
            JPanel moreRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            moreRow.setOpaque(false);
            moreRow.add(more);
            forMe.add(moreRow, BorderLayout.SOUTH);
        }
        forMe.add(top, BorderLayout.NORTH);
        JPanel grid = cardGrid();
        fillCards(grid, forMeExpanded ? products : products.stream().limit(FOR_ME_PREVIEW).toList(), false);
        forMe.add(grid, BorderLayout.CENTER);
    }

    /** 이달의 추천 코디: 첫 코디는 큰 배너로, 다음 코디들은 카드로. */
    private void renderLookbooks() {
        lookbookBanner.removeAll();
        lookbookRow.removeAll();
        List<Lookbook> lookbooks = context.recommend.lookbooks(null, null);
        lookbookBanner.setVisible(!lookbooks.isEmpty());
        if (!lookbooks.isEmpty()) {
            Lookbook first = lookbooks.get(0);
            JLabel more = Theme.label("코디 보기 ›", 1.05f, Font.BOLD, Theme.TEXT);
            JComponent banner = new HomeParts.Banner("lookbook.jpg", Theme.SECONDARY, 300, 1.0,
                    HomeParts.stack(Theme.label(first.getTitle(), 1.9f, Font.BOLD, Theme.TEXT), more)).fitHeight();
            banner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            banner.setToolTipText(first.getTitle() + " 코디 보기");
            banner.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openLookbook(first);
                }
            });
            lookbookBanner.add(banner, BorderLayout.CENTER);
        }
        for (int i = 1; i <= LOOKBOOK_PREVIEW; i++) {
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
        lookbookRow.setVisible(lookbooks.size() > 1);
    }

    /** 밑줄 글자 링크. 누르는 영역은 48px 이상 (NFR-02). */
    private static JButton textLink(String text) {
        JButton link = Theme.linkButton(text);
        // 마우스로 눌렀을 때는 포커스를 가져가지 않아 테두리가 생기지 않는다. Tab 키로 오면 테두리가 보인다 (키보드 사용자).
        link.setRequestFocusEnabled(false);
        return link;
    }

    // ---------- 다른 화면으로 ----------

    private void openProduct(Product product) {
        ShopUi.openProduct(frame, context, product);
    }

    /** UC-04 1단계: 카테고리 칸 → 상품 목록 화면에서 그 카테고리를 보여준다. null 이면 전체 상품. */
    private void openCategory(Category category) {
        ProductListPanel list = ShopUi.findScreen(frame, ProductListPanel.class);
        if (list != null) {
            list.openCategory(category);
        }
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
