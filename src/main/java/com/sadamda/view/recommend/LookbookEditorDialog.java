package com.sadamda.view.recommend;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.SearchFilter;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.view.MainFrame;
import com.sadamda.view.product.ShopUi;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 관리자 코디 올리기·수정 창 (UC-17 1~3단계, FR-26).
 * 제목·상황·계절·대표 사진 주소를 넣고, 왼쪽 상품 목록에서 구성 상품을 골라 오른쪽으로 옮긴다.
 * 저장은 AdminController.saveLookbook. 판매 중지 상품이 섞여 있으면 Model 의 경고 문구를 그대로 보여준다 (2a).
 */
class LookbookEditorDialog extends JDialog {
    private static final List<String> DEFAULT_OCCASIONS = List.of("일상", "모임", "여행", "출근", "나들이", "경조사");
    private static final List<String> SEASONS = List.of("봄", "여름", "가을", "겨울", "사계절");

    private final MainFrame frame;
    private final AppContext context;
    private final Lookbook original;

    private final JTextField titleField = new JTextField();
    private final JComboBox<String> occasionBox = new JComboBox<>();
    private final JComboBox<String> seasonBox = new JComboBox<>(SEASONS.toArray(new String[0]));
    private final JTextField imageField = new JTextField();

    private final JTextField searchField = new JTextField();
    private final JComboBox<Object> categoryBox = new JComboBox<>();
    private final DefaultListModel<Product> candidates = new DefaultListModel<>();
    private final DefaultListModel<Product> chosen = new DefaultListModel<>();
    private final JList<Product> candidateList = new JList<>(candidates);
    private final JList<Product> chosenList = new JList<>(chosen);

    private Lookbook saved;

    LookbookEditorDialog(MainFrame frame, AppContext context, Lookbook original, List<Product> currentProducts) {
        super(frame, original == null ? "새 코디 올리기" : "코디 수정", true);
        this.frame = frame;
        this.context = context;
        this.original = original;

        Set<String> occasions = new LinkedHashSet<>(DEFAULT_OCCASIONS);
        occasions.addAll(context.recommend.occasions());
        occasions.forEach(occasionBox::addItem);
        occasionBox.setEditable(true); // 목록에 없는 상황도 직접 쓸 수 있다

        if (original != null) {
            titleField.setText(original.getTitle());
            occasionBox.setSelectedItem(original.getOccasion());
            seasonBox.setSelectedItem(original.getSeason());
            imageField.setText(original.getImageUrl() == null ? "" : original.getImageUrl());
            currentProducts.forEach(chosen::addElement);
        }

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(ShopUi.CARD);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        root.add(formPanel(), BorderLayout.NORTH);
        root.add(pickerPanel(), BorderLayout.CENTER);
        root.add(buttonPanel(), BorderLayout.SOUTH);
        setContentPane(root);

        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        reloadCandidates();
        setSize(new Dimension(1100, 760));
        setLocationRelativeTo(frame);
    }

    /** 저장한 코디. 취소했으면 null. */
    Lookbook saved() {
        return saved;
    }

