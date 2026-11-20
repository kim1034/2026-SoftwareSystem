package com.sadamda.view.tryon;

import com.sadamda.controller.CodiBookController;
import com.sadamda.model.product.Product;
import com.sadamda.model.tryon.CodiBookService;
import com.sadamda.model.tryon.SavedCodi;
import com.sadamda.view.Theme;

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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 내 코디북 (UC-19): 저장한 코디를 보고, 골라서 [다시 입혀보기]·[이름 바꾸기]·[지우기]를 한다.
 * [다시 입혀보기]를 누르면 창을 닫고 chosen() 으로 고른 코디를 알려 준다. 옷 입혀보기 화면이 그 코디를 되살린다.
 */
class CodiBookDialog extends JDialog {
    private static final int THUMBNAIL = 120;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CodiBookController codiBook;
    private final DefaultListModel<SavedCodi> model = new DefaultListModel<>();
    private final JList<SavedCodi> list = new JList<>(model);
    private final Map<Long, ImageIcon> imageIcons = new HashMap<>();
    private final JLabel summary = Theme.label(" ", 1f, Font.PLAIN, Theme.TEXT);
    private final JButton openButton = compact(Theme.primaryButton("다시 입혀보기"));
    private final JButton renameButton = compact(Theme.secondaryButton("이름 바꾸기"));
    private final JButton deleteButton = compact(Theme.secondaryButton("지우기"));
    private SavedCodi chosen;

    CodiBookDialog(Window owner, CodiBookController codiBook) {
        super(owner, "내 코디북", ModalityType.APPLICATION_MODAL);
        this.codiBook = codiBook;

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new CodiCell());
        list.addListSelectionListener(e -> updateButtons());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
                    choose();
                }
            }
        });

        openButton.addActionListener(e -> choose());
        renameButton.addActionListener(e -> renameSelected());
        deleteButton.addActionListener(e -> deleteSelected());
        JButton closeButton = compact(Theme.secondaryButton("닫기"));
        closeButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(openButton);
        buttons.add(renameButton);
        buttons.add(deleteButton);
        buttons.add(closeButton);

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(summary, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(Theme.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        content.add(Theme.label("저장한 코디를 골라 다시 입혀 보거나, 한 칸만 바꿔 새 코디를 만들어 보세요.",
                1f, Font.PLAIN, Theme.HINT), BorderLayout.NORTH);
        content.add(new JScrollPane(list), BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(new Dimension(760, 680));
        setLocationRelativeTo(owner);

        reload(null);
    }

    /** [다시 입혀보기]로 고른 코디. 닫기만 했으면 null. */
    SavedCodi chosen() {
        return chosen;
    }

    private void reload(Long selectId) {
        model.clear();
        List<SavedCodi> codis = codiBook.list();
        codis.forEach(model::addElement);
        summary.setText(codis.isEmpty()
                ? "코디북이 비어 있습니다. 옷을 입혀 본 뒤 [코디북에 저장]을 눌러 보세요."
                : "모아 둔 코디 " + codis.size() + "개");
        for (int i = 0; i < codis.size(); i++) {
            if (selectId != null && codis.get(i).id() == selectId) {
                list.setSelectedIndex(i);
            }
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean selected = list.getSelectedValue() != null;
        openButton.setEnabled(selected);
        renameButton.setEnabled(selected);
        deleteButton.setEnabled(selected);
    }

    private void choose() {
        chosen = list.getSelectedValue();
        if (chosen != null) {
            dispose();
        }
    }

    private void renameSelected() {
        SavedCodi target = list.getSelectedValue();
        if (target == null) {
            return;
        }
        Object answer = JOptionPane.showInputDialog(this,
                "새 이름을 넣어 주세요. (" + CodiBookService.MAX_TITLE + "자까지)", "이름 바꾸기",
                JOptionPane.PLAIN_MESSAGE, null, null, target.title());
        if (answer == null) {
            return;
        }
        try {
            codiBook.rename(target.id(), answer.toString());
            reload(target.id());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "이름을 바꾸지 못했습니다.\n" + e.getMessage());
        }
    }

    private void deleteSelected() {
        SavedCodi target = list.getSelectedValue();
        if (target == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "'" + target.title() + "' 코디를 지울까요?\n지운 코디는 되돌릴 수 없습니다.",
                "코디 지우기", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            codiBook.delete(target.id());
            imageIcons.remove(target.id());
            reload(null);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "코디를 지우지 못했습니다.\n" + e.getMessage());
            reload(null);
        }
    }

    /** 입혀본 그림이 있으면 그 그림, 없으면 첫 상품 사진. */
    private ImageIcon iconFor(SavedCodi codi, List<Product> products) {
        if (codi.hasImage()) {
            return imageIcons.computeIfAbsent(codi.id(), id -> new ImageIcon(fileThumbnail(new File(codi.imagePath()))));
        }
        String url = products.isEmpty() ? null : products.get(0).mainImageUrl();
        return Thumbnails.get(url, THUMBNAIL, list::repaint);
    }

    private static BufferedImage fileThumbnail(File file) {
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

    private class CodiCell extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, "", index, selected, focused);
            SavedCodi codi = (SavedCodi) value;
            List<Product> products = codiBook.products(codi);
            int total = products.stream().mapToInt(Product::getPrice).sum();
            label.setIcon(iconFor(codi, products));
            label.setText("<html><b>" + ProductPickerDialog.escape(codi.title()) + "</b><br>"
                    + "옷 " + products.size() + "벌 · 합계 " + String.format("%,d원", total) + "<br>"
                    + codi.savedAt().format(WHEN) + " 저장"
                    + (codi.hasImage() ? "" : " · 아직 입혀보지 않음") + "</html>");
            label.setIconTextGap(16);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            return label;
        }
    }

    /** 대화상자 폭에 맞게 좌우 여백만 좁힌다. 높이는 48px 그대로 (NFR-02). */
    private static JButton compact(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        return button;
    }
}
