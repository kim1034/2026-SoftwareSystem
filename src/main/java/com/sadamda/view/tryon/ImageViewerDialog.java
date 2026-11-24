package com.sadamda.view.tryon;

import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

/**
 * 크게 보기 창: 그림 칸을 누르거나 입혀보기가 끝나면 열린다. 화면 거의 전체에 그림을 크게 보여준다.
 * [크게] [작게] [원래 크기] 버튼과 마우스 휠로 확대·축소하고, 확대했을 때는 끌어서 옮겨 본다. Esc 나 [닫기]로 닫는다.
 */
class ImageViewerDialog extends JDialog {
    private static final double MAX_ZOOM = 4.0;

    private final Viewer viewer;

    ImageViewerDialog(Window owner, BufferedImage image, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        viewer = new Viewer(image);

        JButton zoomIn = TryOnUi.secondary("크게 +");
        zoomIn.addActionListener(e -> viewer.zoomBy(1.4, null));
        JButton zoomOut = TryOnUi.secondary("작게 −");
        zoomOut.addActionListener(e -> viewer.zoomBy(1 / 1.4, null));
        JButton fit = TryOnUi.secondary("화면에 맞추기");
        fit.addActionListener(e -> viewer.reset());
        JButton close = TryOnUi.primary("닫기");
        close.addActionListener(e -> dispose());

        JLabel hint = Theme.label("마우스 휠로 확대·축소, 끌어서 옮기기", 0.95f, Font.PLAIN, Theme.HINT);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(zoomIn);
        buttons.add(zoomOut);
        buttons.add(fit);
        buttons.add(close);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.CARD);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        bar.add(hint, BorderLayout.WEST);
        bar.add(buttons, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.add(viewer, BorderLayout.CENTER);
        content.add(bar, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(close);

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize((int) (screen.width * 0.9), (int) (screen.height * 0.94));
        setLocationRelativeTo(owner);
    }

    /** 그림을 그리는 부분: 처음엔 창에 맞춰 보여주고, 확대하면 끌어서 옮긴다. */
    private static class Viewer extends JComponent {
        private final BufferedImage image;
        private double zoom = 1.0; // 창에 맞춘 크기의 몇 배
        private double offsetX;
        private double offsetY;
        private int dragX;
        private int dragY;

        Viewer(BufferedImage image) {
            this.image = image;
            setPreferredSize(new Dimension(800, 800));
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragX = e.getX();
                    dragY = e.getY();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    offsetX += e.getX() - dragX;
                    offsetY += e.getY() - dragY;
                    dragX = e.getX();
                    dragY = e.getY();
                    clamp();
                    repaint();
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    zoomBy(e.getWheelRotation() < 0 ? 1.2 : 1 / 1.2, e.getPoint());
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        if (zoom > 1.01) {
                            reset();
                        } else {
                            zoomBy(2.0, e.getPoint());
                        }
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        void reset() {
            zoom = 1.0;
            offsetX = 0;
            offsetY = 0;
            repaint();
        }

        /** 기준점(없으면 가운데)을 그대로 두고 확대·축소한다. 창에 맞춘 크기보다 작아지지 않는다. */
        void zoomBy(double factor, java.awt.Point at) {
            double next = Math.max(1.0, Math.min(MAX_ZOOM, zoom * factor));
            double px = at == null ? getWidth() / 2.0 : at.x;
            double py = at == null ? getHeight() / 2.0 : at.y;
            double cx = getWidth() / 2.0 + offsetX;
            double cy = getHeight() / 2.0 + offsetY;
            double ratio = next / zoom;
            offsetX = px - (px - cx) * ratio - getWidth() / 2.0;
            offsetY = py - (py - cy) * ratio - getHeight() / 2.0;
            zoom = next;
            clamp();
            repaint();
        }

        private double baseScale() {
            return Math.min((double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
        }

        /** 그림이 창 밖으로 다 빠져나가지 않게 옮길 수 있는 범위를 제한한다. */
        private void clamp() {
            double w = image.getWidth() * baseScale() * zoom;
            double h = image.getHeight() * baseScale() * zoom;
            double maxX = Math.max(0, (w - getWidth()) / 2);
            double maxY = Math.max(0, (h - getHeight()) / 2);
            offsetX = Math.max(-maxX, Math.min(maxX, offsetX));
            offsetY = Math.max(-maxY, Math.min(maxY, offsetY));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = TryOnUi.smooth(graphics);
            g.setColor(new Color(0x1A1A1A));
            g.fillRect(0, 0, getWidth(), getHeight());
            double scale = baseScale() * zoom;
            int w = (int) Math.round(image.getWidth() * scale);
            int h = (int) Math.round(image.getHeight() * scale);
            int x = (int) Math.round((getWidth() - w) / 2.0 + offsetX);
            int y = (int) Math.round((getHeight() - h) / 2.0 + offsetY);
            g.drawImage(image, x, y, w, h, null);
            g.dispose();
        }
    }
}
