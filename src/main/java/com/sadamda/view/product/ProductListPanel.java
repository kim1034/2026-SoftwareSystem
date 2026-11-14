package com.sadamda.view.product;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Category;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

/** UC-04 카테고리별 상품 조회: 큰 카테고리 버튼 + 큰 이미지 카드 목록 + [더 보기]. (UC-05 검색·필터는 아직) */
public class ProductListPanel extends ScreenPanel {
    public static final String NAME = "productList";

    private final CardGrid grid = new CardGrid();
    private final JLabel notice = Theme.label(" ", 1f, Font.BOLD, Theme.TEXT);
    private final JLabel noticeHint = Theme.label(" ", 1f, Font.PLAIN, Theme.HINT);
    private final JPanel noticeBox = new JPanel(new BorderLayout(0, 4));
    private final JButton moreButton = Theme.secondaryButton("더 보기");
    private final JScrollPane scroll;

    private Category category;
    private int page;

    public ProductListPanel(AppContext context, MainFrame frame) {
        super(context, frame, "상품", "UC-04 카테고리별 상품 조회, UC-05 상품 검색 및 필터");
        hideTitle(); // 제목은 위쪽 머리말에 직접 그린다.
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createEmptyBorder()); // 머리말 줄이 화면 끝까지 닿게

        // 위쪽 머리말: 제목 + 큰 카테고리 버튼. 목록을 내려도 그대로 보인다.
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        chips.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Category each : Category.values()) {
            JToggleButton chip = Theme.chip(each.label());
            chip.addActionListener(e -> showCategory(each));
            group.add(chip);
            chips.add(chip);
            if (each == Category.TOP) {
                chip.setSelected(true); // 처음에는 상의
            }
        }
        JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(24, 48, 24, 48)));
        header.add(Theme.label("상품", 1.6f, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        header.add(chips, BorderLayout.CENTER);

        // 상품이 없을 때 안내 (UC-04 2a)
        noticeBox.setOpaque(false);
        noticeBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        noticeBox.add(notice, BorderLayout.NORTH);
        noticeBox.add(noticeHint, BorderLayout.CENTER);

        moreButton.addActionListener(e -> showMore());
        JPanel moreRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        moreRow.setOpaque(false);
        moreRow.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        moreRow.add(moreButton);

        Body body = new Body();
        body.add(noticeBox, BorderLayout.NORTH);
        body.add(grid, BorderLayout.CENTER);
        body.add(moreRow, BorderLayout.SOUTH);
        scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel screen = new JPanel(new BorderLayout());
        screen.setOpaque(false);
        screen.add(header, BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        setContent(screen);

        // 처음 열 때 한 번만 불러온다. 다른 화면에 다녀와도 보던 카테고리와 위치를 유지한다.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (category == null) {
                    showCategory(Category.TOP);
                }
            }
        });
    }

    /** UC-04 1~2단계: 카테고리의 첫 페이지를 보여준다. 없으면 인기 상품을 대신 보여준다 (2a). */
    private void showCategory(Category selected) {
        category = selected;
        page = 0;
        grid.removeAll();
        List<ProductItem> items = fetch(category, page);
        if (items.isEmpty()) {
            notice.setText("준비 중인 상품입니다");
            noticeHint.setText("대신 지금 인기 있는 상품을 보여 드릴게요.");
            noticeBox.setVisible(true);
            addCards(fetchPopular());
            moreButton.getParent().setVisible(false);
        } else {
            noticeBox.setVisible(false);
            addCards(items);
            moreButton.getParent().setVisible(items.size() == MockProducts.PAGE_SIZE);
        }
        refresh();
        scroll.getViewport().setViewPosition(new Point(0, 0));
    }

    /** UC-04 3단계 [더 보기]: 다음 페이지를 아래에 덧붙인다. 더 없으면 버튼을 감춘다. */
    private void showMore() {
        page++;
        List<ProductItem> items = fetch(category, page);
        int firstNew = grid.getComponentCount();
        addCards(items);
        moreButton.getParent().setVisible(items.size() == MockProducts.PAGE_SIZE);
        refresh();
        if (firstNew < grid.getComponentCount()) {
            grid.getComponent(firstNew).requestFocusInWindow(); // 키보드 사용자가 새 상품부터 이어 볼 수 있게
        }
    }

    // TODO Product getter 가 생기면 context.product.listByCategory(category, page) 결과를 ProductItem 으로 바꿔 쓴다.
    private List<ProductItem> fetch(Category category, int page) {
        return MockProducts.listByCategory(category, page);
    }

    // TODO 위와 같이 context.product.popular() 로 바꾼다.
    private List<ProductItem> fetchPopular() {
        return MockProducts.popular();
    }

    private void addCards(List<ProductItem> items) {
        for (ProductItem item : items) {
            // TODO UC-06 상세 화면이 생기면 item.id() 를 넘긴다. 지금은 화면 이동만 한다.
            grid.add(new ProductCard(item, () -> frame.navigate(ProductDetailPanel.NAME)));
        }
    }

    private void refresh() {
        grid.revalidate();
        grid.repaint();
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
