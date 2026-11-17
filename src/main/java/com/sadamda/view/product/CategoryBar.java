package com.sadamda.view.product;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Category;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 큰 카테고리 버튼 줄 (FR-05, UC-04 1단계): [전체] [상의] [하의] … [주얼리].
 * 버튼에는 판매 중인 상품 수를 함께 적는다. 고르면 onSelect 에 카테고리를 넘긴다 ([전체]는 null).
 */
public class CategoryBar extends JPanel {
    private final AppContext context;
    private final JToggleButton allButton;
    private final Map<Category, JToggleButton> buttons = new EnumMap<>(Category.class);
    private final boolean showAll;

    /** 카테고리 칸이 좁아서(최소 130px) 칩의 좌우 여백을 줄여 "주얼리 12"까지 잘리지 않게 한다. */
    private static JToggleButton chip(String text) {
        JToggleButton chip = ShopUi.choiceButton(text);
        chip.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        return chip;
    }

    /** showAll: [전체] 버튼을 둘지. 홈 화면처럼 "카테고리로 찾기"만 필요한 곳은 false. */
    public CategoryBar(AppContext context, boolean showAll, Consumer<Category> onSelect) {
        super(new ProductGrid.ResponsiveGridLayout(130, 10, 5, true));
        this.context = context;
        this.showAll = showAll;
        setOpaque(false);

        allButton = chip("전체");
        allButton.addActionListener(e -> {
            setSelected(null);
            onSelect.accept(null);
        });
        if (showAll) {
            add(allButton);
        }
        for (Category category : Category.values()) {
            JToggleButton button = chip(category.label());
            button.addActionListener(e -> {
                setSelected(category);
                onSelect.accept(category);
            });
            buttons.put(category, button);
            add(button);
        }
        refreshCounts();
    }

    /** 버튼의 상품 수를 다시 센다 (관리자가 상품을 바꾼 뒤 등). */
    public void refreshCounts() {
        for (Map.Entry<Category, JToggleButton> entry : buttons.entrySet()) {
            int count = context.product.countByCategory(entry.getKey());
            entry.getValue().setText(entry.getKey().label() + " " + count);
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
}
