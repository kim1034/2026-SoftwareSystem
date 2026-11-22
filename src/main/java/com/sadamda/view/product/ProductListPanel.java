package com.sadamda.view.product;

import com.sadamda.controller.AppContext;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.WrapLayout;
import com.sadamda.view.member.SignUpPanel;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * UC-04 카테고리별 상품 조회 + UC-05 상품 검색 및 필터:
 * 카테고리 탭, 검색창, 필터·정렬, 큰 이미지 카드 목록, [더 보기].
 * 필터·정렬은 카테고리 목록과 검색 결과 모두에 보인다. SearchFilter 에 카테고리 조건이 없어서,
 * 카테고리 목록은 조건으로 검색한 결과 중 그 카테고리 상품만 화면에서 골라 보여준다.
 */
public class ProductListPanel extends ScreenPanel {
    public static final String NAME = "productList";

    // SearchFilter.sort 값. (TODO 팀과 확정)
    static final String SORT_POPULAR = "POPULAR";
    static final String SORT_NEWEST = "NEWEST";
    static final String SORT_PRICE_LOW = "PRICE_LOW";
    static final String SORT_PRICE_HIGH = "PRICE_HIGH";
    private static final String[][] SORTS = {{"인기순", SORT_POPULAR}, {"신상품순", SORT_NEWEST},
            {"낮은 가격순", SORT_PRICE_LOW}, {"높은 가격순", SORT_PRICE_HIGH}};

    // 필터 선택지. 첫 항목은 "전체"(조건 없음). 연령대·체형·사이즈는 회원가입과 같은 값이라 회원 체형 정보와 바로 맞는다.
    private static final String[] PRICES = {"전체", "10만원 이하", "10~30만원", "30~50만원", "50만원 이상"};
    private static final Integer[][] PRICE_RANGES = {{null, null}, {null, 100000}, {100001, 300000}, {300001, 500000}, {500001, null}};
    private static final String[] COLORS = {"전체", "블랙", "화이트·아이보리", "네이비", "그레이", "베이지·브라운"};

    /** UC-05 2a: 검색 결과가 없을 때 제안하는 자주 찾는 검색어. */
    private static final List<String> SUGGESTED_KEYWORDS = List.of("가디건", "니트", "블라우스", "슬랙스", "스커트");

    private final Map<Category, JToggleButton> tabs = new EnumMap<>(Category.class);
    private final ButtonGroup tabGroup = new ButtonGroup();
    private final JTextField searchField = Theme.textField("상품명이나 종류로 검색 (예: 가디건)");
    private final JLabel searchError = Theme.errorLabel();

    // 카드 위 안내 영역. 상황에 맞는 줄만 보인다.
    private final JPanel infoArea = new JPanel(new GridBagLayout());
    private final JLabel headline = Theme.label(" ", 1.1f, Font.BOLD, Theme.TEXT);
    private final JButton clearSearchLink = Theme.linkButton("검색 지우기");
    private final JPanel headlineRow = new JPanel(new BorderLayout(16, 0));
    private final JLabel hint = Theme.label(" ", 1f, Font.PLAIN, Theme.HINT);
    private final JPanel suggestions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    private final JLabel popularTitle = Theme.label("지금 인기 있는 상품", 1.1f, Font.BOLD, Theme.TEXT);

    // UC-05 3~4단계: 필터·정렬. 바꾸면 바로 목록이 바뀐다.
    private final FilterPill ageFilter = new FilterPill("연령대", withAll(SignUpPanel.AGE_GROUPS));
    private final FilterPill bodyTypeFilter = new FilterPill("체형", withAll(SignUpPanel.BODY_TYPES));
    private final FilterPill sizeFilter = new FilterPill("사이즈", withAll(SignUpPanel.SIZES));
    private final FilterPill priceFilter = new FilterPill("가격대", PRICES);
    private final FilterPill colorFilter = new FilterPill("색상", COLORS);
    private final List<FilterPill> pills = List.of(ageFilter, bodyTypeFilter, sizeFilter, priceFilter, colorFilter);
    private final JCheckBox myBodyBox = Theme.checkBox("내 체형에 맞는 상품");
    private final Component myBodyGap = javax.swing.Box.createHorizontalStrut(16);
    private final JButton sortButton = sortButton();
    private String sort = SORT_POPULAR;
    /** 코드에서 필터 값을 바꾸는 동안에는 목록을 다시 불러오지 않는다. */
    private boolean settingFilters;
    /** 지금 보고 있는 검색어. 검색 중이 아니면 null. */
    private String keyword;

