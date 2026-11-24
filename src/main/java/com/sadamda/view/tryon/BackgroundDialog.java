package com.sadamda.view.tryon;

import com.sadamda.model.tryon.Background;
import com.sadamda.model.tryon.BackgroundCatalog;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/** [배경 모두 보기]: 배경 전체를 계절별로 크게 보여주고 하나를 고른다. */
class BackgroundDialog extends JDialog {
    private Background chosen;
    private boolean picked;

    /**
     * @param current 지금 고른 배경 (원래 배경이면 null)
     * @param photo   "원래 배경" 버튼에 보여줄 올린 사진
     */
    BackgroundDialog(Window owner, List<Background> backgrounds, Background current, BufferedImage photo) {
        super(owner, "배경 고르기", ModalityType.APPLICATION_MODAL);
        chosen = current;

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.CARD);
        list.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        BackgroundTile keep = tile(null, current == null);
        keep.setPhoto(photo);
        JPanel keepRow = new JPanel(new GridLayout(1, 5, 12, 0));
        keepRow.setOpaque(false);
        keepRow.add(keep);
        for (int i = 0; i < 4; i++) {
            keepRow.add(spacer());
        }
        addRow(list, "올린 사진의 배경", keepRow);

        for (String season : BackgroundCatalog.SEASONS) {
            JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
            row.setOpaque(false);
            int count = 0;
            for (Background background : backgrounds) {
                if (background.season().equals(season)) {
                    row.add(tile(background, background.equals(current)));
                    count++;
                }
            }
            if (count == 0) {
                continue;
            }
            for (int i = count; i < 5; i++) {
                row.add(spacer());
            }
            addRow(list, season, row);
        }

        JScrollPane scroll = ModernScrollBar.apply(new JScrollPane(list));
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        JButton close = TryOnUi.secondary("닫기");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttons.setBackground(Theme.CARD);
        buttons.add(close);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.CARD);
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(1000, 860);
        setLocationRelativeTo(owner);
    }

    /** 배경을 골랐는지 (닫기만 했으면 false). */
    boolean picked() {
        return picked;
    }

    /** 고른 배경. 원래 배경이면 null. */
    Background chosen() {
        return chosen;
    }

    private BackgroundTile tile(Background background, boolean selected) {
        BackgroundTile tile = new BackgroundTile(background);
        tile.setPreferredSize(new Dimension(170, 190));
        tile.setChosen(selected);
        tile.addActionListener(e -> {
            chosen = background;
            picked = true;
            dispose();
        });
        return tile;
    }

    private static Component spacer() {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        return spacer;
    }

    private static void addRow(JPanel list, String title, JPanel row) {
        JPanel block = new JPanel(new BorderLayout(0, 8));
        block.setOpaque(false);
        block.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));
        block.add(Theme.label(title, 1.15f, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        block.add(row, BorderLayout.CENTER);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, block.getPreferredSize().height + 40));
        list.add(block);
    }
}
