package com.sadamda.view.product;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Category;
import com.sadamda.view.Theme;
import com.sadamda.view.WrapLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 카테고리 탭 줄 (FR-05, UC-04 1단계): 전체  상의  하의  …  스카프.
 * 상품 목록 화면의 탭처럼 테두리 없이 글자만 두고, 아래에 얇은 구분선을 긋는다.
 * 글자는 모두 굵게 쓰고, 고르거나 마우스를 올린 탭은 진한 글씨와 밑줄. 판매 중인 상품 수는 툴팁·스크린리더로 알린다.
 * 고르면 onSelect 에 카테고리를 넘긴다 ([전체]는 null).
 */
public class CategoryBar extends JPanel {
    private final AppContext context;
    private final JToggleButton allButton;
    private final Map<Category, JToggleButton> buttons = new EnumMap<>(Category.class);
    private final boolean showAll;
    // 탭 색. 기본은 흰 바탕용이고, setColors 로 진한 띠 위에 맞게 바꿀 수 있다.
    private Color normalText = Theme.HINT;
    private Color activeText = Theme.TEXT;
    private Color underline = Theme.PRIMARY;

    /** showAll: 맨 앞에 [전체] 탭을 둘지. */
    public CategoryBar(AppContext context, boolean showAll, Consumer<Category> onSelect) {
        super(new WrapLayout(FlowLayout.LEFT, 0, 0));
        this.context = context;
        this.showAll = showAll;
        setOpaque(false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE));

        allButton = new Tab("전체");
        allButton.setToolTipText("모든 상품 보기");
        allButton.addActionListener(e -> {
            setSelected(null);
            onSelect.accept(null);
        });
        if (showAll) {
            addTab(allButton);
        }
        for (Category category : Category.values()) {
            JToggleButton button = new Tab(category.label());
            button.addActionListener(e -> {
                setSelected(category);
                onSelect.accept(category);
            });
            buttons.put(category, button);
            addTab(button);
        }
        refreshCounts();
    }

    private void addTab(JToggleButton tab) {
        if (getComponentCount() > 0) {
            add(Box.createHorizontalStrut(28));
        }
        add(tab);
    }

    /**
     * 탭 색을 바꾼다 (예: 홈 화면의 진보라 띠 위에서는 연한 글씨 → 흰 글씨·흰 밑줄).
     * 글씨 색은 배경 대비 4.5:1, 밑줄은 3:1 이상인 색을 쓴다 (NFR-01).
     */
    public void setColors(Color normal, Color active, Color underlineColor) {
        normalText = normal;
        activeText = active;
        underline = underlineColor;
        for (java.awt.Component c : getComponents()) {
            if (c instanceof Tab tab) {
                tab.updateStyle();
            }
        }
        repaint();
    }

    /** 탭의 상품 수(툴팁·스크린리더용)를 다시 센다 (관리자가 상품을 바꾼 뒤 등). */
    public void refreshCounts() {
        for (Map.Entry<Category, JToggleButton> entry : buttons.entrySet()) {
            int count = context.product.countByCategory(entry.getKey());
            entry.getValue().setToolTipText("판매 중인 상품 " + count + "개");
            entry.getValue().getAccessibleContext().setAccessibleName(
                    entry.getKey().label() + ", 상품 " + count + "개");
        }
    }

    /** 고른 카테고리를 표시한다. null 이면 [전체] (없으면 아무것도 고르지 않은 상태). */
    public void setSelected(Category selected) {
        allButton.setSelected(showAll && selected == null);
        buttons.forEach((category, button) -> button.setSelected(category == selected));
        repaint();
    }

    /** 아무 탭도 고르지 않은 상태로 (홈처럼 누르면 다른 화면으로 가는 곳에서, 돌아왔을 때 눌린 채로 남지 않게). */
    public void clearSelection() {
        allButton.setSelected(false);
        buttons.values().forEach(button -> button.setSelected(false));
        repaint();
    }

    /**
     * 글자만 있는 탭. 항상 굵은 글씨, 고르거나 마우스를 올리면 진한 글씨와 밑줄 (색은 setColors).
     * [글씨 크기]를 바꿔도 탭 글씨는 "보통" 크기로 고정한다 (탭이 한 줄에 들어가게, 상품 목록 탭과 같음).
     */
    private final class Tab extends JToggleButton {
        private static final float FIXED_SIZE = 20f;
        private static final int UNDERLINE = 3;

        Tab(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(12, 2, 14, 2)); // 높이 48px 이상 (NFR-02)
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addItemListener(e -> updateStyle());
            getModel().addChangeListener(e -> updateStyle()); // 마우스 올림
            updateStyle();
        }

        @Override
        public void updateUI() {
            setUI(new BasicToggleButtonUI());
            updateStyle();
        }

        private boolean active() {
            return isSelected() || (getModel() != null && getModel().isRollover());
        }

        private void updateStyle() {
            // 모든 탭을 항상 굵게 쓴다. 고를 때만 굵어지면 글자 폭이 바뀌어 줄이 흔들린다.
            int style = Font.BOLD;
            if (getFont() == null || getFont().getStyle() != style || getFont().getSize2D() != FIXED_SIZE) {
                setFont(UIManager.getFont("Button.font").deriveFont(style, FIXED_SIZE));
            }
            if (activeText != null) { // updateUI 는 바깥 필드가 정해지기 전에도 불릴 수 있다
                setForeground(active() ? activeText : normalText);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (active()) {
                g2.setColor(underline);
                g2.fillRect(0, getHeight() - UNDERLINE, getWidth(), UNDERLINE);
            }
            if (isFocusOwner()) { // 키보드 사용자를 위한 테두리
                g2.setColor(activeText);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - UNDERLINE - 3);
            }
            g2.dispose();
        }
    }
}
