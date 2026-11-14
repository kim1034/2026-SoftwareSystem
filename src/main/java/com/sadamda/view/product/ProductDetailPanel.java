package com.sadamda.view.product;

import com.sadamda.common.AppException;
import com.sadamda.common.LoginRequiredException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.review.Review;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.WrapLayout;
import com.sadamda.view.cart.CartPanel;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.order.OrderPanel;
import com.sadamda.view.tryon.TryOnPanel;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UC-06 상품 상세 조회: 왼쪽 큰 사진(누르면 확대), 오른쪽 가격·상품명·평점·소재·사이즈·색상과
 * [바로 구매] [장바구니 담기] [AI로 입어보기], 아래에 공통 사이즈표와 리뷰.
 * 열 때는 MainFrame.openProduct(id) 를 쓴다.
 */
public class ProductDetailPanel extends ScreenPanel {
    public static final String NAME = "productDetail";

    /** 모든 상품에 공통으로 보여주는 사담다 표준 치수 (cm). 상품별 치수 데이터가 생기면 바꾼다. */
    private static final String[][] SIZE_CHART = {
            {"사이즈", "가슴둘레", "허리둘레", "엉덩이둘레"},
            {"S (55)", "82~86", "64~68", "88~92"},
            {"M (66)", "86~90", "68~72", "92~96"},
            {"L (77)", "90~95", "72~77", "96~101"},
            {"XL (88)", "95~100", "77~82", "101~106"},
            {"XXL (99)", "100~106", "82~88", "106~112"}};

    private final Body body = new Body();
    private final JScrollPane scroll = new JScrollPane(body);

    private ProductDetail product;
    private List<Review> reviews = List.of();

    // 지금 보고 있는 상품의 화면 조각. showProduct 때마다 새로 만든다.
    private final JPanel mainRow = new JPanel(new GridBagLayout());
    private JPanel gallery;
    private JPanel info;
    private ProductPhoto photo;
    private JComponent reviewSection;
    private final Map<String, JToggleButton> sizeChips = new LinkedHashMap<>();
    private final Map<String, JToggleButton> colorChips = new LinkedHashMap<>();
    private ButtonGroup sizeGroup = new ButtonGroup();
    private ButtonGroup colorGroup = new ButtonGroup();
    private final JLabel sizeError = Theme.errorLabel();
    private final JLabel colorError = Theme.errorLabel();
    private final JLabel actionError = Theme.errorLabel();

    public ProductDetailPanel(AppContext context, MainFrame frame) {
        super(context, frame, "상품 상세", "UC-06 상품 상세 조회");
        hideTitle();
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createEmptyBorder());
        mainRow.setOpaque(false);

        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        setContent(scroll);

