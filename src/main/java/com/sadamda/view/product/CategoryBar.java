package com.sadamda.view.product;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Category;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 큰 카테고리 버튼 줄 (FR-05, UC-04 1단계): [전체] [상의] [하의] … [주얼리].
 * 버튼에는 판매 중인 상품 수를 함께 적는다. 고르면 onSelect 에 카테고리를 넘긴다 ([전체]는 null).
 */
public class CategoryBar extends JPanel {
    private final AppContext context;
    private final JButton allButton;
    private final Map<Category, JButton> buttons = new EnumMap<>(Category.class);
    private final boolean showAll;

    /** showAll: [전체] 버튼을 둘지. 홈 화면처럼 "카테고리로 찾기"만 필요한 곳은 false. */
    public CategoryBar(AppContext context, boolean showAll, Consumer<Category> onSelect) {
        super(new ProductGrid.ResponsiveGridLayout(130, 10, 5, true));
        this.context = context;
        this.showAll = showAll;
        setOpaque(false);

        allButton = ShopUi.choiceButton("전체");
        allButton.addActionListener(e -> {
            setSelected(null);
            onSelect.accept(null);
        });
        if (showAll) {
            add(allButton);
        }
        for (Category category : Category.values()) {
            JButton button = ShopUi.choiceButton(category.label());
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
        for (Map.Entry<Category, JButton> entry : buttons.entrySet()) {
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