    private final CardGrid grid = new CardGrid();
    private final JButton moreButton = Theme.secondaryButton("더 보기");
    private final JPanel moreRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    private final JScrollPane scroll = new JScrollPane();

    /** 마지막으로 본 카테고리. 검색 중에도 기억해 두었다가 [검색 지우기]로 돌아간다. */
    private Category category;
    /** 카테고리 목록의 전체 결과. [더 보기]마다 PAGE_SIZE 개씩 더 보여준다. */
    private List<ProductItem> results = List.of();

    public ProductListPanel(AppContext context, MainFrame frame) {
        super(context, frame, "상품", "UC-04 카테고리별 상품 조회, UC-05 상품 검색 및 필터");
        hideTitle(); // 카테고리 탭이 제목 역할을 한다.
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createEmptyBorder()); // 머리말 줄이 화면 끝까지 닿게

        setContent(createScreen(createHeader()));

        // 처음 열 때 한 번만 불러온다. 다른 화면에 다녀와도 보던 목록과 위치를 유지한다.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (category == null) {
                    showCategory(Category.TOP);
                }
            }
        });
    }

    /** 위쪽 머리말: [검색창 검색] / 카테고리 탭. 목록을 내려도 그대로 보인다. */
    private JPanel createHeader() {
        // 간격을 탭 사이에만 둔다 (FlowLayout 의 간격은 왼쪽 끝에도 생겨서 제목과 줄이 어긋난다).
        JPanel tabRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 0, 0));
        tabRow.setOpaque(false);
        for (Category each : Category.values()) {
            JToggleButton tab = new CategoryTab(each.label());
            tab.addActionListener(e -> showCategory(each));
            tabGroup.add(tab);
            if (tabRow.getComponentCount() > 0) {
                tabRow.add(javax.swing.Box.createHorizontalStrut(28));
            }
            tabRow.add(tab);
            tabs.put(each, tab);
        }
        tabs.get(Category.TOP).setSelected(true); // 처음에는 상의

        // UC-05 1단계: 검색어 입력 후 [검색] 또는 Enter
        searchField.setColumns(18);
        searchField.getAccessibleContext().setAccessibleName("상품 검색");
        searchField.addActionListener(e -> search(searchField.getText()));
        JButton searchButton = Theme.primaryButton("검색");
        searchButton.addActionListener(e -> search(searchField.getText()));
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        searchBox.setOpaque(false);
        searchBox.add(searchError);
        searchBox.add(searchField);
        searchBox.add(searchButton);

        JPanel titleRow = new JPanel(new BorderLayout(24, 0));
        titleRow.setOpaque(false);
        titleRow.add(searchBox, BorderLayout.EAST);

        // 탭의 밑줄이 머리말 아래 구분선에 닿도록 아래 여백은 두지 않는다.
        JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(24, 48, 0, 48)));
        header.add(titleRow, BorderLayout.NORTH);
        header.add(tabRow, BorderLayout.CENTER);
        return header;
    }

    /** 머리말 아래: 필터 + 안내 영역 + 카드 격자 + [더 보기]. 세로로만 스크롤한다. */
    private JPanel createScreen(JPanel header) {
        clearSearchLink.addActionListener(e -> clearSearch());
        headlineRow.setOpaque(false);
        headlineRow.add(headline, BorderLayout.CENTER);
        headlineRow.add(clearSearchLink, BorderLayout.EAST);
        suggestions.setOpaque(false);
        for (String keyword : SUGGESTED_KEYWORDS) {
            JButton suggestion = Theme.secondaryButton(keyword);
            suggestion.addActionListener(e -> {
                searchField.setText(keyword);
                search(keyword);
            });
            suggestions.add(suggestion);
        }
        infoArea.setOpaque(false);
        infoArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        Theme.addToCard(infoArea, headlineRow, 0);
        Theme.addToCard(infoArea, hint, 4);
        Theme.addToCard(infoArea, suggestions, 16);
        Theme.addToCard(infoArea, popularTitle, 40);

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        Theme.addToCard(top, createFilters(), 0);
        Theme.addToCard(top, infoArea, 0);

        moreButton.addActionListener(e -> showMore());
        moreRow.setOpaque(false);
        moreRow.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        moreRow.add(moreButton);

        Body body = new Body();
        body.add(top, BorderLayout.NORTH);
        body.add(grid, BorderLayout.CENTER);
        body.add(moreRow, BorderLayout.SOUTH);
        scroll.setViewportView(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // 결과가 적어 스크롤이 필요 없을 때도 스크롤바 자리를 남겨, 오른쪽 링크와 카드 폭이 흔들리지 않게 한다.
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel screen = new JPanel(new BorderLayout());
        screen.setOpaque(false);
        screen.add(header, BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        return screen;
    }

    /**
     * 필터 두 줄: [연령대 ∨] [체형 ∨] [사이즈 ∨] [가격대 ∨] [색상 ∨]
     *            ☐ 내 체형에 맞는 상품   필터 초기화                         인기순 ↑↓
     */
    private JPanel createFilters() {
        JPanel pillRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 0, 8));
        pillRow.setOpaque(false);
        for (FilterPill pill : pills) {
            pill.onChange(this::filtersChanged);
            if (pillRow.getComponentCount() > 0) {
                pillRow.add(javax.swing.Box.createHorizontalStrut(10));
            }
            pillRow.add(pill);
        }
        myBodyBox.addActionListener(e -> {
            if (myBodyBox.isSelected()) {
                applyMyBody();
            } else {
                setFilters(() -> List.of(ageFilter, bodyTypeFilter, sizeFilter).forEach(p -> p.setSelectedIndex(0)));
                runList();
            }
        });
        JButton resetLink = Theme.linkButton("필터 초기화");
        resetLink.addActionListener(e -> {
            resetFilters();
            runList();
        });
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        options.setOpaque(false);
        options.add(myBodyBox);
        options.add(myBodyGap);
        options.add(resetLink);

        JPanel optionRow = new JPanel(new BorderLayout(16, 0));
        optionRow.setOpaque(false);
        optionRow.add(options, BorderLayout.WEST);
        updateSortButton();
        optionRow.add(sortButton, BorderLayout.EAST);

        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        filters.setBorder(BorderFactory.createEmptyBorder(0, 0, 32, 0));
        Theme.addToCard(filters, pillRow, 0);
        Theme.addToCard(filters, optionRow, 8);
        return filters;
    }

    /** 오른쪽의 [인기순 ↑↓]. 누르면 정렬 방법을 고르는 목록이 열린다. */
    private JButton sortButton() {
        JButton button = new JButton() {
            @Override
            public void updateUI() {
                setUI(new BasicButtonUI());
            }
        };
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 0));
        button.setForeground(Theme.TEXT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            for (String[] each : SORTS) {
                JMenuItem item = new JMenuItem(each[0]);
                item.setFont(button.getFont().deriveFont(each[1].equals(sort) ? Font.BOLD : Font.PLAIN));
                item.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 24));
                item.addActionListener(ev -> {
                    sort = each[1];
                    updateSortButton();
                    filtersChanged();
                });
                menu.add(item);
            }
            menu.show(button, 0, button.getHeight());
        });
        return button;
    }

    private void updateSortButton() {
        for (String[] each : SORTS) {
            if (each[1].equals(sort)) {
                sortButton.setText(each[0] + "  ↑↓");
                sortButton.getAccessibleContext().setAccessibleName("정렬, 지금 " + each[0]);
            }
        }
    }

    /** 홈 화면의 카테고리 버튼에서 부른다. null([전체 상품 보기])이면 이 화면에 [전체]가 없어 상의를 보여준다. */
    public void openCategory(Category selected) {
        showCategory(selected == null ? Category.TOP : selected);
    }

    /** 홈 화면의 검색창에서 부른다. 처음 여는 경우 componentShown 이 검색 결과를 덮지 않게 목록을 먼저 채운다. */
    public void openSearch(String text) {
        if (category == null) {
            showCategory(Category.TOP);
        }
        searchField.setText(text);
        search(text);
    }

    /** UC-04 1~2단계: 카테고리 탭을 고른다. 고른 필터·정렬은 그대로 둔다. */
    private void showCategory(Category selected) {
        category = selected;
        keyword = null;
        tabs.get(selected).setSelected(true);
        clearSearchBox();
        runList();
    }

    /** UC-04 3단계 [더 보기]: 다음 PAGE_SIZE 개를 아래에 덧붙인다. 더 없으면 버튼을 감춘다. */
    private void showMore() {
        int firstNew = grid.getComponentCount();
        addCards(results.subList(firstNew, Math.min(firstNew + ProductService.PAGE_SIZE, results.size())));
        moreRow.setVisible(grid.getComponentCount() < results.size());
        refresh();
        if (firstNew < grid.getComponentCount()) {
            grid.getComponent(firstNew).requestFocusInWindow(); // 키보드 사용자가 새 상품부터 이어 볼 수 있게
        }
    }

    /** UC-05 1단계: 새 검색어로 검색한다. 필터·정렬은 처음 상태로 돌린다. */
    private void search(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            searchError.setText("검색어를 입력해 주세요");
            Theme.setInvalid(searchField, true);
            searchField.requestFocusInWindow();
            return;
        }
        searchError.setText("");
        Theme.setInvalid(searchField, false);
        tabGroup.clearSelection(); // 검색 결과는 특정 카테고리가 아니다
        keyword = trimmed;
        resetFilters();
        runList();
    }

    /**
     * UC-04 2단계, UC-05 2~4단계: 카테고리 또는 검색어와 필터·정렬로 목록을 다시 불러온다.
     * 카테고리에 상품이 없으면 인기 상품을 (UC-04 2a), 검색어로 찾은 게 없으면 자주 찾는 검색어와 인기 상품을
     * 제안하고 (UC-05 2a), 필터 때문에 결과가 없으면 필터를 줄이도록 안내한다.
     */
    private void runList() {
        myBodyBox.setVisible(myBodyProfile() != null); // 3a: 체형 정보가 있는 회원에게만
        myBodyGap.setVisible(myBodyBox.isVisible());
        List<Product> found = context.product.search(currentFilter());
        if (keyword == null) {
            found = found.stream().filter(product -> product.getCategory() == category).toList();
        }
        results = items(found);
        if (!results.isEmpty()) {
            if (keyword == null) {
                hideInfo();
                showCards(results.subList(0, Math.min(ProductService.PAGE_SIZE, results.size())),
                        results.size() > ProductService.PAGE_SIZE);
            } else {
                showInfo("'" + keyword + "' 검색 결과 " + results.size() + "개", true, null, false, false);
                showCards(results, false); // 검색 결과는 한 번에 보여준다
            }
        } else if (filtersActive()) {
            showInfo(keyword == null ? "조건에 맞는 상품이 없어요" : "'" + keyword + "' 중 조건에 맞는 상품이 없어요",
                    keyword != null, "필터를 줄이거나 [필터 초기화]를 눌러 보세요", false, false);
            showCards(List.of(), false);
        } else if (keyword == null) {
            showInfo("준비 중인 상품입니다", false, "대신 지금 인기 있는 상품을 보여 드릴게요.", false, false);
            showCards(fetchPopular(), false);
        } else {
            showInfo("'" + keyword + "'에 맞는 상품이 없어요", true, "이런 검색어로 찾아보세요", true, true);
            showCards(fetchPopular(), false);
        }
    }

    private void filtersChanged() {
        if (!settingFilters) {
            myBodyBox.setSelected(matchesMyBody());
            runList();
        }
    }

    /** 화면에서 고른 조건을 명세의 SearchFilter 로 만든다. "전체"는 null(조건 없음). */
    private SearchFilter currentFilter() {
        Integer[] price = PRICE_RANGES[priceFilter.selectedIndex()];
        return new SearchFilter(keyword, ageFilter.value(), bodyTypeFilter.value(), sizeFilter.value(),
                price[0], price[1], colorFilter.value(), sort);
    }

    private boolean filtersActive() {
        return pills.stream().anyMatch(pill -> pill.selectedIndex() > 0);
    }

    private void resetFilters() {
        setFilters(() -> {
            pills.forEach(pill -> pill.setSelectedIndex(0));
            sort = SORT_POPULAR;
            updateSortButton();
            myBodyBox.setSelected(false);
        });
    }

    /** 코드에서 필터를 바꾸는 동안 목록을 다시 불러오지 않게 한다. */
    private void setFilters(Runnable change) {
        settingFilters = true;
        change.run();
        settingFilters = false;
    }

    /** UC-05 3a [내 체형에 맞는 상품]: 회원 체형 정보로 연령대·체형·사이즈를 한 번에 채운다. */
    private void applyMyBody() {
        BodyProfile body = myBodyProfile();
        if (body == null) {
            return;
        }
        setFilters(() -> {
            ageFilter.select(body.ageGroup());
            bodyTypeFilter.select(body.bodyType());
            sizeFilter.select(body.usualSize());
        });
        runList();
    }

    /** 연령대·체형·사이즈 필터가 내 체형 정보와 같은지. 직접 바꾸면 [내 체형에 맞는 상품] 체크를 푼다. */
    private boolean matchesMyBody() {
        BodyProfile body = myBodyProfile();
        return body != null && java.util.Objects.equals(ageFilter.value(), body.ageGroup())
                && java.util.Objects.equals(bodyTypeFilter.value(), body.bodyType())
                && java.util.Objects.equals(sizeFilter.value(), body.usualSize());
    }

    /** 로그인했고 체형 정보(연령대·체형·사이즈 중 하나 이상)가 있으면 그 정보, 아니면 null. */
    private BodyProfile myBodyProfile() {
        Member member = context.member.currentMember();
        BodyProfile body = member == null ? null : member.getBodyProfile();
        if (body == null || (body.ageGroup() == null && body.bodyType() == null && body.usualSize() == null)) {
            return null;
        }
        return body;
    }

    /** [검색 지우기]: 검색 전에 보던 카테고리 목록으로 돌아간다. */
    private void clearSearch() {
        showCategory(category);
        searchField.requestFocusInWindow();
    }

    private void clearSearchBox() {
        searchField.setText("");
        searchError.setText("");
        Theme.setInvalid(searchField, false);
    }

    private List<ProductItem> fetchPopular() {
        return items(context.product.popular());
    }

    private static List<ProductItem> items(List<Product> products) {
        return products.stream().map(ProductItem::of).toList();
    }

    private void showInfo(String title, boolean clearLink, String hintText, boolean showSuggestions,
                          boolean showPopularTitle) {
        headline.setText(title);
        clearSearchLink.setVisible(clearLink);
        hint.setText(hintText == null ? "" : hintText);
        hint.setVisible(hintText != null);
        suggestions.setVisible(showSuggestions);
        popularTitle.setVisible(showPopularTitle);
        infoArea.setVisible(true);
    }

    private void hideInfo() {
        infoArea.setVisible(false);
    }

    /** 카드를 새로 채우고 맨 위로 올라간다. */
    private void showCards(List<ProductItem> items, boolean hasMore) {
        grid.removeAll();
        addCards(items);
        moreRow.setVisible(hasMore);
        refresh();
        scroll.getViewport().setViewPosition(new Point(0, 0));
    }

    private void addCards(List<ProductItem> items) {
        for (ProductItem item : items) {
            grid.add(new ProductCard(item, cardActions, () -> frame.openProduct(item.id()))); // UC-06 상세로
        }
    }

    /** 카드의 찜 하트와 "♥ 찜 수 ★ 평점 (리뷰 수)" 줄 (UC-10 1a, FR-23). */
    private final ProductCard.Actions cardActions = new WishActions(context, frame, this);

    private void refresh() {
        infoArea.revalidate();
        grid.revalidate();
        grid.repaint();
    }

    /** 회원가입 선택지(0번 "선택 안 함")를 필터 선택지로. 0번은 FilterPill 이 "전체"로 보여준다. */
    private static String[] withAll(String[] options) {
        return Arrays.copyOf(options, options.length);
    }

    /**
     * 카테고리 탭: 테두리 없이 글자만, 고른 탭은 굵은 글씨와 검은 밑줄.
     * [글씨 크기]를 바꿔도 탭 글씨는 "보통" 크기로 고정한다 (탭이 한 줄에 들어가게).
     */
    private static final class CategoryTab extends JToggleButton {
        /** "보통" 글씨(본문 18px)일 때의 카테고리 글씨 크기. */
        private static final float FIXED_SIZE = 20f;
        private static final int UNDERLINE = 3;

        CategoryTab(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(12, 2, 14, 2)); // 높이 48px 이상 (NFR-02)
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addItemListener(e -> updateStyle());
            updateStyle();
        }

        @Override
        public void updateUI() {
            setUI(new BasicToggleButtonUI());
            updateStyle();
        }

        private void updateStyle() {
            Font base = UIManager.getFont("Button.font");
            setFont(base.deriveFont(isSelected() ? Font.BOLD : Font.PLAIN, FIXED_SIZE));
            setForeground(isSelected() ? Theme.TEXT : Theme.HINT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (isSelected()) {
                g2.setColor(Theme.PRIMARY);
                g2.fillRect(0, getHeight() - UNDERLINE, getWidth(), UNDERLINE);
            }
            if (isFocusOwner()) { // 키보드 사용자를 위한 테두리
                g2.setColor(Theme.TEXT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - UNDERLINE - 3);
            }
            g2.dispose();
        }
    }

    /** 목록 전체(안내 + 카드 + [더 보기]). 폭은 창에 맞추고 세로로만 스크롤한다. */
    private static final class Body extends JPanel implements Scrollable {
        Body() {
            super(new BorderLayout());
            setBackground(Theme.CARD);
            setBorder(BorderFactory.createEmptyBorder(32, 48, 48, 48));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height - 48;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * 카드를 창 폭에 맞춰 여러 줄로 놓는다. 카드 폭은 최소 240px (NFR-02 목록 이미지 너비).
     * 글씨를 키우면 최소 폭도 함께 커져서 한 줄의 카드 수가 줄어든다.
     */
    private static final class CardGrid extends JPanel {
        private static final int GAP = 24;
        private static final int ROW_GAP = 40;

        CardGrid() {
            super(null);
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            int width = getWidth();
            int columns = columns(width);
            int cardWidth = cardWidth(width, columns);
            int cardHeight = cardHeight(cardWidth);
            for (int i = 0; i < getComponentCount(); i++) {
                int x = (i % columns) * (cardWidth + GAP);
                int y = (i / columns) * (cardHeight + ROW_GAP);
                getComponent(i).setBounds(x, y, cardWidth, cardHeight);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int width = availableWidth();
            int columns = columns(width);
            int rows = (getComponentCount() + columns - 1) / columns;
            int height = rows == 0 ? 0 : rows * cardHeight(cardWidth(width, columns)) + (rows - 1) * ROW_GAP;
            return new Dimension(width, height);
        }

        /** 레이아웃 전에는 내 폭을 모르므로, 스크롤 창 폭에서 바깥 여백을 뺀 값을 쓴다. */
        private int availableWidth() {
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
            if (viewport == null || viewport.getWidth() == 0) {
                return getWidth();
            }
            Insets insets = ((JComponent) getParent()).getInsets();
            return viewport.getWidth() - insets.left - insets.right;
        }

        private int columns(int width) {
            int minimum = Math.max(240, Math.round(UIManager.getFont("Label.font").getSize2D() * 13.4f));
            return Math.max(1, (width + GAP) / (minimum + GAP));
        }

        private static int cardWidth(int width, int columns) {
            return (width - GAP * (columns - 1)) / columns;
        }

        private int cardHeight(int cardWidth) {
            if (getComponentCount() == 0) {
                return 0;
            }
            Component first = getComponent(0);
            return ((ProductCard) first).heightFor(cardWidth);
        }
    }
}
