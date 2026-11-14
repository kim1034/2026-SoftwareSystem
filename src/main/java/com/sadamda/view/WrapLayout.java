package com.sadamda.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * 폭이 모자라면 다음 줄로 넘기는 FlowLayout. (FlowLayout 은 넘쳐도 높이를 한 줄로 알려 줘서 버튼이 잘린다)
 * 글씨를 키우거나 창을 줄여도 사이즈·색상 같은 선택 버튼이 모두 보이게 한다.
 */
public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension size = layoutSize(target, false);
        size.width -= getHgap() + 1;
        return size;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            // 아직 폭이 정해지지 않았으면 폭이 있는 부모 기준으로 계산한다.
            Container sized = target;
            while (sized.getWidth() == 0 && sized.getParent() != null) {
                sized = sized.getParent();
            }
            int targetWidth = sized.getWidth() == 0 ? Integer.MAX_VALUE : sized.getWidth();

            Insets insets = target.getInsets();
            int horizontalSpace = insets.left + insets.right + getHgap() * 2;
            int maxWidth = targetWidth - horizontalSpace;
            Dimension size = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;
            for (Component component : target.getComponents()) {
                if (!component.isVisible()) {
                    continue;
                }
                Dimension d = preferred ? component.getPreferredSize() : component.getMinimumSize();
                if (rowWidth > 0 && rowWidth + getHgap() + d.width > maxWidth) {
                    addRow(size, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth > 0) {
                    rowWidth += getHgap();
                }
                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            addRow(size, rowWidth, rowHeight);
            size.width += horizontalSpace;
            size.height += insets.top + insets.bottom + getVgap() * 2;
            return size;
        }
    }

    private void addRow(Dimension size, int rowWidth, int rowHeight) {
        size.width = Math.max(size.width, rowWidth);
        if (size.height > 0) {
            size.height += getVgap();
        }
        size.height += rowHeight;
    }
}
