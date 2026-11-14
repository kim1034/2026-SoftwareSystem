package com.sadamda.view.product;

import com.sadamda.controller.AppContext;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.member.SignUpPanel;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
 * 큰 카테고리 버튼, 검색창, 검색 결과의 필터·정렬, 큰 이미지 카드 목록, [더 보기].
 * 필터·정렬은 검색 결과에서만 보인다 (SearchFilter 에 카테고리 조건이 없어서).
 */
public class ProductListPanel extends ScreenPanel {
    public static final String NAME = "productList";

    // SearchFilter.sort 값. (TODO 팀과 확정)
    static final String SORT_POPULAR = "POPULAR";
    static final String SORT_NEWEST = "NEWEST";
    static final String SORT_PRICE_LOW = "PRICE_LOW";
    static final String SORT_PRICE_HIGH = "PRICE_HIGH";

    // 필터 선택지. 첫 항목("… 전체")은 조건 없음. 연령대·체형·사이즈는 회원가입과 같은 값이라 회원 체형 정보와 바로 맞는다.
    private static final String[] PRICES = {"가격대 전체", "10만원 이하", "10~30만원", "30~50만원", "50만원 이상"};
    private static final Integer[][] PRICE_RANGES = {{null, null}, {null, 100000}, {100001, 300000}, {300001, 500000}, {500001, null}};
    private static final String[] COLORS = {"색상 전체", "블랙", "화이트·아이보리", "네이비", "그레이", "베이지·브라운"};

    /** UC-05 2a: 검색 결과가 없을 때 제안하는 자주 찾는 검색어. */
    private static final List<String> SUGGESTED_KEYWORDS = List.of("가디건", "니트", "블라우스", "슬랙스", "스커트");

    private final Map<Category, JToggleButton> chips = new EnumMap<>(Category.class);
    private final ButtonGroup chipGroup = new ButtonGroup();
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

    // UC-05 3~4단계: 검색 결과의 필터·정렬. 바꾸면 바로 목록이 바뀐다.
    private final JPanel filterArea = new JPanel(new GridBagLayout());
    private final JComboBox<String> ageFilter = Theme.comboBox(withAll("연령대 전체", SignUpPanel.AGE_GROUPS));
    private final JComboBox<String> bodyTypeFilter = Theme.comboBox(withAll("체형 전체", SignUpPanel.BODY_TYPES));
    private final JComboBox<String> sizeFilter = Theme.comboBox(withAll("사이즈 전체", SignUpPanel.SIZES));
    private final JComboBox<String> priceFilter = Theme.comboBox(PRICES);
    private final JComboBox<String> colorFilter = Theme.comboBox(COLORS);
    private final JButton myBodyButton = Theme.secondaryButton("내 체형에 맞는 상품");
    private final ButtonGroup sortGroup = new ButtonGroup();
    private final JToggleButton popularSort = Theme.chip("인기순");
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
    private int page;

