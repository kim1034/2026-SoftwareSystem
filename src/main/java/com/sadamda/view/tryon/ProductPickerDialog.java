package com.sadamda.view.tryon;

import com.sadamda.model.product.Product;
import com.sadamda.model.tryon.DressUpSlot;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 옷 입혀보기에서 한 자리(모자·상의·하의·주얼리·신발)에 입힐 상품을 고르는 창. */
class ProductPickerDialog extends JDialog {
    private static final int THUMBNAIL = 88;
    private static final String ALL = "전체";

    private final List<Product> products;
    private final DefaultListModel<Product> model = new DefaultListModel<>();
    private final JList<Product> list = new JList<>(model);
    private final JTextField keyword = new JTextField(14);
    private final JComboBox<String> kind = new JComboBox<>();
    private Product chosen;
    private boolean takeOff;

    ProductPickerDialog(Window owner, DressUpSlot slot, List<Product> products, boolean wearing) {
        super(owner, slot.label() + " 고르기", ModalityType.APPLICATION_MODAL);
        this.products = products;

        Set<String> kinds = new LinkedHashSet<>();
        kinds.add(ALL);
        products.forEach(product -> kinds.add(product.getSubCategory()));
        kinds.forEach(kind::addItem);
        kind.addActionListener(e -> refresh());
        keyword.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        if (kinds.size() > 2) {
            filters.add(new JLabel("종류"));
            filters.add(kind);
        }
        filters.add(new JLabel("찾기"));
        filters.add(keyword);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(THUMBNAIL + 16);
        list.setCellRenderer(new Renderer());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    choose();
                }
            }
        });

        JButton wear = button("이 옷 입히기");
        wear.addActionListener(e -> choose());
        JButton off = button("벗기");
        off.setEnabled(wearing);
        off.addActionListener(e -> {
            takeOff = true;
            dispose();
        });
        JButton close = button("닫기");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(off);
        buttons.add(close);
        buttons.add(wear);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(filters, BorderLayout.NORTH);
        content.add(new JScrollPane(list), BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(wear);

        refresh();
        setSize(760, 680);
        setLocationRelativeTo(owner);
    }

    /** 고른 상품. 고르지 않고 닫았으면 null. */
    Product chosen() {
        return chosen;
    }

    /** [벗기]를 눌렀는지. */
    boolean takeOff() {
        return takeOff;
    }

    private void choose() {
        if (list.getSelectedValue() != null) {
            chosen = list.getSelectedValue();
            dispose();
        }
    }

    private void refresh() {
        String word = keyword.getText().trim().toLowerCase(Locale.ROOT);
        Object selectedKind = kind.getSelectedItem();
        model.clear();
        for (Product product : products) {
            boolean kindMatches = selectedKind == null || ALL.equals(selectedKind)
                    || selectedKind.equals(product.getSubCategory());
            String text = (product.getName() + " " + product.getBrand() + " " + product.getMaterial()
                    + " " + product.getDescription()).toLowerCase(Locale.ROOT);
            if (kindMatches && (word.isEmpty() || text.contains(word))) {
                model.addElement(product);
            }
        }
        if (!model.isEmpty()) {
            list.setSelectedIndex(0);
        }
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        Dimension size = button.getPreferredSize();
        button.setPreferredSize(new Dimension(Math.max(size.width, 96), Math.max(size.height, 48))); // NFR-02
        return button;
    }

    private class Renderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> owner, Object value, int index,
                                                      boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(owner, value, index, selected, focused);
            Product product = (Product) value;
            label.setIcon(Thumbnails.get(product.mainImageUrl(), THUMBNAIL, list::repaint));
            label.setIconTextGap(16);
            label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            label.setText("<html><b>" + escape(product.getName()) + "</b><br>" + escape(product.getBrand())
                    + " · " + String.format("%,d원", product.getPrice()) + "</html>");
            return label;
        }
    }

    static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
