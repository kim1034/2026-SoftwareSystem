package com.sadamda.view.admin;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UC-16 상품 관리: 판매 중지 포함 전체 상품 목록, 등록·수정(5a 필수 항목 안내), 판매 중지·재개, 삭제(3a).
 */
final class ProductAdminTab extends JPanel {
    private static final String ALL = "전체 카테고리";

    private final AppContext context;
    private final JTable table = AdminUi.table("번호", "상품명", "카테고리", "가격", "재고", "상태", "유행");
    private final JTextField keyword = Theme.textField("상품명으로 찾기");
    private final JComboBox<String> category;
    private final JButton suspend = AdminUi.small(Theme.secondaryButton("판매 중지"));

    ProductAdminTab(AppContext context) {
        super(new BorderLayout(0, 16));
        this.context = context;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        List<String> categories = new ArrayList<>(List.of(ALL));
        for (Category c : Category.values()) {
            categories.add(c.label());
        }
        category = Theme.comboBox(categories.toArray(String[]::new));
        keyword.setColumns(16);
        JButton search = AdminUi.small(Theme.secondaryButton("찾기"));
        search.addActionListener(e -> refresh());
        keyword.addActionListener(e -> refresh());
        category.addActionListener(e -> refresh());
        JButton create = AdminUi.small(Theme.primaryButton("새 상품 등록"));
        create.addActionListener(e -> edit(null));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(AdminUi.row(keyword, category, search), BorderLayout.WEST);
        top.add(AdminUi.row(create), BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        add(AdminUi.scroll(table), BorderLayout.CENTER);

        JButton modify = AdminUi.small(Theme.secondaryButton("수정"));
        modify.addActionListener(e -> withSelected(id -> edit(context.admin.products().stream()
                .filter(p -> p.getId().equals(id)).findFirst().orElse(null))));
        suspend.addActionListener(e -> withSelected(this::toggleSale));
        JButton delete = AdminUi.small(Theme.secondaryButton("삭제"));
        delete.addActionListener(e -> withSelected(this::delete));
        table.getSelectionModel().addListSelectionListener(e -> updateSuspendText());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    modify.doClick();
                }
            }
        });
        add(AdminUi.row(modify, suspend, delete,
                AdminUi.hint("줄을 두 번 누르면 수정합니다.")), BorderLayout.SOUTH);
    }

    void refresh() {
        String word = keyword.getText().trim();
        String chosen = (String) category.getSelectedItem();
        List<Object[]> rows = new ArrayList<>();
        context.admin.products().stream()
                .filter(p -> word.isEmpty() || p.getName().contains(word))
                .filter(p -> ALL.equals(chosen) || (p.getCategory() != null && p.getCategory().label().equals(chosen)))
                .sorted(Comparator.comparing(Product::getId))
                .forEach(p -> rows.add(new Object[]{p.getId(), p.getName(),
                        p.getCategory() == null ? "" : p.getCategory().label(), AdminUi.price(p.getPrice()),
                        p.getOptions().stream().mapToInt(ProductOption::stock).sum(),
                        p.isOnSale() ? "판매 중" : "판매 중지", p.isTrending() ? "유행" : ""}));
        AdminUi.fill(table, rows);
        updateSuspendText();
    }

    private void withSelected(java.util.function.Consumer<Long> action) {
        Long id = AdminUi.selectedId(table);
        if (id == null) {
            AdminUi.needSelection(this);
            return;
        }
        action.accept(id);
    }

    private Product find(Long id) {
        return context.admin.products().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    private void updateSuspendText() {
        Long id = AdminUi.selectedId(table);
        Product product = id == null ? null : find(id);
        suspend.setText(product != null && !product.isOnSale() ? "판매 재개" : "판매 중지");
    }

    /** UC-16 4~5단계: 등록(product 가 null)·수정 창. 저장하면 목록을 새로 그린다. */
    private void edit(Product product) {
        ProductEditDialog dialog = new ProductEditDialog(SwingUtilities.getWindowAncestor(this), context, product);
        dialog.setVisible(true);
        if (dialog.saved()) {
            refresh();
        }
    }

    /** UC-16 3a 판매 중지 / 다시 판매. */
    private void toggleSale(Long id) {
        Product product = find(id);
        if (product == null) {
            return;
        }
        if (product.isOnSale()) {
            if (AdminUi.confirm(this, "'" + product.getName() + "'을(를) 판매 중지할까요?\n목록·검색·추천에서 빠집니다.",
                    "판매 중지")) {
                AdminUi.run(this, "판매 중지", () -> context.admin.suspendProduct(id));
            }
        } else {
            Product copy = ProductEditDialog.copyOf(product);
            copy.setStatus(Product.Status.ON_SALE);
            AdminUi.run(this, "판매 재개", () -> context.admin.saveProduct(copy));
        }
        refresh();
    }

    /** UC-16 3단계 삭제. 진행 중인 주문이 있으면 지울 수 없어 판매 중지를 권한다 (3a). */
    private void delete(Long id) {
        Product product = find(id);
        if (product == null) {
            return;
        }
        if (Boolean.TRUE.equals(AdminUi.call(this, "삭제", () -> context.admin.hasActiveOrders(id)))) {
            if (AdminUi.confirm(this, "'" + product.getName() + "'은(는) 진행 중인 주문이 있어 삭제할 수 없어요.\n"
                    + "대신 판매 중지로 바꿀까요?", "삭제할 수 없어요")) {
                AdminUi.run(this, "판매 중지", () -> context.admin.suspendProduct(id));
            }
        } else if (AdminUi.confirm(this, "'" + product.getName() + "'을(를) 삭제할까요? 되돌릴 수 없어요.", "상품 삭제")) {
            AdminUi.run(this, "삭제", () -> context.admin.deleteProduct(id));
        }
        refresh();
    }
}