    public ProductListPanel(AppContext context, MainFrame frame) {
        super(context, frame, "상품", "UC-04 카테고리별 상품 조회, UC-05 상품 검색 및 필터");
        hideTitle(); // 제목은 위쪽 머리말에 직접 그린다.
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

    /** 위쪽 머리말: [제목 ... 검색창 검색] / 큰 카테고리 버튼. 목록을 내려도 그대로 보인다. */
    private JPanel createHeader() {
        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        chipRow.setOpaque(false);
        for (Category each : Category.values()) {
            JToggleButton chip = Theme.chip(each.label());
            chip.addActionListener(e -> showCategory(each));
            chipGroup.add(chip);
            chipRow.add(chip);
            chips.put(each, chip);
        }
        chips.get(Category.TOP).setSelected(true); // 처음에는 상의

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
        titleRow.add(Theme.label("상품", 1.6f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        titleRow.add(searchBox, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(24, 48, 24, 48)));
        header.add(titleRow, BorderLayout.NORTH);
        header.add(chipRow, BorderLayout.CENTER);
        return header;
    }

    /** 머리말 아래: 안내 영역 + 카드 격자 + [더 보기]. 세로로만 스크롤한다. */
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
        Theme.addToCard(infoArea, createFilters(), 16);
        Theme.addToCard(infoArea, suggestions, 16);
        Theme.addToCard(infoArea, popularTitle, 40);

        moreButton.addActionListener(e -> showMore());
        moreRow.setOpaque(false);
        moreRow.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        moreRow.add(moreButton);

        Body body = new Body();
        body.add(infoArea, BorderLayout.NORTH);
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

    /** 필터 두 줄 + 정렬 버튼 한 줄. 선택 상자의 첫 항목이 이름 역할을 한다. */
    private JPanel createFilters() {
        String[] names = {"연령대", "체형", "사이즈", "가격대", "색상"};
        List<JComboBox<String>> boxes = List.of(ageFilter, bodyTypeFilter, sizeFilter, priceFilter, colorFilter);
        for (int i = 0; i < boxes.size(); i++) {
            boxes.get(i).getAccessibleContext().setAccessibleName(names[i] + " 필터");
            boxes.get(i).addActionListener(e -> filtersChanged());
        }
        myBodyButton.addActionListener(e -> applyMyBody());
        JButton resetLink = Theme.linkButton("필터 초기화");
        resetLink.addActionListener(e -> {
            resetFilters();
            runSearch();
        });

        JPanel firstRow = row(ageFilter, bodyTypeFilter, sizeFilter);
        JPanel secondRow = row(priceFilter, colorFilter, myBodyButton, resetLink);
        JPanel sortRow = row();
        String[][] sorts = {{"인기순", SORT_POPULAR}, {"신상품순", SORT_NEWEST},
                {"낮은 가격순", SORT_PRICE_LOW}, {"높은 가격순", SORT_PRICE_HIGH}};
        for (String[] sort : sorts) {
            JToggleButton chip = sort[1].equals(SORT_POPULAR) ? popularSort : Theme.chip(sort[0]);
            chip.setActionCommand(sort[1]);
            chip.addActionListener(e -> filtersChanged());
            sortGroup.add(chip);
            sortRow.add(chip);
        }
        popularSort.setSelected(true);

        filterArea.setOpaque(false);
        Theme.addToCard(filterArea, firstRow, 0);
        Theme.addToCard(filterArea, secondRow, 12);
        Theme.addToCard(filterArea, sortRow, 16);
        return filterArea;
    }

    /** UC-04 1~2단계: 카테고리의 첫 페이지를 보여준다. 없으면 인기 상품을 대신 보여준다 (2a). */
    private void showCategory(Category selected) {
        category = selected;
        keyword = null;
        page = 0;
        chips.get(selected).setSelected(true);
        clearSearchBox();
        List<ProductItem> items = fetch(category, page);
        if (items.isEmpty()) {
            showInfo("준비 중인 상품입니다", false, "대신 지금 인기 있는 상품을 보여 드릴게요.", false, false, false);
            showCards(fetchPopular(), false);
        } else {
            hideInfo();
            showCards(items, items.size() == MockProducts.PAGE_SIZE);
        }
    }

    /** UC-04 3단계 [더 보기]: 다음 페이지를 아래에 덧붙인다. 더 없으면 버튼을 감춘다. */
    private void showMore() {
        page++;
        List<ProductItem> items = fetch(category, page);
        int firstNew = grid.getComponentCount();
        addCards(items);
        moreRow.setVisible(items.size() == MockProducts.PAGE_SIZE);
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
        chipGroup.clearSelection(); // 검색 결과는 특정 카테고리가 아니다
        keyword = trimmed;
        resetFilters();
        runSearch();
    }

    /**
     * UC-05 2~4단계: 검색어와 필터·정렬로 목록을 다시 불러온다.
     * 검색어만으로 결과가 없으면 자주 찾는 검색어와 인기 상품을 제안하고 (2a),
     * 필터 때문에 결과가 없으면 필터를 줄이도록 안내한다.
     */
    private void runSearch() {
        myBodyButton.setVisible(myBodyProfile() != null); // 3a: 체형 정보가 있는 회원에게만
        List<ProductItem> items = fetchSearch(currentFilter());
        if (!items.isEmpty()) {
            showInfo("'" + keyword + "' 검색 결과 " + items.size() + "개", true, null, true, false, false);
            showCards(items, false); // search() 는 결과를 한 번에 준다
        } else if (filtersActive()) {
            showInfo("'" + keyword + "' 중 조건에 맞는 상품이 없어요", true,
                    "필터를 줄이거나 [필터 초기화]를 눌러 보세요", true, false, false);
            showCards(List.of(), false);
        } else {
            showInfo("'" + keyword + "'에 맞는 상품이 없어요", true, "이런 검색어로 찾아보세요", false, true, true);
            showCards(fetchPopular(), false);
        }
    }

    private void filtersChanged() {
        if (!settingFilters && keyword != null) {
            runSearch();
        }
    }

    /** 화면에서 고른 조건을 명세의 SearchFilter 로 만든다. "… 전체"는 null(조건 없음). */
    private SearchFilter currentFilter() {
        Integer[] price = PRICE_RANGES[priceFilter.getSelectedIndex()];
        return new SearchFilter(keyword, chosen(ageFilter), chosen(bodyTypeFilter), chosen(sizeFilter),
                price[0], price[1], chosen(colorFilter), sortGroup.getSelection().getActionCommand());
    }

    private boolean filtersActive() {
        return List.of(ageFilter, bodyTypeFilter, sizeFilter, priceFilter, colorFilter).stream()
                .anyMatch(box -> box.getSelectedIndex() > 0);
    }

    private void resetFilters() {
        settingFilters = true;
        for (JComboBox<String> box : List.of(ageFilter, bodyTypeFilter, sizeFilter, priceFilter, colorFilter)) {
            box.setSelectedIndex(0);
        }
        popularSort.setSelected(true);
        settingFilters = false;
    }

    /** UC-05 3a [내 체형에 맞는 상품]: 회원 체형 정보로 연령대·체형·사이즈를 한 번에 채운다. */
    private void applyMyBody() {
        BodyProfile body = myBodyProfile();
        if (body == null) {
            return;
        }
        settingFilters = true;
        select(ageFilter, body.ageGroup());
        select(bodyTypeFilter, body.bodyType());
        select(sizeFilter, body.usualSize());
        settingFilters = false;
        runSearch();
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

    // TODO Product getter 가 생기면 context.product.listByCategory(category, page) 결과를 ProductItem 으로 바꿔 쓴다.
    private List<ProductItem> fetch(Category category, int page) {
        return MockProducts.listByCategory(category, page);
    }

    // TODO 위와 같이 context.product.popular() 로 바꾼다.
    private List<ProductItem> fetchPopular() {
        return MockProducts.popular();
    }

    // TODO 위와 같이 context.product.search(filter) 로 바꾼다.
    private List<ProductItem> fetchSearch(SearchFilter filter) {
        return MockProducts.search(filter);
    }

    private void showInfo(String title, boolean clearLink, String hintText, boolean showFilters,
                          boolean showSuggestions, boolean showPopularTitle) {
        headline.setText(title);
        clearSearchLink.setVisible(clearLink);
        hint.setText(hintText == null ? "" : hintText);
        hint.setVisible(hintText != null);
        filterArea.setVisible(showFilters);
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
            // TODO UC-06 상세 화면이 생기면 item.id() 를 넘긴다. 지금은 화면 이동만 한다.
            grid.add(new ProductCard(item, () -> frame.navigate(ProductDetailPanel.NAME)));
        }
    }

    private void refresh() {
        infoArea.revalidate();
        grid.revalidate();
        grid.repaint();
    }

    /** "… 전체" 를 맨 앞에 두고, 회원가입 선택지에서 "선택 안 함" 을 뺀 목록. */
    private static String[] withAll(String all, String[] options) {
        String[] items = Arrays.copyOf(options, options.length);
        items[0] = all; // options[0] 은 "선택 안 함"
        return items;
    }

    private static String chosen(JComboBox<String> box) {
        return box.getSelectedIndex() == 0 ? null : (String) box.getSelectedItem();
    }

    /** 선택지에 있는 값이면 고르고, 없으면 "… 전체" 로 둔다. */
    private static void select(JComboBox<String> box, String value) {
        box.setSelectedIndex(0);
        if (value != null) {
            box.setSelectedItem(value);
        }
    }

    private static JPanel row(JComponent... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        for (JComponent component : components) {
            row.add(component);
        }
        return row;
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
