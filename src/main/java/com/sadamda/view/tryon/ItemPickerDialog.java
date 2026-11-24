package com.sadamda.view.tryon;

import com.sadamda.common.AppException;
import com.sadamda.model.product.Product;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.view.ProductImages;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 아이템 고르기 창: 창 하나에서 왼쪽 카테고리 14개를 옮겨 다니며 여러 아이템을 한 번에 고른다.
 * 상품은 큰 사진 카드로 보여주고 [입히기]를 누르면 바로 입힌다. 입은 카테고리에는 [입음] 표시.
 * 창을 닫으면(✕ 포함) 고른 내용이 화면에 그대로 반영된다. 취소 버튼은 따로 두지 않는다.
 */
class ItemPickerDialog extends JDialog {
    private static final String POPULAR = "인기순";
    private static final String PRICE_LOW = "낮은 가격순";
    private static final String PRICE_HIGH = "높은 가격순";
    private static final int CARD_IMAGE = 140;

    private final Outfit working;
    private final Function<DressUpSlot, List<Product>> productsFor;
    private final Map<DressUpSlot, SideItem> sideItems = new EnumMap<>(DressUpSlot.class);
    private final Map<DressUpSlot, List<Product>> loaded = new EnumMap<>(DressUpSlot.class);
    private final List<ItemCard> cards = new ArrayList<>();
    private final JTextField keyword = Theme.textField("이름·브랜드로 찾기");
    private final JLabel info = Theme.label(" ", 0.95f, Font.PLAIN, Theme.TEXT);
    private final GridPanel grid = new GridPanel();
    private final JScrollPane gridScroll = ModernScrollBar.apply(new JScrollPane(grid));
    private final JLabel wornCount = Theme.label(" ", 1f, Font.PLAIN, Theme.TEXT);
    private final JPanel wornThumbs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel total = Theme.label(" ", 1.05f, Font.BOLD, Theme.TEXT);
    private DressUpSlot current;
    private String sort = POPULAR;
    private String notice;

