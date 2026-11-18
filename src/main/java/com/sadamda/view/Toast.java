package com.sadamda.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * 화면을 막지 않고 잠깐 떴다 사라지는 알림 (쇼핑몰의 "장바구니에 담았어요 · 장바구니 보기" 모양).
 * 확인 창처럼 [확인]을 누르지 않아도 되고, 원하면 오른쪽 링크로 바로 이동한다.
 * 어르신도 읽을 수 있게 5초 동안 보여 주고, 마우스를 올려 두면 사라지지 않는다.
 */
public final class Toast {
    private static final int SHOW_MILLIS = 5000;
    private static JComponent current;
    private static Timer timer;

    private Toast() {
    }

    /** message 만 보여 준다. */
    public static void show(JComponent anchor, String message) {
        show(anchor, message, null, null);
    }

    /** message 와 오른쪽 링크 [actionLabel]. 링크를 누르면 action 을 하고 알림을 닫는다. */
    public static void show(JComponent anchor, String message, String actionLabel, Runnable action) {
        JRootPane root = SwingUtilities.getRootPane(anchor);
        if (root == null) {
            return;
        }
        hide();
        JLayeredPane layers = root.getLayeredPane();

        JPanel toast = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
            }
        };
        toast.setOpaque(false);
        toast.setBorder(BorderFactory.createEmptyBorder(16, 28, 16, 28));
        Font base = UIManager.getFont("Label.font");
        JLabel text = new JLabel(message);
        text.setForeground(Color.WHITE);
        text.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 1));
        toast.add(text);
        if (actionLabel != null && action != null) {
            JButton link = new JButton("<html><u>" + actionLabel + "</u></html>");
            link.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 1));
            link.setForeground(Color.WHITE);
            link.setContentAreaFilled(false);
            link.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.addActionListener(e -> {
                hide();
                action.run();
            });
            toast.add(link);
        }
        toast.getAccessibleContext().setAccessibleName(message);

        Dimension size = toast.getPreferredSize();
        int x = (layers.getWidth() - size.width) / 2;
        int y = layers.getHeight() - size.height - Math.round(base.getSize2D() * 7); // 아래 고정 버튼 줄 위
        toast.setBounds(Math.max(16, x), Math.max(16, y), size.width, size.height);
        layers.add(toast, JLayeredPane.POPUP_LAYER);
        layers.revalidate();
        layers.repaint();
        current = toast;

        timer = new Timer(SHOW_MILLIS, e -> {
            if (toast.getMousePosition(true) != null) {
                timer.restart(); // 읽는 중이면 기다린다
            } else {
                hide();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public static void hide() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (current != null && current.getParent() != null) {
            JComponent parent = (JComponent) current.getParent();
            parent.remove(current);
            parent.repaint();
        }
        current = null;
    }
}
