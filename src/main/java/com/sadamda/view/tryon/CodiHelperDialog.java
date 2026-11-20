package com.sadamda.view.tryon;

import com.sadamda.controller.RecommendController;
import com.sadamda.model.product.Product;
import com.sadamda.model.recommend.OutfitSuggestion;
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.view.Theme;
import com.sadamda.view.WrapLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

/**
 * 상황별 코디 도우미 (UC-20): "어디 가세요?" → 상황·계절을 고르면 체형에 맞는 코디 3벌을 보여준다.
 * [이 코디 입혀보기]를 누르면 창을 닫고 chosen() 으로 알려 준다. 옷 입혀보기 화면이 그 옷을 칸에 넣고 입힌다.
 * 체형·연령대는 옷 입혀보기 캐릭터의 신체 스펙을 쓴다 (회원이면 처음엔 등록한 체형).
 */
class CodiHelperDialog extends JDialog {
    private static final int THUMBNAIL = 96;

    private final RecommendController recommend;
    private final CharacterSpec spec;
    private final JPanel results = new JPanel();
    private String occasion;
    private String season;
    private OutfitSuggestion chosen;

    CodiHelperDialog(Window owner, RecommendController recommend, CharacterSpec spec, String occasion) {
        super(owner, "코디 추천받기", ModalityType.APPLICATION_MODAL);
        this.recommend = recommend;
        this.spec = spec;
        this.occasion = recommend.helperOccasions().contains(occasion) ? occasion : recommend.helperOccasions().get(0);
        this.season = recommend.currentSeason();

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(left(Theme.label("어디 가세요?", 1.3f, Font.BOLD, Theme.TEXT)));
        top.add(left(chips(recommend.helperOccasions(), this.occasion, value -> {
            this.occasion = value;
            refresh();
        })));
        top.add(left(Theme.label("언제 입으세요?", 1.1f, Font.BOLD, Theme.TEXT)));
        top.add(left(chips(recommend.helperSeasons(), season, value -> {
            season = value;
            refresh();
        })));
        top.add(left(Theme.label("체형 기준: " + spec.bodyType() + " · " + spec.ageGroup()
                + " (옷 입혀보기의 [캐릭터 신체 스펙]에서 바꿀 수 있어요)", 1f, Font.PLAIN, Theme.HINT)));

        results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
        results.setBackground(Theme.PAGE);
        results.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(results, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        JButton closeButton = Theme.secondaryButton("닫기");
        closeButton.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        closeButton.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(closeButton);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(Theme.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(new Dimension(820, 820));
        setLocationRelativeTo(owner);
        refresh();
    }

    /** [이 코디 입혀보기]로 고른 코디. 닫기만 했으면 null. */
    OutfitSuggestion chosen() {
        return chosen;
    }

    private void refresh() {
        results.removeAll();
        List<OutfitSuggestion> suggestions;
        try {
            suggestions = recommend.suggestOutfits(occasion, season, spec.bodyType(), spec.ageGroup());
        } catch (RuntimeException e) {
            suggestions = List.of();
        }
        if (suggestions.isEmpty()) {
            results.add(left(Theme.label("조건에 맞는 상품이 부족해 코디를 만들지 못했어요. 다른 상황이나 계절을 골라 보세요.",
                    1f, Font.PLAIN, Theme.TEXT)));
        }
        for (OutfitSuggestion suggestion : suggestions) {
            results.add(left(card(suggestion)));
            results.add(javax.swing.Box.createVerticalStrut(12));
        }
        results.revalidate();
        results.repaint();
    }

    private JPanel card(OutfitSuggestion suggestion) {
        JPanel photos = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        photos.setOpaque(false);
        for (Product product : suggestion.products()) {
            JLabel photo = new JLabel();
            photo.setToolTipText(product.getName());
            photo.setIcon(Thumbnails.get(product.mainImageUrl(), THUMBNAIL,
                    () -> photo.setIcon(Thumbnails.get(product.mainImageUrl(), THUMBNAIL, () -> { }))));
            photos.add(photo);
        }

        JPanel text = new JPanel(new GridLayout(0, 1, 0, 4));
        text.setOpaque(false);
        text.add(Theme.label(suggestion.title(), 1.15f, Font.BOLD, Theme.TEXT));
        text.add(Theme.label(suggestion.reason(), 1f, Font.PLAIN, Theme.HINT));
        text.add(Theme.label("옷 " + suggestion.products().size() + "벌 · 합계 "
                + String.format("%,d원", suggestion.totalPrice()), 1f, Font.PLAIN, Theme.TEXT));

        JButton wear = Theme.primaryButton("이 코디 입혀보기");
        wear.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        wear.addActionListener(e -> {
            chosen = suggestion;
            dispose();
        });

        JPanel card = new JPanel(new BorderLayout(16, 8));
        card.setBackground(Theme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        card.add(photos, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);
        JPanel action = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        action.setOpaque(false);
        action.add(wear);
        card.add(action, BorderLayout.SOUTH);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private static JPanel chips(List<String> values, String selected, java.util.function.Consumer<String> onPick) {
        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 8));
        row.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (String value : values) {
            JToggleButton chip = Theme.chip(value);
            chip.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20)); // 48px 이상 (NFR-02)
            chip.setSelected(value.equals(selected));
            chip.addActionListener(e -> onPick.accept(value));
            group.add(chip);
            row.add(chip);
        }
        return row;
    }

    private static <T extends Component> T left(T component) {
        if (component instanceof javax.swing.JComponent swing) {
            swing.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        return component;
    }
}
