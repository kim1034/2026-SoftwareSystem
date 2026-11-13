package com.sadamda.view.tryon;

import com.sadamda.controller.TryOnController;
import com.sadamda.model.tryon.TryOnImage;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 내 보관함 (FR-16): 보관함에 저장한 합성 이미지를 보고, 고른 이미지를 지운다 (NFR-08: 사용자가 언제든 삭제 가능).
 * 지운 이미지는 이 PC 의 보관함 폴더에서도 사라지며 되돌릴 수 없다.
 */
class LockerDialog extends JDialog {
    private static final int THUMBNAIL = 120;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TryOnController tryOn;
    private final DefaultListModel<TryOnImage> model = new DefaultListModel<>();
    private final JList<TryOnImage> list = new JList<>(model);
    private final Map<Long, ImageIcon> icons = new HashMap<>();
    private final JLabel summary = new JLabel(" ");
    private final JButton deleteButton = bigButton("지우기");

    LockerDialog(Window owner, TryOnController tryOn) {
        super(owner, "내 보관함", ModalityType.APPLICATION_MODAL);
        this.tryOn = tryOn;

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ImageCell());
        list.addListSelectionListener(e -> deleteButton.setEnabled(list.getSelectedValue() != null));

        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelected());
        JButton closeButton = bigButton("닫기");
        closeButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(deleteButton);
        buttons.add(closeButton);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.add(summary, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        content.add(new JScrollPane(list), BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(new Dimension(560, 640));
        setLocationRelativeTo(owner);

        reload();
    }

    private void reload() {
        model.clear();
        List<TryOnImage> images = tryOn.locker();
        images.forEach(model::addElement);
        summary.setText(images.isEmpty() ? "보관함이 비어 있습니다." : "저장한 그림 " + images.size() + "개");
        deleteButton.setEnabled(false);
    }

    private void deleteSelected() {
        TryOnImage target = list.getSelectedValue();
        if (target == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "고른 그림을 보관함에서 지울까요?\n지운 그림은 되돌릴 수 없습니다.",
                "보관함에서 지우기", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            tryOn.deleteFromLocker(target.id());
            icons.remove(target.id());
            reload();
            summary.setText("지웠습니다. " + summary.getText());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "그림을 지우지 못했습니다.\n" + e.getMessage());
            reload();
        }
    }

    private ImageIcon iconFor(TryOnImage image) {
        return icons.computeIfAbsent(image.id(), id -> new ImageIcon(thumbnail(new File(image.imagePath()))));
    }

    /** 보관함 파일을 정사각형 작은 그림으로 만든다. 열 수 없으면 회색 칸. */
    private static BufferedImage thumbnail(File file) {
        BufferedImage target = new BufferedImage(THUMBNAIL, THUMBNAIL, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setColor(new Color(235, 235, 235));
        g.fillRect(0, 0, THUMBNAIL, THUMBNAIL);
        try {
            BufferedImage source = ImageIO.read(file);
            if (source != null) {
                double scale = Math.min((double) THUMBNAIL / source.getWidth(), (double) THUMBNAIL / source.getHeight());
                int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
                int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, THUMBNAIL, THUMBNAIL);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source, (THUMBNAIL - width) / 2, (THUMBNAIL - height) / 2, width, height, null);
            }
        } catch (IOException ignored) {
            // 회색 칸으로 둔다
        }
        g.dispose();
        return target;
    }

    private class ImageCell extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, "", index, selected, focused);
            TryOnImage image = (TryOnImage) value;
            label.setIcon(iconFor(image));
            label.setText("<html>" + image.createdAt().format(WHEN) + " 저장</html>");
            label.setIconTextGap(16);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            return label;
        }
    }

    private static JButton bigButton(String text) {
        JButton button = new JButton(text);
        button.setMinimumSize(new Dimension(48, 48)); // NFR-02
        button.setMargin(new Insets(10, 14, 10, 14));
        return button;
    }
}
