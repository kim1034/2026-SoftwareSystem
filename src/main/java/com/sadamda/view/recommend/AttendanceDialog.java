package com.sadamda.view.recommend;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Product;
import com.sadamda.model.recommend.OutfitSuggestion;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ProductThumbnail;
import com.sadamda.view.Theme;
import com.sadamda.view.product.ShopUi;
import com.sadamda.view.tryon.TryOnPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;

/**
 * 옷장 출석의 그날의 코디 (UC-21 3·5단계): 상품 사진·고른 이유·합계와
 * [캐릭터에 입혀보기](옷 입혀보기로 이동) · [코디북에 저장](UC-19) · [닫기].
 */
class AttendanceDialog extends JDialog {
    AttendanceDialog(Window owner, AppContext context, MainFrame frame, OutfitSuggestion outfit, String message) {
        super(owner, "오늘의 코디", ModalityType.APPLICATION_MODAL);

        JPanel text = new JPanel(new GridLayout(0, 1, 0, 6));
        text.setOpaque(false);
        if (message != null) {
            for (String line : message.split("\n")) {
                text.add(ShopUi.label(line, 1.05f, Font.BOLD, ShopUi.TEXT));
            }
        }
        text.add(ShopUi.label(outfit.title(), 1.25f, Font.BOLD, ShopUi.TEXT));
        text.add(ShopUi.label(outfit.reason(), 1f, Font.PLAIN, ShopUi.HINT));
        text.add(ShopUi.label("옷 " + outfit.products().size() + "벌 · 합계 " + ShopUi.price(outfit.totalPrice()),
                1f, Font.PLAIN, ShopUi.TEXT));

        JPanel photos = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        photos.setOpaque(false);
        for (Product product : outfit.products()) {
            ProductThumbnail photo = new ProductThumbnail(product.mainImageUrl());
            photo.setToolTipText(product.getName());
            photos.add(photo);
        }

        JButton wear = ShopUi.primaryButton("캐릭터에 입혀보기");
        wear.addActionListener(e -> {
            dispose();
            frame.navigate(TryOnPanel.NAME);
            TryOnPanel tryOn = ShopUi.findScreen(frame, TryOnPanel.class);
            if (tryOn != null) {
                SwingUtilities.invokeLater(() -> tryOn.wearOutfit(outfit));
            }
        });
        JButton save = ShopUi.secondaryButton("코디북에 저장");
        save.addActionListener(e -> saveToCodiBook(context, outfit));
        JButton close = ShopUi.secondaryButton("닫기");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(wear);
        buttons.add(save);
        buttons.add(close);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(Theme.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        content.add(text, BorderLayout.NORTH);
        content.add(photos, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setMinimumSize(new Dimension(640, getHeight()));
        setLocationRelativeTo(owner);
    }

    /** UC-19 그날의 코디를 그대로 코디북에 모아 둔다. 캐릭터는 회원 체형으로 시작한다. */
    private void saveToCodiBook(AppContext context, OutfitSuggestion suggestion) {
        Outfit outfit = new Outfit();
        for (Product product : suggestion.products()) {
            for (DressUpSlot slot : DressUpSlot.values()) {
                if (slot.category() == product.getCategory()) {
                    outfit.put(slot, product);
                }
            }
        }
        String title = suggestion.title().replace(" 오늘의 코디 · ", " ");
        if (title.length() > 30) {
            title = title.substring(0, 30);
        }
        try {
            context.codiBook.save(title, CharacterStyle.REAL, context.dressUp.initialSpec(), outfit, null);
            JOptionPane.showMessageDialog(this, "'" + title + "'를 코디북에 저장했어요.\n옷 입혀보기의 [내 코디북]에서 볼 수 있어요.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "코디북에 저장하지 못했어요.\n" + e.getMessage());
        }
    }
}