    ItemPickerDialog(Window owner, Outfit outfit, DressUpSlot start, Function<DressUpSlot, List<Product>> productsFor) {
        super(owner, "아이템 고르기", ModalityType.APPLICATION_MODAL);
        this.working = outfit.copy();
        this.productsFor = productsFor;

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.CARD);
        content.add(sidebar(), BorderLayout.WEST);
        content.add(main(), BorderLayout.CENTER);
        content.add(footer(), BorderLayout.SOUTH);
        setContentPane(content);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        show(start == null ? DressUpSlot.TOP : start);
        refreshWorn();
        setSize(1160, 860);
        setLocationRelativeTo(owner);
    }

    /** 고른 한 벌. 창을 어떻게 닫든 이 내용을 반영한다. */
    Outfit result() {
        return working;
    }

    private JComponent sidebar() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(new Color(0xFAFAFA));
        list.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        DressUpSlot.Group group = null;
        for (DressUpSlot slot : DressUpSlot.values()) {
            if (slot.group() != group) {
                group = slot.group();
                JLabel heading = Theme.label(group.label(), 0.85f, Font.BOLD, Theme.HINT);
                heading.setBorder(BorderFactory.createEmptyBorder(list.getComponentCount() == 0 ? 2 : 10, 10, 2, 0));
                heading.setAlignmentX(Component.LEFT_ALIGNMENT);
                list.add(heading);
            }
            SideItem item = new SideItem(slot);
            item.addActionListener(e -> show(slot));
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            sideItems.put(slot, item);
            list.add(item);
        }
        JScrollPane scroll = ModernScrollBar.apply(new JScrollPane(list));
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.LINE));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(240, 100));
        return scroll;
    }

    private JComponent main() {
        keyword.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { fillGrid(); }
            @Override public void removeUpdate(DocumentEvent e) { fillGrid(); }
            @Override public void changedUpdate(DocumentEvent e) { fillGrid(); }
        });
        JPanel sorts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sorts.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (String option : List.of(POPULAR, PRICE_LOW, PRICE_HIGH)) {
            JToggleButton chip = TryOnUi.compact(Theme.chip(option));
            chip.setSelected(option.equals(sort));
            chip.addActionListener(e -> {
                sort = option;
                fillGrid();
            });
            group.add(chip);
            sorts.add(chip);
        }
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.add(keyword, BorderLayout.CENTER);
        bar.add(sorts, BorderLayout.EAST);

        JPanel infoBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = TryOnUi.smooth(graphics);
                g.setColor(Theme.PAGE);
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g.dispose();
            }
        };
        infoBox.setOpaque(false);
        infoBox.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        infoBox.add(info, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(bar, BorderLayout.NORTH);
        top.add(infoBox, BorderLayout.CENTER);

        gridScroll.getVerticalScrollBar().setUnitIncrement(24);
        gridScroll.getViewport().setBackground(Theme.CARD);

        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBackground(Theme.CARD);
        main.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        main.add(top, BorderLayout.NORTH);
        main.add(gridScroll, BorderLayout.CENTER);
        return main;
    }

    private JComponent footer() {
        wornThumbs.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(wornCount);
        left.add(wornThumbs);

        JButton undress = TryOnUi.secondary("모두 벗기");
        undress.addActionListener(e -> {
            working.clear();
            notice = "모두 벗겼어요.";
            refreshWorn();
        });
        JButton done = TryOnUi.primary("다 골랐어요");
        done.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(done);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(total);
        right.add(undress);
        right.add(done);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(12, 20, 14, 20)));
        footer.add(left, BorderLayout.CENTER);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    /** 왼쪽에서 카테고리를 고르면 그 상품을 보여준다. */
    private void show(DressUpSlot slot) {
        current = slot;
        notice = null;
        sideItems.values().forEach(item -> item.repaint());
        keyword.setText("");
        fillGrid();
        gridScroll.getVerticalScrollBar().setValue(0);
    }

    private List<Product> products(DressUpSlot slot) {
        return loaded.computeIfAbsent(slot, productsFor);
    }

    private void fillGrid() {
        if (current == null) {
            return;
        }
        String word = keyword.getText().trim().toLowerCase(Locale.ROOT);
        List<Product> shown = new ArrayList<>();
        for (Product product : products(current)) {
            String text = (product.getName() + " " + product.getBrand()).toLowerCase(Locale.ROOT);
            if (word.isEmpty() || text.contains(word)) {
                shown.add(product);
            }
        }
        if (PRICE_LOW.equals(sort)) {
            shown.sort(Comparator.comparingInt(Product::getPrice));
        } else if (PRICE_HIGH.equals(sort)) {
            shown.sort(Comparator.comparingInt(Product::getPrice).reversed());
        }
        grid.removeAll();
        cards.clear();
        for (Product product : shown) {
            ItemCard card = new ItemCard(current, product);
            cards.add(card);
            grid.add(card);
        }
        if (shown.isEmpty()) {
            JLabel empty = Theme.label(word.isEmpty() ? "아직 고를 수 있는 상품이 없어요." : "찾는 상품이 없어요.",
                    1f, Font.PLAIN, Theme.HINT);
            empty.setBorder(BorderFactory.createEmptyBorder(24, 4, 0, 0));
            grid.add(empty);
        }
        grid.revalidate();
        grid.repaint();
        updateInfo(shown.size());
    }

    private void updateInfo(int count) {
        if (notice != null) {
            info.setText(notice);
            return;
        }
        String label = current.label();
        String text = label + " " + count + "개 · 한 번에 하나만 입을 수 있어요. 다른 " + label + object(label)
                + " 누르면 바꿔 입혀요.";
        if (current == DressUpSlot.DRESS) {
            text += " 원피스를 입히면 상의·하의는 벗겨져요.";
        } else if (current == DressUpSlot.TOP || current == DressUpSlot.BOTTOM) {
            text += " " + label + object(label) + " 입히면 원피스는 벗겨져요.";
        }
        info.setText(text);
    }

    /** [입히기] / [벗기기] */
    private void toggle(DressUpSlot slot, Product product) {
        if (product.equals(working.get(slot))) {
            working.remove(slot);
            notice = slot.label() + object(slot.label()) + " 벗겼어요.";
        } else {
            try {
                List<DressUpSlot> removed = working.put(slot, product);
                String last = removed.isEmpty() ? null : removed.get(removed.size() - 1).label();
                notice = removed.isEmpty() ? null : slot.label() + object(slot.label()) + " 입혀서 "
                        + String.join("·", removed.stream().map(DressUpSlot::label).toList()) + object(last)
                        + " 벗겼어요.";
            } catch (AppException e) {
                notice = e.getMessage();
            }
        }
        refreshWorn();
    }

    /** 입은 표시(카드·왼쪽 목록·아래 줄)를 다시 그린다. */
    private void refreshWorn() {
        cards.forEach(ItemCard::refresh);
        sideItems.values().forEach(item -> item.repaint());
        wornCount.setText("입힌 아이템 " + working.size() + "개");
        rebuildThumbs();
        total.setText(working.isEmpty() ? " " : "합계 " + String.format("%,d원", working.totalPrice()));
        updateInfo(cards.size());
    }

    /** 아래 줄의 입힌 아이템 작은 사진. 사진을 다 받으면 다시 만든다. */
    private void rebuildThumbs() {
        wornThumbs.removeAll();
        for (Product product : working.products()) {
            JLabel thumb = new JLabel(Thumbnails.get(product.mainImageUrl(), 40, this::rebuildThumbs));
            thumb.setToolTipText(product.getName());
            thumb.setBorder(BorderFactory.createLineBorder(Theme.LINE));
            wornThumbs.add(thumb);
        }
        wornThumbs.revalidate();
        wornThumbs.repaint();
    }

    /** 받침이 있으면 "을", 없으면 "를". */
    static String object(String word) {
        return hasFinalConsonant(word) ? "을" : "를";
    }

    private static boolean hasFinalConsonant(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        char last = word.charAt(word.length() - 1);
        return last >= 0xAC00 && last <= 0xD7A3 && (last - 0xAC00) % 28 != 0;
    }

    /** 왼쪽 카테고리 한 줄: 고른 카테고리는 검정 바탕, 입은 카테고리는 오른쪽에 [입음]. */
    private class SideItem extends JButton {
        private final DressUpSlot slot;

        SideItem(DressUpSlot slot) {
            this.slot = slot;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName(slot.label());
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
            Font base = UIManager.getFont("Label.font");
            setFont(base.deriveFont(Font.PLAIN, base.getSize2D() * 1.02f));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(getFont());
            return new Dimension(160, Math.max(40, metrics.getHeight() + 12));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = TryOnUi.smooth(graphics);
            boolean selected = slot == current;
            boolean worn = working.get(slot) != null;
            if (selected) {
                g.setColor(Theme.PRIMARY);
                g.fill(new RoundRectangle2D.Float(0, 2, getWidth(), getHeight() - 4, 14, 14));
            } else if (getModel().isRollover() || isFocusOwner()) {
                g.setColor(Theme.SECONDARY);
                g.fill(new RoundRectangle2D.Float(0, 2, getWidth(), getHeight() - 4, 14, 14));
            }
            Font font = selected ? getFont().deriveFont(Font.BOLD) : getFont();
            g.setFont(font);
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(selected ? Color.WHITE : Theme.TEXT);
            g.drawString(slot.label(), 12, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            if (worn) {
                Font small = getFont().deriveFont(Font.BOLD, getFont().getSize2D() * 0.72f);
                g.setFont(small);
                FontMetrics sm = g.getFontMetrics();
                String text = "입음";
                int w = sm.stringWidth(text) + 14;
                int h = sm.getHeight() + 2;
                int x = getWidth() - w - 10;
                int y = (getHeight() - h) / 2;
                g.setColor(selected ? Color.WHITE : Theme.SECONDARY);
                g.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
                g.setColor(Theme.TEXT);
                g.drawString(text, x + 7, y + 1 + sm.getAscent());
            }
            g.dispose();
        }
    }

    /** 상품 카드: 큰 사진, 이름(두 줄까지), 브랜드, 가격, [입히기]/[입는 중 · 벗기기]. 입는 중이면 굵은 테두리. */
    private class ItemCard extends JPanel {
        private final DressUpSlot slot;
        private final Product product;
        private final CardLayout buttonCards = new CardLayout();
        private final JPanel buttons = new JPanel(buttonCards);

        ItemCard(DressUpSlot slot, Product product) {
            super(new BorderLayout(0, 8));
            this.slot = slot;
            this.product = product;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 10, 8));

            JComponent infoView = new JComponent() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    paintInfo(TryOnUi.smooth(graphics), getWidth());
                }

                @Override
                public Dimension getPreferredSize() {
                    FontMetrics metrics = getFontMetrics(UIManager.getFont("Label.font"));
                    return new Dimension(170, CARD_IMAGE + 10 + metrics.getHeight() * 4 + 6);
                }
            };
            infoView.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            infoView.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!worn()) {
                        toggle(slot, product);
                    }
                }
            });
            infoView.setToolTipText(product.getName());

            JButton wear = Theme.secondaryButton("입히기");
            wear.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
            wear.addActionListener(e -> toggle(slot, product));
            wear.getAccessibleContext().setAccessibleName(product.getName() + " 입히기");
            JButton off = Theme.primaryButton("입는 중 · 벗기기");
            off.setIcon(TryOnUi.checkIcon(16, Color.WHITE));
            off.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
            off.addActionListener(e -> toggle(slot, product));
            off.getAccessibleContext().setAccessibleName(product.getName() + " 벗기기");
            buttons.setOpaque(false);
            buttons.add(wear, "wear");
            buttons.add(off, "off");

            add(infoView, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);
            refresh();
        }

        private boolean worn() {
            return product.equals(working.get(slot));
        }

        void refresh() {
            buttonCards.show(buttons, worn() ? "off" : "wear");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = TryOnUi.smooth(graphics);
            boolean worn = worn();
            g.setColor(Theme.CARD);
            g.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 18, 18));
            g.setColor(worn ? Theme.PRIMARY : Theme.LINE);
            g.setStroke(new BasicStroke(worn ? 3f : 1f));
            g.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 3, getHeight() - 3, 18, 18));
            g.dispose();
        }

        private void paintInfo(Graphics2D g, int width) {
            BufferedImage image = ProductImages.get(product.mainImageUrl());
            if (image == null && !ProductImages.failed(product.mainImageUrl())) {
                ProductImages.request(product.mainImageUrl(), this::repaint);
            }
            if (image != null) {
                TryOnUi.drawContain(g, image, 4, 4, width - 8, CARD_IMAGE - 8);
            } else {
                g.setColor(new Color(0xF4F4F4));
                g.fillRect(4, 4, width - 8, CARD_IMAGE - 8);
            }
            Font base = UIManager.getFont("Label.font");
            if (worn()) {
                Font badgeFont = base.deriveFont(Font.BOLD, base.getSize2D() * 0.75f);
                g.setFont(badgeFont);
                FontMetrics bm = g.getFontMetrics();
                int bw = bm.stringWidth("입는 중") + 16;
                int bh = bm.getHeight() + 4;
                g.setColor(Theme.PRIMARY);
                g.fill(new RoundRectangle2D.Float(4, 4, bw, bh, bh, bh));
                g.setColor(Color.WHITE);
                g.drawString("입는 중", 12, 6 + bm.getAscent());
            }
            Font bold = base.deriveFont(Font.BOLD, base.getSize2D() * 0.9f);
            Font small = base.deriveFont(Font.PLAIN, base.getSize2D() * 0.82f);
            Font price = base.deriveFont(Font.BOLD, base.getSize2D() * 0.95f);
            int y = CARD_IMAGE + 8;
            g.setColor(Theme.TEXT);
            g.setFont(bold);
            FontMetrics metrics = g.getFontMetrics();
            for (String line : TryOnUi.wrap(product.getName(), metrics, width - 8, 2)) {
                y += metrics.getAscent();
                g.drawString(line, 4, y);
                y += metrics.getDescent() + 1;
            }
            g.setFont(small);
            metrics = g.getFontMetrics();
            y += metrics.getAscent() + 2;
            g.setColor(Theme.HINT);
            g.drawString(TryOnUi.ellipsize(product.getBrand(), metrics, width - 8), 4, y);
            y += metrics.getDescent();
            g.setFont(price);
            metrics = g.getFontMetrics();
            y += metrics.getAscent() + 2;
            g.setColor(Theme.TEXT);
            g.drawString(String.format("%,d원", product.getPrice()), 4, y);
            g.dispose();
        }
    }

    /** 폭은 창에 맞추고(4열), 높이는 카드 수만큼 늘어나 세로로 스크롤한다. */
    private static class GridPanel extends JPanel implements Scrollable {
        GridPanel() {
            super(new GridLayout(0, 4, 14, 14));
            setBackground(Theme.CARD);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 48);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
