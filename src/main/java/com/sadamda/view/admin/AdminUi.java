package com.sadamda.view.admin;

import com.sadamda.common.AppException;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** 관리자 화면 공통 부품: 읽기 전용 표, 버튼 줄, 실패 안내. */
final class AdminUi {
    private AdminUi() {
    }

    /** 고칠 수 없는 표. 한 줄만 고를 수 있고 줄 높이는 글씨 크기에 맞춘다. */
    static JTable table(String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model) {
            @Override
            public void updateUI() {
                super.updateUI();
                setRowHeight(getFontMetrics(getFont()).getHeight() + 16);
            }
        };
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    static JScrollPane scroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        return scroll;
    }

    /** 표 내용을 rows 로 바꾼다. */
    static void fill(JTable table, List<Object[]> rows) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        rows.forEach(model::addRow);
    }

    /** 고른 줄의 첫 칸(번호). 고른 줄이 없으면 null. */
    static Long selectedId(JTable table) {
        int view = table.getSelectedRow();
        if (view < 0) {
            return null;
        }
        Object value = table.getModel().getValueAt(table.convertRowIndexToModel(view), 0);
        return value instanceof Number n ? n.longValue() : null;
    }

    /** 입력 칸은 세로로만 스크롤하고, [닫기]·[저장] 버튼은 창 아래에 늘 보이게 둔다. */
    static JPanel dialogContent(JPanel form, JPanel buttons) {
        JScrollPane scroll = new JScrollPane(form, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel content = new JPanel(new java.awt.BorderLayout());
        content.setBackground(Theme.CARD);
        content.add(scroll, java.awt.BorderLayout.CENTER);
        buttons.setOpaque(true);
        buttons.setBackground(Theme.CARD);
        content.add(buttons, java.awt.BorderLayout.SOUTH);
        return content;
    }

    static JPanel row(JComponent... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        for (JComponent c : components) {
            row.add(c);
        }
        return row;
    }

    static JButton small(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        return button;
    }

    static JLabel heading(String text) {
        return Theme.label(text, 1.2f, Font.BOLD, Theme.TEXT);
    }

    static JLabel hint(String text) {
        return Theme.label(text, 1f, Font.PLAIN, Theme.HINT);
    }

    static String price(int won) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(won) + "원";
    }

    /** 일을 하고, AppException 이면 그 문장을 창으로 보여 준다. 성공하면 true. */
    static boolean run(Component parent, String title, Runnable work) {
        return call(parent, title, () -> {
            work.run();
            return Boolean.TRUE;
        }) != null;
    }

    static <T> T call(Component parent, String title, Supplier<T> work) {
        try {
            return work.get();
        } catch (AppException e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), title, JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    static void needSelection(Component parent) {
        JOptionPane.showMessageDialog(parent, "표에서 하나를 먼저 골라 주세요.");
    }
}
