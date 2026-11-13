package com.sadamda.view.product;

import com.sadamda.common.LoginRequiredException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.product.ProductTags;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.member.MyInfoPanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 목록 화면: 큰 카테고리 버튼, 검색창, 필터, 큰 이미지 카드 목록.
 * UC-04 카테고리별 상품 조회 (FR-05), UC-05 상품 검색 및 필터 (FR-06, FR-07).
 *
 * 다른 화면에서 이 화면을 특정 카테고리·검색어로 열 때는 showCategory / showSearch 를 부른 뒤 navigate(NAME) 한다.
 */
public class ProductListPanel extends ScreenPanel {
    public static final String NAME = "productList";

    private static final String ANY = "전체";
    /** 가격대 필터: 이름, 최소, 최대 (원, null 이면 제한 없음). */
    private static final Object[][] PRICE_RANGES = {
            {ANY, null, null},
            {"5만원 이하", null, 50_000},
            {"5만~10만원", 50_000, 100_000},
            {"10만~20만원", 100_000, 200_000},
            {"20만~50만원", 200_000, 500_000},
            {"50만원 이상", 500_000, null},
    };
    private static final List<String> SIZES = List.of(ANY, "55", "66", "77", "88", "230", "235", "240", "245", "250", "FREE");
    private static final List<String> COLORS = List.of(ANY, "블랙", "화이트", "아이보리", "베이지", "그레이", "네이비",
            "브라운", "카멜", "레드", "핑크", "블루", "그린", "골드", "실버");

    private final JTextField keywordField = new JTextField(24);
    private final CategoryBar categoryBar;
    private final JComboBox<String> ageBox = combo(withAny(ProductTags.AGE_GROUPS), "연령대");
    private final JComboBox<String> bodyBox = combo(withAny(ProductTags.BODY_TYPES), "체형");
    private final JComboBox<String> sizeBox = combo(SIZES, "사이즈");
    private final JComboBox<String> priceBox = combo(priceNames(), "가격대");
    private final JComboBox<String> colorBox = combo(COLORS, "색상");
    private final JComboBox<String> sortBox = combo(ProductService.SORTS, "정렬");

    private final JLabel summary = ShopUi.label("", 1.15f, Font.BOLD, ShopUi.TEXT);
    private final ProductGrid grid;
    private final JButton moreButton = ShopUi.secondaryButton("더 보기");
    private final JPanel emptyPanel = new JPanel(new BorderLayout(0, 16));
    private final JPanel body = new JPanel();
    private JScrollPane scroll;

    private Category category;              // null 이면 전체
    private boolean listingByCategory;      // true: UC-04 쪽 단위 조회, false: 검색 결과를 화면에서 나눠 보여준다
    private int page;
    private List<Product> results = List.of();
    private boolean adjusting;              // 코드로 필터를 바꾸는 동안 다시 조회하지 않는다

    public ProductListPanel(AppContext context, MainFrame frame) {
        super(context, frame, "상품", "UC-04 카테고리별 상품 조회, UC-05 상품 검색 및 필터");
        hideTitle();
        setBackground(ShopUi.PAGE);
        grid = new ProductGrid(product -> ShopUi.openProduct(frame, context, product));
        categoryBar = new CategoryBar(context, true, selected -> {
            category = selected;
            reload();
        });
        setContent(buildContent());
        ShopUi.whenShown(this, () -> {
            categoryBar.refreshCounts();
            reload();
        });
        reload();
    }

    // ---------- 다른 화면에서 쓰는 입구 ----------

    /** 카테고리 상품 목록으로 연다 (UC-04). 검색어·필터는 지운다. */
    public void showCategory(Category category) {
        clearFilters();
        this.category = category;
        reload();
    }

    /** 검색어로 연다 (UC-05). 카테고리·필터는 지운다. */
    public void showSearch(String keyword) {
        clearFilters();
        category = null;
        keywordField.setText(keyword == null ? "" : keyword.trim());
        reload();
    }

    // ---------- 화면 구성 ----------

    private JComponent buildContent() {
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ShopUi.PAGE);
        body.setBorder(BorderFactory.createEmptyBorder(8, 8, 24, 8));

        addBlock(ShopUi.title("상품 둘러보기"), 0);
        addBlock(ShopUi.hint("카테고리를 누르거나 찾는 상품 이름을 입력하세요."), 4);
        addBlock(searchRow(), 16);
        addBlock(categoryBar, 16);
        addBlock(filterPanel(), 16);
        JPanel summaryRow = new JPanel(new BorderLayout());
        summaryRow.setOpaque(false);
        summaryRow.add(summary, BorderLayout.CENTER);
        addBlock(summaryRow, 24);
        addBlock(emptyPanel, 12);
        addBlock(grid, 12);

