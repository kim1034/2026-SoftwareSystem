package com.sadamda.view.product;

import com.sadamda.model.product.Product;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.List;
import java.util.function.Consumer;

/**
 * 상품 카드를 창 폭에 맞춰 여러 줄로 늘어놓는 판.
 * 창이 넓으면 한 줄에 많이, 좁거나 글씨가 크면 적게 놓는다. 세로 스크롤 안에 두고 쓴다.
 */
public class ProductGrid extends JPanel {
    private final Consumer<Product> onOpen;

    public ProductGrid(Consumer<Product> onOpen) {
        super(new ResponsiveGridLayout(ShopProductCard.IMAGE_WIDTH + 24, 16, 4));
        this.onOpen = onOpen;
        setOpaque(false);
    }

    /** 카드를 모두 바꾼다. */
    public void setProducts(List<Product> products) {
        removeAll();
        addProducts(products);
    }

    /** [더 보기]: 지금 카드 뒤에 이어 붙인다. */
    public void addProducts(List<Product> products) {
        for (Product product : products) {
            add(new ShopProductCard(product, onOpen));
        }
        revalidate();
        repaint();
    }

    public int count() {
        return getComponentCount();
    }

    /**
     * 칸 폭이 minCellWidth 이상이 되도록 열 수를 정하고, 줄마다 가장 큰 카드 높이에 맞춘다.
     * 세로 스크롤 안에서는 스크롤 영역의 폭을 기준으로 계산해 가로로 넘치지 않는다.
     */
    static final class ResponsiveGridLayout implements LayoutManager {
        private final int minCellWidth;
        private final int gap;
        private final int defaultColumns;
        private final boolean balanced;

        ResponsiveGridLayout(int minCellWidth, int gap, int defaultColumns) {
            this(minCellWidth, gap, defaultColumns, false);
        }

        /** balanced: 여러 줄이 되면 줄마다 개수를 고르게 한다 (8개면 7+1 대신 4+4). */
        ResponsiveGridLayout(int minCellWidth, int gap, int defaultColumns, boolean balanced) {
            this.minCellWidth = minCellWidth;
            this.gap = gap;
            this.defaultColumns = defaultColumns;
            this.balanced = balanced;
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        private int availableWidth(Container parent) {
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, parent);
            int width = parent.getWidth();
            if (viewport != null && viewport.getWidth() > 0) {
                // 바깥 여백만큼 빼야 가로 스크롤이 생기지 않는다
                width = viewport.getWidth() - outerMargin(parent, viewport);
            }
            Insets insets = parent.getInsets();
            return width - insets.left - insets.right;
        }

        /** viewport 와 이 판 사이에 있는 테두리 여백의 합. */
        private static int outerMargin(Container parent, JViewport viewport) {
            int margin = 0;
            for (Container c = parent.getParent(); c != null && c != viewport; c = c.getParent()) {
                Insets insets = c.getInsets();
                margin += insets.left + insets.right;
            }
            return margin;
        }

        private int columns(Container parent, int width) {
            if (width <= 0) {
                return defaultColumns;
            }
            int columns = Math.max(1, (width + gap) / (minCellWidth + gap));
            int count = parent.getComponentCount();
            if (balanced && count > columns) {
                int rows = (count + columns - 1) / columns;
                columns = (count + rows - 1) / rows;
            }
            return columns;
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            int width = availableWidth(parent);
            int columns = columns(parent, width);
            int height = 0;
            int count = parent.getComponentCount();
            for (int start = 0; start < count; start += columns) {
                height += rowHeight(parent, start, columns) + (start > 0 ? gap : 0);
            }
            Insets insets = parent.getInsets();
            int prefWidth = width > 0 ? width : columns * minCellWidth + (columns - 1) * gap;
            return new Dimension(prefWidth + insets.left + insets.right, height + insets.top + insets.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(minCellWidth, 0);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int width = parent.getWidth() - insets.left - insets.right;
            int columns = columns(parent, width);
            int cellWidth = Math.max(1, (width - (columns - 1) * gap) / columns);
            int y = insets.top;
            int count = parent.getComponentCount();
            for (int start = 0; start < count; start += columns) {
                int rowHeight = rowHeight(parent, start, columns);
                for (int i = start; i < Math.min(start + columns, count); i++) {
                    int column = i - start;
                    parent.getComponent(i).setBounds(insets.left + column * (cellWidth + gap), y, cellWidth, rowHeight);
                }
                y += rowHeight + gap;
            }
        }

        private static int rowHeight(Container parent, int start, int columns) {
            int height = 0;
            for (int i = start; i < Math.min(start + columns, parent.getComponentCount()); i++) {
                height = Math.max(height, parent.getComponent(i).getPreferredSize().height);
            }
            return height;
        }
    }
}
