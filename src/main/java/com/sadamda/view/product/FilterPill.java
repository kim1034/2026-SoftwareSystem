package com.sadamda.view.product;

import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 상품 목록의 필터 버튼 [연령대 ∨]. 누르면 선택지 목록이 열리고, 고르면 버튼 글자가 고른 값으로 바뀐다.
 * 첫 선택지(0번)는 "전체"(조건 없음)이다. 조건을 고른 버튼은 검정 바탕으로 눈에 띄게 한다.
 */
final class FilterPill extends JButton {
    private static final Color FILL = new Color(0xF1F1F1);

    private final String name;
    private final String[] options;
    private int selected;
    private Runnable onChange = () -> { };

    /** options[0] 은 "전체"로 보인다. */
    FilterPill(String name, String[] options) {
        this.name = name;
        this.options = options.clone();
        this.options[0] = "전체";
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 44)); // 오른쪽은 ∨ 자리. 높이 48px 이상 (NFR-02)
        addActionListener(e -> showMenu());
        refreshText();
    }

    @Override
    public void updateUI() {
        setUI(new BasicButtonUI());
        Font base = UIManager.getFont("Button.font");
        setFont(base.deriveFont(Font.PLAIN, base.getSize2D()));
    }

    void onChange(Runnable action) {
        onChange = action;
    }

    int selectedIndex() {
        return selected;
    }

    /** 고른 값. "전체"면 null. */
    String value() {
        return selected == 0 ? null : options[selected];
    }

    /** 코드에서 고른다. onChange 는 부르지 않는다. */
    void setSelectedIndex(int index) {
        selected = index;
        refreshText();
    }

    /** 선택지에 있는 값이면 고르고, 없으면 "전체"로 둔다. onChange 는 부르지 않는다. */
    void select(String value) {
        int index = 0;
        for (int i = 1; i < options.length; i++) {
            if (options[i].equals(value)) {
                index = i;
            }
        }
        setSelectedIndex(index);
    }

    private void showMenu() {
        JPopupMenu menu = new JPopupMenu();
        for (int i = 0; i < options.length; i++) {
            int index = i;
            JMenuItem item = new JMenuItem(options[i]);
            item.setFont(getFont().deriveFont(i == selected ? Font.BOLD : Font.PLAIN));
            item.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 24));
            item.addActionListener(e -> {
                setSelectedIndex(index);
                onChange.run();
            });
            menu.add(item);
        }
        menu.show(this, 0, getHeight() + 4);
    }

    private void refreshText() {
        setText(selected == 0 ? name : options[selected]);
        setForeground(selected == 0 ? Theme.TEXT : Color.WHITE);
        getAccessibleContext().setAccessibleName(name + " 필터, 지금 " + options[selected]);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(selected == 0 ? FILL : Theme.PRIMARY);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
        // ∨ 표시
        float size = getFont().getSize2D() * 0.35f;
        float cx = getWidth() - 26;
        float cy = getHeight() / 2f;
        Path2D chevron = new Path2D.Float();
        chevron.moveTo(cx - size, cy - size / 2);
        chevron.lineTo(cx, cy + size / 2);
        chevron.lineTo(cx + size, cy - size / 2);
        g2.setColor(getForeground());
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(chevron);
        if (isFocusOwner()) { // 키보드 사용자를 위한 테두리
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(3, 3, getWidth() - 7, getHeight() - 7, getHeight() - 7, getHeight() - 7));
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
