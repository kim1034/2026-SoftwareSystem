package com.sadamda.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 화면 공통 디자인: 색, 둥근 입력칸·버튼, 카드. 담당: 강준서
 * 글자 색은 배경 대비 4.5:1 이상 (NFR-01), 버튼은 높이 48px 이상 (NFR-02).
 * 글씨 크기 조절(UC-15) 때 updateComponentTreeUI 가 UI 를 다시 깔기 때문에, 모양은 updateUI 에서 지정한다.
 */
public final class Theme {
    public static final Color PAGE = new Color(0xE8F0FA);      // 화면 배경: 연한 하늘색 (PRIMARY 계열)
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(0x1F2937);      // 흰 배경 대비 14.7:1
    public static final Color HINT = new Color(0x6B7280);      // 흰 배경 대비 4.8:1
    public static final Color LINE = new Color(0xD1D5DB);      // 테두리 (글자 아님)
    public static final Color PRIMARY = new Color(0x1565C0);   // 흰 글씨 대비 5.75:1
    public static final Color SECONDARY = new Color(0xF2C94C); // TEXT 글씨 대비 9.25:1
    public static final Color ERROR = new Color(0xC62828);     // 흰 배경 대비 5.6:1

    private static final float ARC = 14;
    private static final String INVALID = "Theme.invalid";

    private Theme() {
    }

    /** 기본 글씨 크기의 scale 배 크기 라벨. 글씨 크기를 바꾸면 함께 바뀐다. */
    public static JLabel label(String text, float scale, int style, Color color) {
        JLabel label = new JLabel(text) {
            @Override
            public void updateUI() {
                super.updateUI();
                Font base = UIManager.getFont("Label.font");
                setFont(base.deriveFont(style, base.getSize2D() * scale));
            }
        };
        label.setForeground(color);
        return label;
    }

    /** 입력 오류 문구. 오류가 없으면 빈 문자열이라 보이지 않는다. */
    public static JLabel errorLabel() {
        return label("", 1f, Font.PLAIN, ERROR);
    }

