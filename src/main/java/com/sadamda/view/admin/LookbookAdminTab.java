package com.sadamda.view.admin;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Product;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UC-17 룩북·유행 아이템 관리: 코디 등록·수정·삭제(판매 중지 상품이 섞이면 경고, 2a)와 유행 아이템 지정·해제.
 */
final class LookbookAdminTab extends JPanel {
    private final AppContext context;
    private final JTable lookbooks = AdminUi.table("번호", "코디 제목", "상황", "계절", "구성 상품");
    private final JTable trending = AdminUi.table("번호", "상품명", "카테고리", "가격", "상태");

    LookbookAdminTab(AppContext context) {
        super(new BorderLayout());
        this.context = context;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton create = AdminUi.small(Theme.primaryButton("새 코디 등록"));
        create.addActionListener(e -> editLookbook(null));
        JButton modify = AdminUi.small(Theme.secondaryButton("수정"));
        modify.addActionListener(e -> {
            Long id = AdminUi.selectedId(lookbooks);
            if (id == null) {
                AdminUi.needSelection(this);
                return;
            }
            editLookbook(context.recommend.lookbook(id));
        });
        JButton delete = AdminUi.small(Theme.secondaryButton("삭제"));
        delete.addActionListener(e -> deleteLookbook());
        JPanel top = section("룩북 코디 (UC-17 1~3단계)", lookbooks, create, modify, delete);

        JButton add = AdminUi.small(Theme.primaryButton("유행 아이템 지정"));
        add.addActionListener(e -> addTrending());
        JButton remove = AdminUi.small(Theme.secondaryButton("지정 해제"));
        remove.addActionListener(e -> removeTrending());
        JPanel bottom = section("이번 시즌 유행 아이템 (UC-17 4~5단계)", trending, add, remove);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.5);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    private static JPanel section(String title, JTable table, JButton... buttons) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(AdminUi.heading(title), BorderLayout.WEST);
        head.add(AdminUi.row(buttons), BorderLayout.EAST);
        panel.add(head, BorderLayout.NORTH);
        panel.add(AdminUi.scroll(table), BorderLayout.CENTER);
        return panel;
    }

    void refresh() {
        List<Object[]> rows = new ArrayList<>();
        context.recommend.lookbooks(null, null).stream().sorted(Comparator.comparing(Lookbook::getId))
                .forEach(l -> rows.add(new Object[]{l.getId(), l.getTitle(), nz(l.getOccasion()), nz(l.getSeason()),
                        l.getProductIds().size() + "개"}));
        AdminUi.fill(lookbooks, rows);

        List<Object[]> items = new ArrayList<>();
        context.admin.products().stream().filter(Product::isTrending).sorted(Comparator.comparing(Product::getId))
                .forEach(p -> items.add(new Object[]{p.getId(), p.getName(),
                        p.getCategory() == null ? "" : p.getCategory().label(), AdminUi.price(p.getPrice()),
                        p.isOnSale() ? "판매 중" : "판매 중지"}));
        AdminUi.fill(trending, items);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private void deleteLookbook() {
        Long id = AdminUi.selectedId(lookbooks);
        if (id == null) {
            AdminUi.needSelection(this);
            return;
        }
        if (AdminUi.confirm(this, "이 코디를 삭제할까요?", "코디 삭제")) {
            AdminUi.run(this, "코디 삭제", () -> context.admin.deleteLookbook(id));
            refresh();
        }
    }

    private void addTrending() {
        List<Product> candidates = context.admin.products().stream()
                .filter(p -> p.isOnSale() && !p.isTrending()).toList();
        List<Long> ids = ProductPicker.pick(SwingUtilities.getWindowAncestor(this), "유행 아이템으로 지정할 상품",
                candidates, true);
        for (Long id : ids) {
            AdminUi.run(this, "유행 아이템 지정", () -> context.admin.setTrending(id, true));
        }
        refresh();
    }

    private void removeTrending() {
        Long id = AdminUi.selectedId(trending);
        if (id == null) {
            AdminUi.needSelection(this);
            return;
        }
        AdminUi.run(this, "지정 해제", () -> context.admin.setTrending(id, false));
        refresh();
    }

    private void editLookbook(Lookbook lookbook) {
        LookbookDialog dialog = new LookbookDialog(SwingUtilities.getWindowAncestor(this), lookbook);
        dialog.setVisible(true);
        refresh();
    }

    /** 코디 등록·수정 창. 저장에 실패하면(판매 중지 상품 포함 등) 그 문장을 창 안에 보여 준다. */
    private final class LookbookDialog extends JDialog {
        private final Lookbook original;
        private final JTextField title = Theme.textField("예: 봄 나들이 코디");
        private final JTextField occasion = Theme.textField("예: 모임, 여행, 일상");
        private final JTextField season = Theme.textField("예: 봄");
        private final JTextField image = Theme.textField("대표 사진 주소 (선택)");
        private final JTable items = AdminUi.table("번호", "구성 상품", "상태");
        private final JLabel error = Theme.errorLabel();
        private final List<Long> productIds = new ArrayList<>();

        LookbookDialog(Window owner, Lookbook lookbook) {
            super(owner, lookbook == null ? "새 코디 등록" : "코디 수정", Dialog.ModalityType.APPLICATION_MODAL);
            this.original = lookbook;
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            form.setBackground(Theme.CARD);
            form.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
            Theme.addToCard(form, AdminUi.heading(getTitle()), 0);
            Theme.addToCard(form, Theme.field("코디 제목", null, title), 16);
            JPanel pair = new JPanel(new GridLayout(1, 2, 16, 0));
            pair.setOpaque(false);
            pair.add(Theme.field("상황", null, occasion));
            pair.add(Theme.field("계절", null, season));
            Theme.addToCard(form, pair, 16);
            Theme.addToCard(form, Theme.field("대표 사진", null, image), 16);

            JButton add = AdminUi.small(Theme.secondaryButton("상품 추가"));
            add.addActionListener(e -> {
                List<Product> candidates = context.admin.products().stream()
                        .filter(p -> !productIds.contains(p.getId())).toList();
                productIds.addAll(ProductPicker.pick(this, "코디에 넣을 상품", candidates, true));
                fillItems();
            });
            JButton remove = AdminUi.small(Theme.secondaryButton("빼기"));
            remove.addActionListener(e -> {
                Long id = AdminUi.selectedId(items);
                if (id != null) {
                    productIds.remove(id);
                    fillItems();
                }
            });
            JPanel itemsHead = new JPanel(new BorderLayout());
            itemsHead.setOpaque(false);
            itemsHead.add(Theme.label("구성 상품", 1f, java.awt.Font.BOLD, Theme.TEXT), BorderLayout.WEST);
            itemsHead.add(AdminUi.row(add, remove), BorderLayout.EAST);
            Theme.addToCard(form, itemsHead, 16);
            javax.swing.JScrollPane itemsScroll = AdminUi.scroll(items);
            itemsScroll.setPreferredSize(new java.awt.Dimension(600, 220));
            Theme.addToCard(form, itemsScroll, 8);
            Theme.addToCard(form, error, 12);

            JButton cancel = Theme.secondaryButton("닫기");
            cancel.addActionListener(e -> dispose());
            JButton save = Theme.primaryButton("저장");
            save.addActionListener(e -> save());
            JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
            buttons.setOpaque(false);
            buttons.add(cancel);
            buttons.add(save);
            buttons.setBorder(BorderFactory.createEmptyBorder(12, 32, 20, 32));

            if (lookbook != null) {
                title.setText(lookbook.getTitle());
                occasion.setText(nz(lookbook.getOccasion()));
                season.setText(nz(lookbook.getSeason()));
                image.setText(nz(lookbook.getImageUrl()));
                productIds.addAll(lookbook.getProductIds());
            }
            fillItems();
            setContentPane(AdminUi.dialogContent(form, buttons));
            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            pack();
            java.awt.Dimension screen = getToolkit().getScreenSize();
            setSize(Math.min(Math.max(getWidth() + 24, 800), screen.width - 80), Math.min(getHeight(), screen.height - 120));
            setLocationRelativeTo(owner);
        }

        private void fillItems() {
            Map<Long, Product> byId = context.admin.products().stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            List<Object[]> rows = new ArrayList<>();
            for (Long id : productIds) {
                Product p = byId.get(id);
                rows.add(new Object[]{id, p == null ? "(없는 상품)" : p.getName(),
                        p == null ? "" : p.isOnSale() ? "판매 중" : "판매 중지 — 빼야 저장돼요"});
            }
            AdminUi.fill(items, rows);
        }

        private void save() {
            error.setText("");
            Lookbook lookbook = new Lookbook(title.getText().trim(), image.getText().trim(),
                    occasion.getText().trim(), season.getText().trim(), productIds);
            if (original != null) {
                lookbook.setId(original.getId());
            }
            try {
                context.admin.saveLookbook(lookbook);
                dispose();
            } catch (com.sadamda.common.AppException e) {
                error.setText("<html>" + e.getMessage().replace("\n", "<br>") + "</html>"); // 2a 경고
                pack();
            }
        }
    }
}