        // 창 폭이나 글씨 크기가 바뀌면 사진·정보를 나란히 둘지 위아래로 둘지 다시 정한다.
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                arrange();
            }
        });
        body.addPropertyChangeListener("font", e -> SwingUtilities.invokeLater(this::arrange));
    }

    /** UC-06 1~2단계: 상품 정보와 리뷰를 불러와 화면을 새로 만든다. */
    public void showProduct(Long productId) {
        product = fetchDetail(productId);
        reviews = product == null ? List.of() : fetchReviews(productId);
        rebuild();
        scroll.getViewport().setViewPosition(new Point(0, 0));
    }

    // TODO Product getter 가 생기면 context.product.detail(productId) 결과를 ProductDetail 로 바꿔 쓴다.
    private ProductDetail fetchDetail(Long productId) {
        return MockProducts.detail(productId);
    }

    // TODO ReviewService 가 구현되면 context.product.reviews(productId) 로 바꾼다.
    private List<Review> fetchReviews(Long productId) {
        return MockProducts.reviews(productId);
    }

    private void rebuild() {
        body.removeAll();
        JButton back = Theme.linkButton("← 상품 목록으로");
        back.addActionListener(e -> frame.navigate(ProductListPanel.NAME));
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.setOpaque(false);
        backRow.add(back);
        Theme.addToCard(body, backRow, 0);

        if (product == null) {
            Theme.addToCard(body, Theme.label("상품을 찾을 수 없어요", 1.3f, Font.BOLD, Theme.TEXT), 24);
            Theme.addToCard(body, Theme.label("판매가 끝났거나 주소가 바뀌었을 수 있어요.", 1f, Font.PLAIN, Theme.HINT), 8);
        } else {
            gallery = createGallery();
            info = createInfo();
            Theme.addToCard(body, mainRow, 16);
            Theme.addToCard(body, divider(), 48);
            Theme.addToCard(body, createSizeChart(), 40);
            Theme.addToCard(body, divider(), 48);
            reviewSection = createReviews();
            Theme.addToCard(body, reviewSection, 40);
            arrange();
        }
        body.revalidate();
        body.repaint();
    }

    /** 폭이 넉넉하면 [사진 | 구분선 | 정보], 모자라면 사진 위·정보 아래로 놓는다. */
    private void arrange() {
        if (product == null || gallery == null) {
            return;
        }
        int available = scroll.getViewport().getWidth() - body.getInsets().left - body.getInsets().right;
        if (available <= 0) {
            return;
        }
        float fontSize = UIManager.getFont("Label.font").getSize2D();
        int photoWidth = Math.max(360, Math.round(fontSize * 26));
        int infoMinimum = Math.round(fontSize * 24);
        boolean sideBySide = available >= photoWidth + 97 + infoMinimum; // 97: 구분선과 양쪽 여백
        photo.setPhotoWidth(sideBySide ? photoWidth : Math.min(photoWidth, available));

        mainRow.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        if (sideBySide) {
            mainRow.add(gallery, c);
            c.fill = GridBagConstraints.VERTICAL;
            c.insets = new Insets(0, 48, 0, 48);
            JPanel line = new JPanel();
            line.setBackground(Theme.LINE);
            line.setPreferredSize(new Dimension(1, 1));
            mainRow.add(line, c);
            c.insets = new Insets(0, 0, 0, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            mainRow.add(info, c);
        } else {
            c.gridx = 0;
            c.anchor = GridBagConstraints.NORTH;
            mainRow.add(gallery, c);
            c.insets = new Insets(32, 0, 0, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            mainRow.add(info, c);
        }
        mainRow.revalidate();
        // 줄바꿈되는 글(상품명·리뷰)은 폭이 정해진 뒤에야 높이가 맞으므로 한 번 더 배치한다.
        SwingUtilities.invokeLater(() -> {
            body.revalidate();
            body.repaint();
        });
    }

    /** 왼쪽: 큰 사진, 여러 장이면 작은 사진 목록, "누르면 크게" 안내. */
    private JPanel createGallery() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        photo = new ProductPhoto(product.imageUrls(), 0, 360);
        Theme.addToCard(panel, photo, 0);
        if (product.imageUrls().size() > 1) {
            JPanel thumbs = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
            thumbs.setOpaque(false);
            for (int i = 0; i < product.imageUrls().size(); i++) {
                int index = i;
                JButton thumb = Theme.secondaryButton((i + 1) + "");
                thumb.getAccessibleContext().setAccessibleName((i + 1) + "번째 사진 보기");
                thumb.addActionListener(e -> photo.showImage(index));
                thumbs.add(thumb);
            }
            Theme.addToCard(panel, thumbs, 16);
        }
        JLabel hint = Theme.label("사진을 누르면 크게 볼 수 있어요", 1f, Font.PLAIN, Theme.HINT);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.addToCard(panel, hint, 12);
        return panel;
    }

    /** 오른쪽: 가격, 상품명, 평점, 소재, 사이즈·색상 선택, 주문 버튼 (UC-06 2단계, 4단계). */
    private JPanel createInfo() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        Theme.addToCard(panel, Theme.label(price(product.price()), 1.8f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(panel, text(product.name(), 1.3f, Font.PLAIN, Theme.TEXT), 8);
        Theme.addToCard(panel, ratingRow(), 8);

        Theme.addToCard(panel, divider(), 24);
        JPanel materialRow = new JPanel(new BorderLayout(24, 0));
        materialRow.setOpaque(false);
        materialRow.add(Theme.label("소재", 1f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        materialRow.add(text(product.material() == null ? "정보 없음" : product.material(), 1f, Font.PLAIN, Theme.TEXT),
                BorderLayout.CENTER);
        Theme.addToCard(panel, materialRow, 24);

        createOptionChips();
        Theme.addToCard(panel, labelWithError("사이즈", sizeError), 32);
        Theme.addToCard(panel, chipRow(sizeChips), 4);
        Theme.addToCard(panel, labelWithError("색상", colorError), 20);
        Theme.addToCard(panel, chipRow(colorChips), 4);

        JButton buyNow = Theme.primaryButton("바로 구매");
        buyNow.addActionListener(e -> buyNow());
        JButton addToCart = Theme.secondaryButton("장바구니 담기");
        addToCart.addActionListener(e -> addToCart());
        JButton tryOn = Theme.secondaryButton("AI로 입어보기");
        tryOn.addActionListener(e -> tryOn());
        JPanel secondary = new JPanel(new GridLayout(1, 2, 12, 0));
        secondary.setOpaque(false);
        secondary.add(addToCart);
        secondary.add(tryOn);

        Theme.addToCard(panel, actionError, 32);
        Theme.addToCard(panel, buyNow, 8);
        Theme.addToCard(panel, secondary, 12);
        Theme.addToCard(panel, Theme.label("수량은 장바구니에서 바꿀 수 있어요", 1f, Font.PLAIN, Theme.HINT), 12);
        return panel;
    }

    /** "★ 4.7  리뷰 3개" — 리뷰 링크를 누르면 아래 리뷰로 내려간다. */
    private JPanel ratingRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        if (reviews.isEmpty()) {
            row.add(Theme.label("아직 리뷰가 없어요", 1f, Font.PLAIN, Theme.HINT));
            return row;
        }
        row.add(Theme.label("★ " + averageRating(), 1f, Font.BOLD, Theme.TEXT));
        JButton link = Theme.linkButton("리뷰 " + reviews.size() + "개");
        link.addActionListener(e -> reviewSection.scrollRectToVisible(
                new Rectangle(0, 0, reviewSection.getWidth(), scroll.getViewport().getHeight())));
        row.add(link);
        return row;
    }

    // ── 사이즈·색상 (UC-06 4단계, 4b 품절) ──

    private void createOptionChips() {
        sizeChips.clear();
        colorChips.clear();
        sizeGroup = new ButtonGroup();
        colorGroup = new ButtonGroup();
        sizeError.setText("");
        colorError.setText("");
        actionError.setText("");
        List<String> sizes = product.options().stream().map(ProductOption::size).distinct().toList();
        List<String> colors = product.options().stream().map(ProductOption::color).distinct().toList();
        for (String size : sizes) {
            JToggleButton chip = Theme.chip(size);
            chip.addActionListener(e -> {
                sizeError.setText("");
                updateColorChips();
            });
            sizeGroup.add(chip);
            sizeChips.put(size, chip);
        }
        for (String color : colors) {
            JToggleButton chip = Theme.chip(color);
            chip.addActionListener(e -> colorError.setText(""));
            colorGroup.add(chip);
            colorChips.put(color, chip);
        }
        // 사이즈는 어떤 색이든 재고가 있으면 고를 수 있다. 다 품절이면 "품절"을 붙이고 막는다.
        sizeChips.forEach((size, chip) -> setAvailable(chip, size,
                product.options().stream().anyMatch(o -> o.size().equals(size) && o.stock() > 0)));
        updateColorChips();
        // 색이 한 가지뿐이고 살 수 있으면 미리 골라 둔다.
        if (colors.size() == 1 && colorChips.get(colors.get(0)).isEnabled()) {
            colorChips.get(colors.get(0)).setSelected(true);
        }
    }

    /** 사이즈를 고르면 그 사이즈에서 품절인 색을 막는다. 고른 색이 품절이 되면 선택을 푼다. */
    private void updateColorChips() {
        String size = selected(sizeChips);
        colorChips.forEach((color, chip) -> {
            boolean available = product.options().stream().anyMatch(o -> o.color().equals(color) && o.stock() > 0
                    && (size == null || o.size().equals(size)));
            if (!available && chip.isSelected()) {
                colorGroup.clearSelection();
            }
            setAvailable(chip, color, available);
        });
    }

    private static void setAvailable(JToggleButton chip, String label, boolean available) {
        chip.setEnabled(available);
        chip.setText(available ? label : label + " 품절");
        chip.getAccessibleContext().setAccessibleName(available ? label : label + ", 품절이라 고를 수 없음");
    }

    /** 고른 사이즈·색상의 옵션. 빠진 게 있으면 그 줄 옆에 이유를 쓰고 null. */
    private ProductOption chosenOption() {
        String size = selected(sizeChips);
        String color = selected(colorChips);
        sizeError.setText(size == null ? "사이즈를 골라 주세요" : "");
        colorError.setText(color == null ? "색상을 골라 주세요" : "");
        if (size == null || color == null) {
            (size == null ? sizeChips : colorChips).values().stream()
                    .filter(JToggleButton::isEnabled).findFirst().ifPresent(JComponent::requestFocusInWindow);
            return null;
        }
        return product.options().stream()
                .filter(o -> o.size().equals(size) && o.color().equals(color)).findFirst().orElse(null);
    }

    private static String selected(Map<String, JToggleButton> chips) {
        return chips.entrySet().stream().filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    // ── 버튼 (UC-06 4단계, 4a 로그인) ──

    /** [장바구니 담기]: 담은 뒤 장바구니로 갈지 계속 둘러볼지 묻는다. */
    private void addToCart() {
        actionError.setText("");
        ProductOption option = chosenOption();
        if (option == null || !loggedIn()) {
            return;
        }
        try {
            context.cart.add(new OrderLine(product.id(), option.id(), 1));
        } catch (LoginRequiredException e) {
            frame.navigate(LoginPanel.NAME);
            return;
        } catch (AppException e) {
            actionError.setText(e.getMessage()); // 예: 그사이 품절됨
            return;
        } catch (UnsupportedOperationException e) {
            JOptionPane.showMessageDialog(this, "장바구니 담기는 아직 연결되지 않았습니다.");
            return;
        }
        String[] choices = {"장바구니로 가기", "계속 쇼핑하기"};
        int answer = JOptionPane.showOptionDialog(this, "장바구니에 담았어요.", "장바구니",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, choices, choices[1]);
        if (answer == 0) {
            frame.navigate(CartPanel.NAME);
        }
    }

    /** [바로 구매]: 이 옵션 하나로 주문서를 만든다 (UC-11). */
    private void buyNow() {
        actionError.setText("");
        ProductOption option = chosenOption();
        if (option == null || !loggedIn()) {
            return;
        }
        try {
            context.order.prepareDirect(new OrderLine(product.id(), option.id(), 1));
        } catch (LoginRequiredException e) {
            frame.navigate(LoginPanel.NAME);
            return;
        } catch (AppException e) {
            actionError.setText(e.getMessage());
            return;
        } catch (UnsupportedOperationException e) {
            JOptionPane.showMessageDialog(this, "바로 구매는 아직 연결되지 않았습니다.");
            return;
        }
        frame.navigate(OrderPanel.NAME); // TODO 주문서 화면이 생기면 OrderSheet 를 넘긴다.
    }

    /** [AI로 입어보기]: 사이즈·색상과 상관없이 이 상품으로 입어 본다 (UC-09). */
    private void tryOn() {
        actionError.setText("");
        if (loggedIn()) {
            frame.navigate(TryOnPanel.NAME); // TODO AI 입어보기 화면이 생기면 product.id() 를 넘긴다.
        }
    }

    /** UC-06 4a: 비회원이면 로그인 화면으로 보낸다. 로그인하면 이 화면으로 돌아온다. */
    private boolean loggedIn() {
        if (context.member.currentMember() != null) {
            return true;
        }
        frame.navigate(LoginPanel.NAME);
        return false;
    }

    // ── 사이즈표, 리뷰 ──

    private JPanel createSizeChart() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setOpaque(false);
        Theme.addToCard(section, Theme.label("사이즈표", 1.3f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(section, Theme.label("사담다 표준 치수예요 (단위 cm). 상품마다 조금 다를 수 있어요.",
                1f, Font.PLAIN, Theme.HINT), 4);

        JPanel table = new JPanel(new GridLayout(SIZE_CHART.length, SIZE_CHART[0].length));
        table.setOpaque(false);
        table.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.TEXT));
        for (int row = 0; row < SIZE_CHART.length; row++) {
            for (String cell : SIZE_CHART[row]) {
                JLabel label = Theme.label(cell, 1f, row == 0 ? Font.BOLD : Font.PLAIN, Theme.TEXT);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setOpaque(row == 0);
                label.setBackground(Theme.PAGE);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                        BorderFactory.createEmptyBorder(12, 8, 12, 8)));
                table.add(label);
            }
        }
        Theme.addToCard(section, table, 16);
        return section;
    }

    /** 리뷰 목록: 별점, 작성자 체형 정보, 내용, 리뷰 사진 (FR-23 조회). */
    private JPanel createReviews() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setOpaque(false);
        if (reviews.isEmpty()) {
            Theme.addToCard(section, Theme.label("리뷰", 1.3f, Font.BOLD, Theme.TEXT), 0);
            Theme.addToCard(section, Theme.label("아직 리뷰가 없어요.", 1f, Font.PLAIN, Theme.HINT), 8);
            return section;
        }
        Theme.addToCard(section, Theme.label("리뷰 " + reviews.size() + "개  ★ " + averageRating(),
                1.3f, Font.BOLD, Theme.TEXT), 0);
        for (Review review : reviews) {
            Theme.addToCard(section, divider(), 24);
            Theme.addToCard(section, Theme.label(stars(review.rating()) + "  " + review.rating() + "점",
                    1f, Font.BOLD, Theme.TEXT), 24);
            String bodyInfo = bodyInfo(review.bodyProfile());
            if (bodyInfo != null) {
                Theme.addToCard(section, Theme.label(bodyInfo, 1f, Font.PLAIN, Theme.HINT), 4);
            }
            Theme.addToCard(section, text(review.content(), 1f, Font.PLAIN, Theme.TEXT), 8);
            if (review.photoUrls() != null && !review.photoUrls().isEmpty()) {
                JPanel photos = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                photos.setOpaque(false);
                for (int i = 0; i < review.photoUrls().size(); i++) {
                    ProductPhoto small = new ProductPhoto(review.photoUrls(), i, 120);
                    small.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
                    photos.add(small);
                }
                Theme.addToCard(section, photos, 12);
            }
        }
        return section;
    }

    private String averageRating() {
        double average = reviews.stream().mapToInt(Review::rating).average().orElse(0);
        return String.format(Locale.ROOT, "%.1f", average);
    }

    private static String stars(int rating) {
        return "★".repeat(rating) + "☆".repeat(Math.max(0, 5 - rating));
    }

    /** 리뷰 쓴 사람의 체형 정보 "50대 · 160cm · M (66) · 보통 체형". 없으면 null. */
    private static String bodyInfo(BodyProfile body) {
        if (body == null) {
            return null;
        }
        String joined = java.util.stream.Stream.of(body.ageGroup(),
                        body.heightCm() == null ? null : body.heightCm() + "cm", body.usualSize(), body.bodyType())
                .filter(java.util.Objects::nonNull).collect(Collectors.joining(" · "));
        return joined.isEmpty() ? null : joined;
    }

    // ── 작은 부품 ──

    private static String price(int won) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(won) + "원";
    }

    private static JPanel labelWithError(String text, JLabel error) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(Theme.label(text, 1f, Font.BOLD, Theme.TEXT));
        row.add(javax.swing.Box.createHorizontalStrut(16));
        row.add(error);
        return row;
    }

    /** 선택 버튼 줄. 폭이 모자라면 다음 줄로 넘어간다. */
    private static JPanel chipRow(Map<String, JToggleButton> chips) {
        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 8));
        row.setOpaque(false);
        chips.values().forEach(row::add);
        return row;
    }

    private static JComponent divider() {
        JPanel line = new JPanel();
        line.setBackground(Theme.LINE);
        line.setPreferredSize(new Dimension(1, 1));
        return line;
    }

    /** 폭에 맞춰 줄바꿈되는 글 (상품명, 리뷰 내용). 글씨 크기를 따라 커진다. */
    private static JTextArea text(String content, float scale, int style, Color color) {
        JTextArea area = new JTextArea(content) {
            @Override
            public void updateUI() {
                super.updateUI();
                Font base = UIManager.getFont("Label.font");
                setFont(base.deriveFont(style, base.getSize2D() * scale));
                // 기본 커서는 글 칸이 화면에 들어올 때 자기 쪽으로 화면을 끌어내린다. 읽기 전용이라 그 동작을 없앤다.
                // (글씨 크기를 바꾸면 UI 와 함께 커서도 새로 깔리므로 여기서 매번 바꿔 끼운다)
                setCaret(new DefaultCaret() {
                    @Override
                    protected void adjustVisibility(Rectangle nloc) {
                    }
                });
            }
        };
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setForeground(color);
        area.setBorder(null);
        area.setColumns(1); // 폭은 놓인 칸에 맞춘다
        return area;
    }

    /** 화면 전체. 폭은 창에 맞추고 세로로만 스크롤한다. */
    private static final class Body extends JPanel implements Scrollable {
        Body() {
            super(new GridBagLayout());
            setBackground(Theme.CARD);
            setBorder(BorderFactory.createEmptyBorder(16, 48, 64, 48));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height - 48;
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