    private JComponent formPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addField(form, 0, "코디 제목", titleField);
        addField(form, 1, "상황", occasionBox);
        addField(form, 2, "계절", seasonBox);
        addField(form, 3, "대표 사진 주소 (선택)", imageField);
        imageField.setToolTipText("비워 두면 구성 상품 사진을 모아 보여줍니다");
        return form;
    }

    private static void addField(JPanel form, int row, String name, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(row == 0 ? 0 : 8, 0, 0, 12);
        c.anchor = GridBagConstraints.WEST;
        form.add(ShopUi.label(name, 1f, Font.BOLD, ShopUi.TEXT), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(row == 0 ? 0 : 8, 0, 0, 0);
        field.setPreferredSize(new Dimension(400, ShopUi.MIN_TOUCH));
        field.getAccessibleContext().setAccessibleName(name);
        form.add(field, c);
    }

    private JComponent pickerPanel() {
        categoryBox.addItem("전체 카테고리");
        for (Category category : Category.values()) {
            categoryBox.addItem(category);
        }
        categoryBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Category category) {
                    setText(category.label());
                }
                return this;
            }
        });
        categoryBox.addActionListener(e -> reloadCandidates());
        categoryBox.setPreferredSize(new Dimension(180, ShopUi.MIN_TOUCH));
        searchField.addActionListener(e -> reloadCandidates());
        searchField.getAccessibleContext().setAccessibleName("상품 찾기");
        JButton find = ShopUi.secondaryButton("찾기");
        find.addActionListener(e -> reloadCandidates());

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(categoryBox, BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(find, BorderLayout.EAST);
        left.add(ShopUi.label("판매 중인 상품", 1f, Font.BOLD, ShopUi.TEXT), BorderLayout.NORTH);
        JPanel leftBody = new JPanel(new BorderLayout(0, 8));
        leftBody.setOpaque(false);
        leftBody.add(searchRow, BorderLayout.NORTH);
        leftBody.add(listScroll(candidateList), BorderLayout.CENTER);
        left.add(leftBody, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.add(ShopUi.label("코디 구성 상품 (위에서부터 보여집니다)", 1f, Font.BOLD, ShopUi.TEXT), BorderLayout.NORTH);
        right.add(listScroll(chosenList), BorderLayout.CENTER);

        JButton add = ShopUi.primaryButton("넣기 →");
        add.addActionListener(e -> addSelected());
        JButton remove = ShopUi.secondaryButton("← 빼기");
        remove.addActionListener(e -> removeSelected());
        JButton up = ShopUi.secondaryButton("위로");
        up.addActionListener(e -> move(-1));
        JButton down = ShopUi.secondaryButton("아래로");
        down.addActionListener(e -> move(1));
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));
        buttons.setOpaque(false);
        buttons.add(add);
        buttons.add(remove);
        buttons.add(up);
        buttons.add(down);
        middle.add(buttons);

        // 두 번 누르면 바로 넣기·빼기
        candidateList.addMouseListener(doubleClick(this::addSelected));
        chosenList.addMouseListener(doubleClick(this::removeSelected));

        JPanel picker = new JPanel(new GridBagLayout());
        picker.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 1;
        picker.add(left, c);
        c.weightx = 0;
        c.insets = new Insets(0, 12, 0, 12);
        picker.add(middle, c);
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        picker.add(right, c);
        return picker;
    }

    private static MouseAdapter doubleClick(Runnable action) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    action.run();
                }
            }
        };
    }

    private static JScrollPane listScroll(JList<Product> list) {
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setFixedCellHeight(-1);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                if (value instanceof Product product) {
                    String state = !product.isOnSale() ? " [판매 중지]" : product.isSoldOut() ? " [품절]" : "";
                    setText("[" + product.getCategory().label() + "] " + product.getName() + " · "
                            + ShopUi.price(product.getPrice()) + state);
                }
                setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(420, 300));
        return scroll;
    }

    private JComponent buttonPanel() {
        JButton save = ShopUi.primaryButton(original == null ? "코디 올리기" : "수정 저장");
        save.addActionListener(e -> save());
        JButton cancel = ShopUi.secondaryButton("취소");
        cancel.addActionListener(e -> dispose());
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(ShopUi.row(10, save, cancel), BorderLayout.EAST);
        return row;
    }

    private void reloadCandidates() {
        Object category = categoryBox.getSelectedItem();
        String keyword = searchField.getText().isBlank() ? null : searchField.getText().trim();
        candidates.clear();
        for (Product product : context.product.search(new SearchFilter(keyword, null, null, null, null, null, null, null))) {
            if (!(category instanceof Category c) || product.getCategory() == c) {
                candidates.addElement(product);
            }
        }
    }

    private void addSelected() {
        for (Product product : candidateList.getSelectedValuesList()) {
            if (!contains(chosen, product)) {
                chosen.addElement(product);
            }
        }
    }

    private void removeSelected() {
        for (Product product : chosenList.getSelectedValuesList()) {
            chosen.removeElement(product);
        }
    }

    private void move(int delta) {
        int index = chosenList.getSelectedIndex();
        int target = index + delta;
        if (index < 0 || target < 0 || target >= chosen.size()) {
            return;
        }
        Product product = chosen.remove(index);
        chosen.add(target, product);
        chosenList.setSelectedIndex(target);
    }

    private static boolean contains(DefaultListModel<Product> model, Product product) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).getId().equals(product.getId())) {
                return true;
            }
        }
        return false;
    }

    /** UC-17 3단계 저장. 실패하면 Model 의 안내 문구를 보여주고 창을 유지한다. */
    private void save() {
        List<Long> productIds = new ArrayList<>();
        for (int i = 0; i < chosen.size(); i++) {
            productIds.add(chosen.get(i).getId());
        }
        String image = imageField.getText().isBlank() ? null : imageField.getText().trim();
        if (image == null && !productIds.isEmpty()) {
            image = chosen.get(0).mainImageUrl();
        }
        Object occasion = occasionBox.getSelectedItem();
        Lookbook lookbook = new Lookbook(titleField.getText().trim(), image,
                occasion == null || occasion.toString().isBlank() ? null : occasion.toString().trim(),
                (String) seasonBox.getSelectedItem(), productIds);
        if (original != null) {
            lookbook.setId(original.getId()); // 같은 id 로 저장하면 수정이 된다
        }
        try {
            saved = context.admin.saveLookbook(lookbook);
            JOptionPane.showMessageDialog(this, original == null ? "코디를 올렸습니다." : "코디를 수정했습니다.");
            dispose();
        } catch (RuntimeException e) {
            ShopUi.showError(this, frame, e);
        }
    }
}