    /** 흰 바탕에 둥근 모서리를 그리는 카드. 내용은 addToCard 로 위에서 아래로 쌓는다. */
    public static JPanel card() {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.setColor(CARD);
                g2.fill(shape);
                g2.setColor(LINE);
                g2.draw(shape);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(40, 48, 40, 48));
        return card;
    }

    /** 카드 맨 아래에 한 줄을 추가한다. 카드 폭만큼 늘어난다. */
    public static void addToCard(JPanel card, JComponent component, int topGap) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE; // 이전 줄 바로 아래
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(topGap, 0, 0, 0);
        card.add(component, c);
    }

    /**
     * 하늘색 배경 가운데 위쪽에 카드를 둔 화면 내용.
     * 글씨를 키우거나 창이 작아도 모든 내용에 닿을 수 있게 스크롤을 둔다.
     */
    public static JScrollPane centeredPage(JPanel card) {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(PAGE);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.weighty = 1; // 남는 공간은 아래로
        c.insets = new Insets(16, 16, 16, 16);
        page.add(card, c);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /** [이름표  오류 문구] 아래에 입력란. 오류 문구가 필요 없으면 error 는 null. */
    public static JPanel field(String label, JLabel error, JComponent input) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        header.add(label(label, 1f, Font.BOLD, TEXT));
        if (error != null) {
            header.add(Box.createHorizontalStrut(16));
            header.add(error);
        }

        JPanel block = new JPanel(new BorderLayout(0, 8));
        block.setOpaque(false);
        block.add(header, BorderLayout.NORTH);
        block.add(input, BorderLayout.CENTER);
        return block;
    }

    /** 비어 있을 때 회색 안내 문구(hint)를 보여주는 둥근 입력칸. */
    public static JTextField textField(String hint) {
        JTextField field = new JTextField(20) {
            @Override
            public void updateUI() {
                super.updateUI();
                styleInput(this);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintHint(this, g, hint);
            }
        };
        repaintOnFocus(field);
        return field;
    }

    public static JPasswordField passwordField(String hint) {
        JPasswordField field = new JPasswordField(20) {
            @Override
            public void updateUI() {
                super.updateUI();
                styleInput(this);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintHint(this, g, hint);
            }
        };
        repaintOnFocus(field);
        return field;
    }

    /** macOS 기본 콤보박스는 글씨가 커져도 높이가 그대로라 글자가 잘려서, 기본 Swing 모양을 쓴다. */
    public static JComboBox<String> comboBox(String... items) {
        JComboBox<String> box = new JComboBox<>(items) {
            @Override
            public void updateUI() {
                setUI(new BasicComboBoxUI() {
                    @Override
                    protected JButton createArrowButton() {
                        JButton arrow = new BasicArrowButton(SwingConstants.SOUTH, CARD, CARD, HINT, CARD);
                        arrow.setBorder(BorderFactory.createEmptyBorder());
                        return arrow;
                    }
                });
                setBorder(new RoundBorder());
                setBackground(CARD);
                setForeground(TEXT);
            }
        };
        repaintOnFocus(box);
        return box;
    }

    /** 주 버튼: 파란 바탕에 흰 글씨. */
    public static JButton primaryButton(String text) {
        return button(text, PRIMARY, Color.WHITE);
    }

    /** 보조 버튼: 노란 바탕에 진한 글씨. (흰 글씨는 노란 바탕에서 대비가 부족하다) */
    public static JButton secondaryButton(String text) {
        return button(text, SECONDARY, TEXT);
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text) {
            @Override
            public void updateUI() {
                setUI(new BasicButtonUI());
                // 버튼 글씨는 본문보다 2px 크게: 보통 18px → 20px (NFR-01)
                Font base = UIManager.getFont("Button.font");
                setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 2));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(getModel().isPressed() ? background.darker() : background);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                g2.dispose();
                super.paintComponent(g);
                paintFocusRing(this, g);
            }
        };
        button.setForeground(foreground);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(14, 48, 14, 48)); // 높이 48px 이상 (NFR-02)
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** 글자만 있는 파란 링크 버튼 (예: 비밀번호 찾기). 누르는 영역은 48px 이상 (NFR-02). */
    public static JButton linkButton(String text) {
        JButton button = new JButton(text) {
            @Override
            public void updateUI() {
                setUI(new BasicButtonUI());
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintFocusRing(this, g);
            }
        };
        button.setForeground(PRIMARY);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** 글씨 크기에 맞춰 커지는 체크박스. macOS 기본 체크박스는 너무 작다 (NFR-02). */
    public static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(text) {
            @Override
            public void updateUI() {
                setUI(new BasicCheckBoxUI());
            }
        };
        box.setIcon(new CheckIcon(box, false));
        box.setSelectedIcon(new CheckIcon(box, true));
        box.setIconTextGap(12);
        box.setOpaque(false);
        box.setForeground(TEXT);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return box;
    }

    /**
     * 비밀번호 칸 오른쪽에 [보기]/[숨기기] 버튼을 붙인다. 입력한 글자를 확인해 오타를 줄인다.
     * 두 버튼을 CardLayout 으로 겹쳐 두어, 글자가 바뀌어도 버튼 폭(=입력칸 폭)이 흔들리지 않는다.
     */
    public static JPanel passwordWithToggle(JPasswordField field) {
        char hiddenEchoChar = field.getEchoChar();
        CardLayout toggleCards = new CardLayout();
        JPanel toggle = new JPanel(toggleCards);
        toggle.setOpaque(false);

        JButton showButton = linkButton("보기");
        JButton hideButton = linkButton("숨기기");
        showButton.addActionListener(e -> {
            field.setEchoChar((char) 0);
            toggleCards.show(toggle, "hide");
            hideButton.requestFocusInWindow(); // 키보드로 누른 경우 포커스를 이어 준다.
        });
        hideButton.addActionListener(e -> {
            field.setEchoChar(hiddenEchoChar);
            toggleCards.show(toggle, "show");
            showButton.requestFocusInWindow();
        });
        toggle.add(showButton, "show");
        toggle.add(hideButton, "hide");

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(field, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);
        return row;
    }

    /**
     * 입력칸을 "틀림" 상태로 표시하거나 해제한다. 틀린 칸은 테두리가 빨갛게 된다.
     * 오류 문구(errorLabel)와 함께 쓴다. 색만으로 알리지 않기 위해서다.
     */
    public static void setInvalid(JComponent input, boolean invalid) {
        input.putClientProperty(INVALID, invalid);
        input.repaint();
    }

    /**
     * 키보드로 포커스가 왔을 때 버튼 안쪽에 글자색 테두리를 그린다.
     * 글자색은 배경 대비 4.5:1 이상이라 테두리도 잘 보인다.
     */
    private static void paintFocusRing(JButton button, Graphics g) {
        if (!button.isFocusOwner()) {
            return;
        }
        Graphics2D g2 = smooth(g);
        g2.setColor(button.getForeground());
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new RoundRectangle2D.Float(3, 3, button.getWidth() - 7, button.getHeight() - 7, ARC - 4, ARC - 4));
        g2.dispose();
    }

    private static void styleInput(JTextComponent field) {
        field.setBorder(new RoundBorder());
        field.setBackground(CARD);
        field.setForeground(TEXT);
    }

    /** 테두리 색이 포커스에 따라 바뀌므로 포커스가 바뀌면 다시 그린다. */
    private static void repaintOnFocus(Component c) {
        c.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                c.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                c.repaint();
            }
        });
    }

    private static void paintHint(JTextComponent field, Graphics g, String hint) {
        if (field.getDocument().getLength() > 0) {
            return;
        }
        Graphics2D g2 = smooth(g);
        g2.setColor(HINT);
        g2.setFont(field.getFont());
        Insets in = field.getInsets();
        FontMetrics fm = g2.getFontMetrics();
        int y = in.top + (field.getHeight() - in.top - in.bottom - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(hint, in.left, y);
        g2.dispose();
    }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** 둥근 테두리. 틀린 칸이면 빨간색, 포커스가 있으면 파란색으로 굵게 그린다. */
    private static final class RoundBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = smooth(g);
            boolean invalid = c instanceof JComponent jc && Boolean.TRUE.equals(jc.getClientProperty(INVALID));
            boolean focused = c.hasFocus();
            g2.setColor(invalid ? ERROR : focused ? PRIMARY : LINE);
            g2.setStroke(new BasicStroke(invalid || focused ? 2f : 1.2f));
            g2.draw(new RoundRectangle2D.Float(x + 1, y + 1, width - 2, height - 2, ARC, ARC));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(12, 16, 12, 16);
        }
    }

    /** 체크박스 네모. 크기는 글씨 크기를 따른다. */
    private record CheckIcon(JCheckBox box, boolean checked) implements Icon {
        @Override
        public int getIconWidth() {
            return Math.round(box.getFont().getSize2D() * 1.3f);
        }

        @Override
        public int getIconHeight() {
            return getIconWidth();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int s = getIconWidth();
            Graphics2D g2 = smooth(g);
            RoundRectangle2D square = new RoundRectangle2D.Float(x + 1, y + 1, s - 2, s - 2, 8, 8);
            if (checked) {
                g2.setColor(PRIMARY);
                g2.fill(square);
                Path2D check = new Path2D.Float();
                check.moveTo(x + s * 0.25, y + s * 0.52);
                check.lineTo(x + s * 0.43, y + s * 0.70);
                check.lineTo(x + s * 0.76, y + s * 0.32);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(s / 9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(check);
            } else {
                g2.setColor(CARD);
                g2.fill(square);
                g2.setColor(HINT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(square);
            }
            g2.dispose();
        }
    }
}