        moreButton.addActionListener(e -> loadMore());
        JPanel moreRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        moreRow.setOpaque(false);
        moreButton.setPreferredSize(new Dimension(320, 56));
        moreRow.add(moreButton);
        addBlock(moreRow, 16);

        emptyPanel.setOpaque(false);
        scroll = ShopUi.verticalScroll(body);
        return scroll;
    }

    private void addBlock(JComponent component, int topGap) {
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

    private JComponent searchRow() {
        keywordField.setPreferredSize(new Dimension(520, ShopUi.MIN_TOUCH));
        keywordField.getAccessibleContext().setAccessibleName("검색어");
        keywordField.setToolTipText("예: 버킷햇, 로퍼, 캐시미어, 브랜드 이름");
        keywordField.addActionListener(e -> reload()); // Enter 로 검색
        JButton search = ShopUi.primaryButton("검색");
        search.addActionListener(e -> reload());
        JButton clear = ShopUi.secondaryButton("처음으로");
        clear.setToolTipText("검색어와 필터를 모두 지웁니다");
        clear.addActionListener(e -> {
            clearFilters();
            category = null;
            reload();
        });
        return ShopUi.row(10, keywordField, search, clear);
    }

    private JComponent filterPanel() {
        JPanel card = ShopUi.card(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel combos = new JPanel(new ProductGrid.ResponsiveGridLayout(170, 12, 6, true));
        combos.setOpaque(false);
        combos.add(labeled("연령대", ageBox));
        combos.add(labeled("체형", bodyBox));
        combos.add(labeled("사이즈", sizeBox));
        combos.add(labeled("가격대", priceBox));
        combos.add(labeled("색상", colorBox));
        combos.add(labeled("정렬", sortBox));
        for (JComboBox<String> box : List.of(ageBox, bodyBox, sizeBox, priceBox, colorBox, sortBox)) {
            box.addActionListener(e -> {
                if (!adjusting) {
                    reload();
                }
            });
        }

        JButton mine = ShopUi.secondaryButton("내 체형에 맞는 상품");
        mine.setToolTipText("회원 정보의 연령대·체형·평소 사이즈로 골라 줍니다");
        mine.addActionListener(e -> applyMyBody());
        JButton reset = ShopUi.secondaryButton("필터 지우기");
        reset.addActionListener(e -> {
            clearFilters();
            reload();
        });

        card.add(ShopUi.sectionHeader("조건으로 찾기", null, null), BorderLayout.NORTH);
        card.add(combos, BorderLayout.CENTER);
        card.add(ShopUi.row(10, mine, reset), BorderLayout.SOUTH);
        return card;
    }

    private static JComponent labeled(String name, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(ShopUi.label(name, 0.95f, Font.BOLD, ShopUi.HINT), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private static JComboBox<String> combo(List<String> values, String accessibleName) {
        JComboBox<String> box = new JComboBox<>(values.toArray(new String[0]));
        box.setPreferredSize(new Dimension(170, ShopUi.MIN_TOUCH));
        box.getAccessibleContext().setAccessibleName(accessibleName);
        return box;
    }

    private static List<String> withAny(List<String> values) {
        List<String> list = new ArrayList<>();
        list.add(ANY);
        list.addAll(values);
        return list;
    }

    private static List<String> priceNames() {
        List<String> names = new ArrayList<>();
        for (Object[] range : PRICE_RANGES) {
            names.add((String) range[0]);
        }
        return names;
    }

    // ---------- 조회 ----------

    private void clearFilters() {
        adjusting = true;
        keywordField.setText("");
        for (JComboBox<String> box : List.of(ageBox, bodyBox, sizeBox, priceBox, colorBox, sortBox)) {
            box.setSelectedIndex(0);
        }
        adjusting = false;
    }

    /** UC-05 3a [내 체형에 맞는 상품]: 회원 체형 정보로 필터를 채워 검색한다. */
    private void applyMyBody() {
        Member member = context.member.currentMember();
        if (member == null) {
            ShopUi.showError(this, frame, new LoginRequiredException());
            return;
        }
        BodyProfile profile = member.getBodyProfile();
        if (profile == null || (profile.ageGroup() == null && profile.bodyType() == null && profile.usualSize() == null)) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "등록된 체형 정보가 없습니다.\n내 정보 화면에서 연령대·체형을 입력할까요?", "체형 정보 없음",
                    JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                frame.navigate(MyInfoPanel.NAME);
            }
            return;
        }
        adjusting = true;
        select(ageBox, profile.ageGroup());
        select(bodyBox, profile.bodyType());
        select(sizeBox, profile.usualSize());
        adjusting = false;
        reload();
    }

    private static void select(JComboBox<String> box, String value) {
        if (value == null || value.isBlank()) {
            box.setSelectedIndex(0);
            return;
        }
        box.setSelectedItem(value);
        if (!value.equals(box.getSelectedItem())) {
            box.setSelectedIndex(0); // 목록에 없는 값(예: 평소 사이즈 "M")은 조건에서 뺀다
        }
    }

    private String selected(JComboBox<String> box) {
        Object value = box.getSelectedItem();
        return value == null || ANY.equals(value) ? null : value.toString();
    }

    private boolean hasFilters() {
        return !keywordField.getText().isBlank() || box(ageBox) || box(bodyBox) || box(sizeBox)
                || box(priceBox) || box(colorBox) || sortBox.getSelectedIndex() != 0;
    }

    private static boolean box(JComboBox<String> box) {
        return box.getSelectedIndex() > 0;
    }

    /** 지금 고른 카테고리·검색어·필터로 처음부터 다시 보여준다. */
    private void reload() {
        categoryBar.setSelected(category);
        page = 0;
        listingByCategory = category != null && !hasFilters();
        List<Product> first;
        try {
            if (listingByCategory) {
                // UC-04 2~3단계: 카테고리 상품을 인기순으로 한 쪽씩
                first = context.product.listByCategory(category, 0);
                results = List.of();
            } else {
                // UC-05: 검색·필터 결과. 카테고리를 골랐으면 그 카테고리 안에서만
                Object[] range = PRICE_RANGES[priceBox.getSelectedIndex()];
                SearchFilter filter = new SearchFilter(blankToNull(keywordField.getText()), selected(ageBox),
                        selected(bodyBox), selected(sizeBox), (Integer) range[1], (Integer) range[2],
                        selected(colorBox), selected(sortBox));
                results = context.product.search(filter).stream()
                        .filter(product -> category == null || product.getCategory() == category)
                        .collect(Collectors.toList());
                first = results.stream().limit(ProductService.PAGE_SIZE).collect(Collectors.toList());
            }
        } catch (RuntimeException e) {
            ShopUi.showError(this, frame, e);
            return;
        }
        grid.setProducts(first);
        updateSummary();
        updateEmptyState(first.isEmpty());
        updateMoreButton();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    /** UC-04 3단계 [더 보기] */
    private void loadMore() {
        page++;
        List<Product> next;
        if (listingByCategory) {
            next = context.product.listByCategory(category, page);
        } else {
            next = results.stream().skip((long) page * ProductService.PAGE_SIZE).limit(ProductService.PAGE_SIZE)
                    .collect(Collectors.toList());
        }
        grid.addProducts(next);
        updateMoreButton();
    }

    private int totalCount() {
        return listingByCategory ? context.product.countByCategory(category) : results.size();
    }

    private void updateMoreButton() {
        int remaining = totalCount() - grid.count();
        moreButton.setVisible(remaining > 0);
        moreButton.setText("더 보기 (" + Math.max(remaining, 0) + "개 남음)");
    }

    private void updateSummary() {
        String where = category == null ? "전체 상품" : category.label();
        String keyword = keywordField.getText().trim();
        String text = keyword.isEmpty() ? where : where + " · \"" + keyword + "\" 검색 결과";
        summary.setText(text + "  " + totalCount() + "개");
    }

    /** UC-04 2a, UC-05 2a: 결과가 없으면 안내와 비슷한 검색어, 인기 상품을 보여준다. */
    private void updateEmptyState(boolean empty) {
        emptyPanel.removeAll();
        emptyPanel.setVisible(empty);
        if (!empty) {
            return;
        }
        String keyword = keywordField.getText().trim();
        String message = listingByCategory || (category != null && !hasFilters())
                ? "준비 중인 상품입니다. 곧 새 상품을 들여올게요."
                : "조건에 맞는 상품이 없습니다. 검색어나 필터를 바꿔 보세요.";
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 8));
        top.setOpaque(false);
        top.add(ShopUi.label(message, 1.1f, Font.BOLD, ShopUi.TEXT));

        List<String> suggestions = keyword.isEmpty() ? List.of() : context.product.suggestKeywords(keyword);
        if (!suggestions.isEmpty()) {
            JPanel chips = ShopUi.row(8, ShopUi.hint("이 검색어는 어떠세요?"));
            for (String suggestion : suggestions) {
                JButton chip = ShopUi.choiceButton(suggestion);
                chip.addActionListener(e -> showSearch(suggestion));
                chips.add(chip);
            }
            top.add(chips);
        }
        top.add(ShopUi.sectionHeader("이런 상품은 어떠세요?", "많이 찾는 인기 상품입니다.", null));
        emptyPanel.add(top, BorderLayout.NORTH);

        ProductGrid popular = new ProductGrid(product -> ShopUi.openProduct(frame, context, product));
        popular.setProducts(context.product.popular());
        emptyPanel.add(popular, BorderLayout.CENTER);
        emptyPanel.revalidate();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
