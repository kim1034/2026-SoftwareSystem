package com.sadamda.view.recommend;

import com.sadamda.controller.AppContext;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.recommend.Lookbook;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.product.ProductImages;
import com.sadamda.view.product.ShopUi;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 룩북 화면 (UC-07 룩북 코디 추천 보기, FR-09·FR-10).
 * - 목록: 상황·계절로 고르기, 코디 카드
 * - 코디 보기: 큰 코디 사진, 구성 상품, [코디 전체 담기]
 * - 관리자: [새 코디 올리기], 코디 [수정]·[삭제] (UC-17, FR-26)
 *
 * 다른 화면에서 특정 코디를 열 때는 showLookbook(id) 를 부른 뒤 navigate(NAME) 한다.
 */
public class LookbookPanel extends ScreenPanel {
    public static final String NAME = "lookbook";

    private static final String ANY = "전체";
    private static final String LIST = "list";
    private static final String DETAIL = "detail";

    private final CardLayout views = new CardLayout();
    private final JPanel viewHolder = new JPanel(views);

    private final JComboBox<String> occasionBox = new JComboBox<>();
    private final JComboBox<String> seasonBox = new JComboBox<>();
    private final JButton createButton = ShopUi.primaryButton("새 코디 올리기");
    private final JLabel listSummary = ShopUi.label("", 1.1f, Font.BOLD, ShopUi.TEXT);
    private final JPanel listGrid = new JPanel(new GridLayout(0, 3, 16, 16));
    private final JPanel detail = new JPanel(new BorderLayout(0, 20));
    private JScrollPane listScroll;
    private JScrollPane detailScroll;

    private Long openLookbookId; // 코디 보기 중이면 그 코디 id
    private boolean adjusting;

    public LookbookPanel(AppContext context, MainFrame frame) {
        super(context, frame, "룩북", "UC-07 룩북 코디 추천 보기");
        hideTitle();
        setBackground(ShopUi.PAGE);
        viewHolder.add(buildList(), LIST);
        viewHolder.add(buildDetailHolder(), DETAIL);
        setContent(viewHolder);
        ShopUi.whenShown(this, this::refresh);
        refresh();
    }

    /** 코디 보기로 연다 (홈 화면 등에서). */
    public void showLookbook(Long lookbookId) {
        openLookbookId = lookbookId;
        refresh();
    }

    /** 지금 보고 있는 화면(목록 또는 코디 보기)을 최신 내용으로 다시 그린다. */
    private void refresh() {
        createButton.setVisible(context.session.isAdmin());
        if (openLookbookId != null) {
            renderDetail(openLookbookId);
        } else {
            reloadFilters();
            renderList();
        }
    }

    // ---------- 목록 ----------

