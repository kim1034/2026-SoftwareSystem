package com.sadamda.view.admin;

import com.sadamda.model.product.Product;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 상품을 찾아 고르는 창 (룩북 구성 상품, 유행 아이템 지정). multiple 이면 여러 개를 고를 수 있다. */
final class ProductPicker extends JDialog {
    private final List<Product> all;
    private final JTable table = AdminUi.table("번호", "상품명", "카테고리", "가격", "상태");
    private final JTextField keyword = Theme.textField("상품명으로 찾기");
    private List<Long> chosen = List.of();

    private ProductPicker(Window owner, String title, List<Product> products, boolean multiple) {
        super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        this.all = products.stream().sorted(Comparator.comparing(Product::getId)).toList();
        if (multiple) {
            table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(Theme.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        keyword.setColumns(20);
        keyword.addActionListener(e -> fill());
        JButton search = AdminUi.small(Theme.secondaryButton("찾기"));
        search.addActionListener(e -> fill());
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(AdminUi.heading(title), BorderLayout.NORTH);
        top.add(AdminUi.row(keyword, search,
                AdminUi.hint(multiple ? "Ctrl 을 누른 채 누르면 여러 개를 고를 수 있어요." : "")), BorderLayout.CENTER);
        content.add(top, BorderLayout.NORTH);
        content.add(AdminUi.scroll(table), BorderLayout.CENTER);

        JButton cancel = Theme.secondaryButton("닫기");
        cancel.addActionListener(e -> dispose());
        JButton ok = Theme.primaryButton("고르기");
        ok.addActionListener(e -> {
            List<Long> ids = new ArrayList<>();
            for (int view : table.getSelectedRows()) {
                ids.add(((Number) table.getModel().getValueAt(table.convertRowIndexToModel(view), 0)).longValue());
            }
            chosen = ids;
            dispose();
        });
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        fill();
        setSize(900, 640);
        setLocationRelativeTo(owner);
    }

    /** 고른 상품 번호. 닫기를 누르면 빈 목록. */
    static List<Long> pick(Window owner, String title, List<Product> products, boolean multiple) {
        ProductPicker picker = new ProductPicker(owner, title, products, multiple);
        picker.setVisible(true);
        return picker.chosen;
    }

    private void fill() {
        String word = keyword.getText().trim();
        List<Object[]> rows = new ArrayList<>();
        for (Product p : all) {
            if (word.isEmpty() || p.getName().contains(word)) {
                rows.add(new Object[]{p.getId(), p.getName(), p.getCategory() == null ? "" : p.getCategory().label(),
                        AdminUi.price(p.getPrice()), p.isOnSale() ? "판매 중" : "판매 중지"});
            }
        }
        AdminUi.fill(table, rows);
    }
}
