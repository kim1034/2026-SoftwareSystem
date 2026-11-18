package com.sadamda.view.product;

import com.sadamda.view.ProductImages;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 상품 사진 한 장을 연한 회색 칸 안에 잘리지 않게(전체가 보이게) 그린다. 상세 화면의 큰 사진과 리뷰 사진에 쓴다.
 * 누르면 사진을 크게 보는 창을 연다 (UC-06 3단계).
 */
class ProductPhoto extends JButton {
    private static final float RATIO = 4f / 3; // 세로 ÷ 가로
    private static final int ARC = 16;

    private final List<String> urls;
    private int index;
    private int width;

    /** urls 중 index 번째를 보여준다. 크게 보는 창에서는 같은 목록을 앞뒤로 넘겨 볼 수 있다. */
    ProductPhoto(List<String> urls, int index, int width) {
        this.urls = urls;
        this.index = index;
        this.width = width;
        getAccessibleContext().setAccessibleName("상품 사진, 누르면 크게 보기");
        setToolTipText("누르면 크게 볼 수 있어요");
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addActionListener(e -> openZoom(this, urls, this.index));
        requestImage();
    }

    @Override
    public void updateUI() {
        setUI(new BasicButtonUI());
    }

    void setPhotoWidth(int width) {
        this.width = width;
        revalidate();
    }

    void showImage(int index) {
        this.index = index;
        requestImage();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, Math.round(width * RATIO));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private void requestImage() {
        ProductImages.request(url(), this::repaint);
    }

    private String url() {
        return urls.isEmpty() ? null : urls.get(index);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        paintPhoto(g2, url(), getWidth(), getHeight(), true);
        if (isFocusOwner()) {
            g2.setColor(Theme.TEXT);
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 3, getHeight() - 3, ARC, ARC));
        }
        g2.dispose();
    }

    /** 사진 전체가 보이게 가운데 맞춰 그린다. 아직 없으면 안내 문구. */
    static void paintPhoto(Graphics2D g2, String url, int width, int height, boolean roundedBox) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        Shape box = roundedBox
                ? new RoundRectangle2D.Float(0, 0, width, height, ARC, ARC)
                : new java.awt.Rectangle(0, 0, width, height);
        g2.setColor(Theme.PAGE);
        g2.fill(box);

        BufferedImage image = ProductImages.get(url);
        if (image != null) {
            double scale = Math.min(width / (double) image.getWidth(), height / (double) image.getHeight());
            int drawWidth = (int) Math.round(image.getWidth() * scale);
            int drawHeight = (int) Math.round(image.getHeight() * scale);
            Graphics2D clipped = (Graphics2D) g2.create();
            clipped.clip(box);
            clipped.drawImage(image, (width - drawWidth) / 2, (height - drawHeight) / 2, drawWidth, drawHeight, null);
            clipped.dispose();
            return;
        }
        String message = ProductImages.failed(url) ? "사진 없음" : "사진을 불러오는 중";
        g2.setColor(Theme.HINT);
        g2.setFont(UIManager.getFont("Label.font"));
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(message, (width - metrics.stringWidth(message)) / 2,
                (height - metrics.getHeight()) / 2 + metrics.getAscent());
    }

    /** UC-06 3단계: 사진을 창 크기에 맞춰 크게 보여준다. [닫기] 또는 Esc 로 닫는다. 여러 장이면 앞뒤로 넘긴다. */
    static void openZoom(JComponent from, List<String> urls, int start) {
        Window owner = SwingUtilities.getWindowAncestor(from);
        JDialog dialog = new JDialog(owner, "사진 크게 보기", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        int[] current = {start};

        JComponent photo = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                paintPhoto(g2, urls.get(current[0]), getWidth(), getHeight(), false);
                g2.dispose();
            }
        };
        JLabel counter = Theme.label(" ", 1f, java.awt.Font.PLAIN, Theme.HINT);
        Runnable refresh = () -> {
            ProductImages.request(urls.get(current[0]), photo::repaint);
            counter.setText((current[0] + 1) + " / " + urls.size());
            photo.repaint();
        };

        JButton close = Theme.primaryButton("닫기");
        close.addActionListener(e -> dialog.dispose());
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.CARD);
        top.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        top.add(Theme.label("사진 크게 보기", 1.2f, java.awt.Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        top.add(close, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.CARD);
        content.add(top, BorderLayout.NORTH);
        content.add(photo, BorderLayout.CENTER);
        if (urls.size() > 1) {
            JButton previous = Theme.secondaryButton("이전 사진");
            JButton next = Theme.secondaryButton("다음 사진");
            previous.addActionListener(e -> {
                current[0] = (current[0] - 1 + urls.size()) % urls.size();
                refresh.run();
            });
            next.addActionListener(e -> {
                current[0] = (current[0] + 1) % urls.size();
                refresh.run();
            });
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 16));
            bottom.setBackground(Theme.CARD);
            bottom.add(previous);
            bottom.add(counter);
            bottom.add(next);
            content.add(bottom, BorderLayout.SOUTH);
        }
        dialog.setContentPane(content);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        refresh.run();
        if (owner != null) {
            dialog.setSize((int) (owner.getWidth() * 0.85), (int) (owner.getHeight() * 0.9));
        } else {
            dialog.setSize(900, 800);
        }
        dialog.setLocationRelativeTo(owner);
        close.requestFocusInWindow();
        dialog.setVisible(true);
    }
}