    private JComponent buildList() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ShopUi.PAGE);
        body.setBorder(BorderFactory.createEmptyBorder(8, 8, 24, 8));

        addBlock(body, ShopUi.sectionHeader("룩북 코디", "상황과 계절에 맞춘 코디를 골라 보세요. 코디를 누르면 구성 상품을 볼 수 있어요.",
                createButton), 0);
        createButton.addActionListener(e -> openEditor(null));

        occasionBox.getAccessibleContext().setAccessibleName("상황");
        seasonBox.getAccessibleContext().setAccessibleName("계절");
        for (JComboBox<String> box : List.of(occasionBox, seasonBox)) {
            box.setPreferredSize(new Dimension(200, ShopUi.MIN_TOUCH));
            box.addActionListener(e -> {
                if (!adjusting) {
                    renderList();
                }
            });
        }
        addBlock(body, ShopUi.row(12, ShopUi.label("상황", 1f, Font.BOLD, ShopUi.HINT), occasionBox,
                ShopUi.label("계절", 1f, Font.BOLD, ShopUi.HINT), seasonBox), 16);
        addBlock(body, listSummary, 20);
        listGrid.setOpaque(false);
        addBlock(body, listGrid, 12);

        listScroll = ShopUi.verticalScroll(body);
        return listScroll;
    }

    private static void addBlock(JPanel body, JComponent component, int topGap) {
        if (topGap > 0) {
            JPanel gap = new JPanel();
            gap.setOpaque(false);
            gap.setPreferredSize(new Dimension(1, topGap));
            gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, topGap));
            gap.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(gap);
        }
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(component);
    }

    /** UC-07 2단계 필터 값은 등록된 코디에 있는 것만 보여준다. 고른 값은 유지한다. */
    private void reloadFilters() {
        adjusting = true;
        refill(occasionBox, context.recommend.occasions());
        refill(seasonBox, context.recommend.seasons());
        adjusting = false;
    }

    private static void refill(JComboBox<String> box, List<String> values) {
        Object before = box.getSelectedItem();
        box.removeAllItems();
        box.addItem(ANY);
        values.forEach(box::addItem);
        box.setSelectedItem(before != null && values.contains(before) ? before : ANY);
    }

    private void renderList() {
        String occasion = selected(occasionBox);
        String season = selected(seasonBox);
        List<Lookbook> lookbooks = context.recommend.lookbooks(occasion, season);
        listGrid.removeAll();
        for (Lookbook lookbook : lookbooks) {
            listGrid.add(new LookbookCard(lookbook, products(lookbook), this::openDetail));
        }
        // 카드가 셋보다 적어도 카드 크기가 커지지 않게 빈칸을 채운다
        for (int i = lookbooks.size(); i < 3; i++) {
            JPanel filler = new JPanel();
            filler.setOpaque(false);
            listGrid.add(filler);
        }
        listSummary.setText(lookbooks.isEmpty() ? "조건에 맞는 코디가 없습니다. 상황이나 계절을 '전체'로 바꿔 보세요."
                : "코디 " + lookbooks.size() + "개");
        listGrid.revalidate();
        listGrid.repaint();
        views.show(viewHolder, LIST);
    }

    private static String selected(JComboBox<String> box) {
        Object value = box.getSelectedItem();
        return value == null || ANY.equals(value) ? null : value.toString();
    }

    private List<Product> products(Lookbook lookbook) {
        try {
            return context.recommend.lookbookProducts(lookbook.getId());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    // ---------- 코디 보기 ----------

    private JComponent buildDetailHolder() {
        detail.setBackground(ShopUi.PAGE);
        detail.setBorder(BorderFactory.createEmptyBorder(8, 8, 24, 8));
        detailScroll = ShopUi.verticalScroll(detail);
        return detailScroll;
    }

    private void openDetail(Lookbook lookbook) {
        openLookbookId = lookbook.getId();
        renderDetail(lookbook.getId());
    }

    private void backToList() {
        openLookbookId = null;
        refresh();
    }

    /** UC-07 3~4단계: 코디 사진과 구성 상품. */
    private void renderDetail(Long lookbookId) {
        Lookbook lookbook;
        List<Product> products;
        try {
            lookbook = context.recommend.lookbook(lookbookId);
            products = context.recommend.lookbookProducts(lookbookId);
        } catch (RuntimeException e) {
            // 그사이 지워진 코디: 목록으로 돌아간다
            openLookbookId = null;
            reloadFilters();
            renderList();
            return;
        }
        detail.removeAll();

        JButton back = ShopUi.secondaryButton("← 코디 목록으로");
        back.addActionListener(e -> backToList());
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(ShopUi.row(0, back), BorderLayout.WEST);
        if (context.session.isAdmin()) {
            JButton edit = ShopUi.secondaryButton("코디 수정");
            edit.addActionListener(e -> openEditor(lookbook));
            JButton delete = ShopUi.secondaryButton("코디 삭제");
            delete.addActionListener(e -> deleteLookbook(lookbook));
            top.add(ShopUi.row(10, edit, delete), BorderLayout.EAST);
        }
        detail.add(top, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(28, 0));
        main.setOpaque(false);
        JPanel pictureCard = ShopUi.card(new BorderLayout());
        pictureCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        pictureCard.add(LookbookCard.picture(lookbook, products, 420), BorderLayout.CENTER);
        JPanel pictureHolder = new JPanel(new BorderLayout());
        pictureHolder.setOpaque(false);
        pictureHolder.add(pictureCard, BorderLayout.NORTH);
        main.add(pictureHolder, BorderLayout.WEST);
        main.add(detailInfo(lookbook, products), BorderLayout.CENTER);
        detail.add(main, BorderLayout.CENTER);

        detail.revalidate();
        detail.repaint();
        views.show(viewHolder, DETAIL);
        SwingUtilities.invokeLater(() -> detailScroll.getVerticalScrollBar().setValue(0));
    }

    private JComponent detailInfo(Lookbook lookbook, List<Product> products) {
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        addBlock(info, LookbookCard.tags(lookbook), 0);
        addBlock(info, ShopUi.label("<html><div style='width:480px'>" + ShopUi.escape(lookbook.getTitle()) + "</div></html>", 1.5f, Font.BOLD, ShopUi.TEXT), 8);
        int total = products.stream().mapToInt(Product::getPrice).sum();
        addBlock(info, ShopUi.label("구성 상품 " + products.size() + "개 · 합계 " + ShopUi.price(total), 1.15f, Font.BOLD,
                ShopUi.PRIMARY), 8);

        JButton addAll = ShopUi.primaryButton("코디 전체 담기");
        addAll.setToolTipText("구성 상품을 한 번에 장바구니에 담습니다");
        addAll.addActionListener(e -> addAllToCart(products));
        addAll.setEnabled(!products.isEmpty());
        addBlock(info, ShopUi.row(0, addAll), 16);
        addBlock(info, ShopUi.hint("색상·사이즈는 첫 번째 남은 옵션으로 담깁니다. 장바구니에서 바꿀 수 있어요."), 6);

        JPanel list = new JPanel(new GridLayout(0, 1, 0, 10));
        list.setOpaque(false);
        for (Product product : products) {
            list.add(productRow(product));
        }
        if (products.isEmpty()) {
            list.add(ShopUi.label("구성 상품이 모두 판매가 끝났습니다.", 1f, Font.PLAIN, ShopUi.HINT));
        }
        addBlock(info, list, 20);
        return info;
    }

    /** 구성 상품 한 줄. 누르면 상품 정보를 연다. 품절이면 표시한다 (6a). */
    private JComponent productRow(Product product) {
        JPanel row = ShopUi.card(new BorderLayout(16, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 16));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel image = new JLabel();
        image.setIcon(ProductImages.get(product.mainImageUrl(), 96, 110, () ->
                image.setIcon(ProductImages.get(product.mainImageUrl(), 96, 110, null))));
        row.add(image, BorderLayout.WEST);

        JPanel texts = new JPanel(new GridLayout(0, 1, 0, 2));
        texts.setOpaque(false);
        String status = !product.isOnSale() ? "  · 판매 중지" : product.isSoldOut() ? "  · 품절" : "";
        texts.add(ShopUi.hint(product.getCategory().label() + " · " + product.getBrand() + status));
        texts.add(ShopUi.label("<html><div style='width:300px'>" + ShopUi.escape(product.getName()) + "</div></html>", 1f, Font.PLAIN, ShopUi.TEXT));
        texts.add(ShopUi.label(ShopUi.price(product.getPrice()), 1.05f, Font.BOLD, ShopUi.TEXT));
        row.add(texts, BorderLayout.CENTER);

        JButton view = ShopUi.secondaryButton("상품 보기");
        view.addActionListener(e -> ShopUi.openProduct(frame, context, product));
        JPanel right = new JPanel(new java.awt.GridBagLayout());
        right.setOpaque(false);
        right.add(view);
        row.add(right, BorderLayout.EAST);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ShopUi.openProduct(frame, context, product);
            }
        });
        return row;
    }

    /**
     * UC-07 5~6단계 [코디 전체 담기].
     * 품절·판매 중지 상품이 있으면 알려 주고, 나머지만 담을지 묻는다 (6a).
     */
    private void addAllToCart(List<Product> products) {
        List<OrderLine> lines = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        for (Product product : products) {
            ProductOption option = product.isOnSale() ? firstInStock(product) : null;
            if (option == null) {
                unavailable.add(product.getName());
            } else {
                lines.add(new OrderLine(product.getId(), option.id(), 1));
            }
        }
        if (lines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "담을 수 있는 상품이 없습니다. 모두 품절되었습니다.");
            return;
        }
        if (!unavailable.isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "품절된 상품이 있습니다:\n- " + String.join("\n- ", unavailable) + "\n\n나머지 "
                            + lines.size() + "개만 담을까요?", "품절 상품 안내", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            context.cart.addAll(lines);
            JOptionPane.showMessageDialog(this, "코디 상품 " + lines.size() + "개를 장바구니에 담았습니다.");
        } catch (RuntimeException e) {
            ShopUi.showError(this, frame, e);
        }
    }

    private static ProductOption firstInStock(Product product) {
        return product.getOptions().stream().filter(option -> option.stock() > 0).findFirst().orElse(null);
    }

    // ---------- 관리자: 코디 올리기·수정·삭제 (UC-17) ----------

    private void openEditor(Lookbook original) {
        List<Product> current = original == null ? List.of() : context.recommend.lookbookProducts(original.getId());
        LookbookEditorDialog dialog = new LookbookEditorDialog(frame, context, original, current);
        dialog.setVisible(true);
        Lookbook saved = dialog.saved();
        if (saved != null) {
            openLookbookId = saved.getId();
            refresh();
        }
    }

    private void deleteLookbook(Lookbook lookbook) {
        int answer = JOptionPane.showConfirmDialog(this, "\"" + lookbook.getTitle() + "\" 코디를 삭제할까요?\n삭제하면 되돌릴 수 없습니다.",
                "코디 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            context.admin.deleteLookbook(lookbook.getId());
            JOptionPane.showMessageDialog(this, "코디를 삭제했습니다.");
            backToList();
        } catch (RuntimeException e) {
            ShopUi.showError(this, frame, e);
        }
    }
}
