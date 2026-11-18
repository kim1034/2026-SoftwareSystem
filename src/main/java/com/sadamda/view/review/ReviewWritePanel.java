package com.sadamda.view.review;

import com.sadamda.common.AppException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderItem;
import com.sadamda.model.product.ProductOption;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ProductThumbnail;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.WrapLayout;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.member.MyInfoPanel;
import com.sadamda.view.order.OrderHistoryPanel;
import com.sadamda.view.product.ProductDetail;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * UC-14 리뷰 작성: 주문 내역의 [리뷰 쓰기]로 연다 (MainFrame.openReview).
 * 별점(필수, 3a), 내용(선택, 500자), 사진(선택, 3장, JPG·PNG), 체형 정보(회원 정보에서 자동, [내 정보에서 바꾸기]).
 * 등록하면 그 상품의 상세 화면에서 리뷰를 보여 준다 (4단계).
 */
public class ReviewWritePanel extends ScreenPanel {
    public static final String NAME = "reviewWrite";
    private static final int MAX_CONTENT = 500;
    private static final int MAX_PHOTOS = 3;

    private final JPanel column = new JPanel(new GridBagLayout()) {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(columnWidth(), super.getPreferredSize().height);
        }
    };
    private final JScrollPane page;
    private final JButton submitButton = Theme.primaryButton("리뷰 등록");
    private final JPanel submitBar = new JPanel(new GridBagLayout());

    private Order order;
    private Long productId;
    private int rating;
    private final List<String> photos = new ArrayList<>();
    private final JTextArea contentArea = Theme.textArea("상품을 받아 보신 느낌을 자유롭게 적어 주세요 (선택)", 5);
    private final JLabel contentCount = Theme.label("0 / " + MAX_CONTENT + "자", 1f, Font.PLAIN, Theme.HINT);
    private final JLabel ratingError = Theme.errorLabel();
    private final JLabel photoError = Theme.errorLabel();
    private final List<StarButton> stars = new ArrayList<>();
    private final JLabel ratingText = Theme.label(" ", 1.1f, Font.BOLD, Theme.TEXT);

    public ReviewWritePanel(AppContext context, MainFrame frame) {
        super(context, frame, "리뷰 쓰기", "UC-14 리뷰 작성");
        hideTitle();
        setBackground(Theme.PAGE);
        setBorder(BorderFactory.createEmptyBorder());
        column.setOpaque(false);
        page = Theme.centeredPage(column);
        page.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        submitButton.addActionListener(e -> submit());
        submitBar.setBackground(Theme.CARD);
        submitBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)));
        submitBar.add(submitButton);
        JPanel screen = new JPanel(new BorderLayout());
        screen.setOpaque(false);
        screen.add(page, BorderLayout.CENTER);
        screen.add(submitBar, BorderLayout.SOUTH);
        setContent(screen);

        // 내용은 500자까지만 들어가고, 쓴 글자 수를 보여 준다.
        ((AbstractDocument) contentArea.getDocument()).setDocumentFilter(new LengthLimit(MAX_CONTENT));
        contentArea.getAccessibleContext().setAccessibleName("리뷰 내용");
        contentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCount();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCount();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCount();
            }
        });
        for (int i = 1; i <= 5; i++) {
            StarButton star = new StarButton(i);
            star.addActionListener(e -> setRating(star.value));
            stars.add(star);
        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (context.member.currentMember() == null) {
                    frame.navigate(LoginPanel.NAME); // 로그인하면 이 화면으로 돌아온다
                } else {
                    render(); // 내 정보에서 체형을 바꾸고 돌아왔을 수 있어 다시 그린다
                }
            }
        });
        column.addPropertyChangeListener("font", e -> SwingUtilities.invokeLater(this::fitSubmitButton));
    }

    /** UC-14 1단계: 이 주문의 상품 중 아직 리뷰를 안 쓴 첫 상품으로 연다. */
    public void showOrder(Long orderId) {
        Member member = context.member.currentMember();
        order = member == null ? null : context.order.orderDetail(orderId);
        productId = order == null ? null : order.getItems().stream().map(OrderItem::productId)
                .filter(id -> !context.review.hasWritten(orderId, id)).findFirst()
                .orElse(order.getItems().get(0).productId());
        resetForm();
        render();
    }

    private void resetForm() {
        setRating(0);
        contentArea.setText("");
        photos.clear();
        ratingError.setText("");
        photoError.setText("");
    }

    private void render() {
        column.removeAll();
        JButton back = Theme.linkButton("← 주문 내역으로");
        back.addActionListener(e -> frame.navigate(OrderHistoryPanel.NAME));
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.setOpaque(false);
        backRow.add(back);
        Theme.addToCard(column, backRow, 0);
        JLabel title = Theme.label("리뷰 쓰기", 1.6f, Font.BOLD, Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.addToCard(column, title, 8);

        if (order == null) {
            Theme.addToCard(column, Theme.label("리뷰를 쓸 주문을 주문 내역에서 골라 주세요.", 1f, Font.PLAIN, Theme.HINT), 24);
            submitBar.setVisible(false);
        } else {
            Theme.addToCard(column, productCard(), 24);
            boolean done = context.review.hasWritten(order.getId(), productId);
            if (done) {
                Theme.addToCard(column, Theme.label("이 상품은 리뷰를 이미 썼어요. 상품 상세에서 볼 수 있어요.",
                        1f, Font.PLAIN, Theme.HINT), 24);
            } else {
                Theme.addToCard(column, formCard(), 16);
            }
            submitBar.setVisible(!done);
        }
        fitSubmitButton();
        column.revalidate();
        column.repaint();
        page.getViewport().setViewPosition(new Point(0, 0));
        // 여러 줄로 접히는 버튼 줄(상품 고르기·사진)은 폭이 정해진 뒤에야 높이가 맞으므로 한 번 더 배치한다.
        SwingUtilities.invokeLater(() -> {
            column.revalidate();
            column.repaint();
        });
    }

    /** 리뷰 쓸 상품. 주문에 상품이 여러 개면 고르는 버튼을 보여 주고, 이미 쓴 상품은 "작성함"으로 막는다. */
    private JPanel productCard() {
        JPanel card = whiteCard();
        OrderItem line = order.getItems().stream().filter(l -> l.productId().equals(productId)).findFirst()
                .orElseThrow();
        ProductDetail product = ProductDetail.of(context.product.detail(productId));
        ProductOption option = product.options().stream()
                .filter(o -> o.id().equals(line.optionId())).findFirst().orElseThrow();
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.add(new ProductThumbnail(product.imageUrls().get(0)), BorderLayout.WEST);
        JPanel text = new JPanel(new GridBagLayout());
        text.setOpaque(false);
        Theme.addToCard(text, Theme.label(product.name(), 1.05f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(text, Theme.label(option.size() + " / " + option.color(), 1f, Font.PLAIN, Theme.HINT), 6);
        row.add(text, BorderLayout.CENTER);
        Theme.addToCard(card, row, 0);

        if (order.getItems().size() > 1) {
            Theme.addToCard(card, Theme.label("어떤 상품의 리뷰를 쓸까요?", 1f, Font.BOLD, Theme.TEXT), 20);
            JPanel chips = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 8));
            chips.setOpaque(false);
            ButtonGroup group = new ButtonGroup();
            for (OrderItem each : order.getItems()) {
                String name = context.product.detail(each.productId()).getName();
                boolean written = context.review.hasWritten(order.getId(), each.productId());
                JToggleButton chip = Theme.chip(written ? name + " (작성함)" : name);
                chip.setEnabled(!written || each.productId().equals(productId));
                chip.setSelected(each.productId().equals(productId));
                chip.addActionListener(e -> {
                    productId = each.productId();
                    resetForm();
                    render();
                });
                group.add(chip);
                chips.add(chip);
            }
            Theme.addToCard(card, chips, 4);
        }
        return card;
    }

    /** 별점, 내용, 사진, 내 체형 (UC-14 2단계). */
    private JPanel formCard() {
        JPanel card = whiteCard();
        JPanel ratingHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ratingHeader.setOpaque(false);
        ratingHeader.add(Theme.label("별점 (필수)", 1f, Font.BOLD, Theme.TEXT));
        ratingHeader.add(javax.swing.Box.createHorizontalStrut(16));
        ratingHeader.add(ratingError);
        Theme.addToCard(card, ratingHeader, 0);
        JPanel starRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        starRow.setOpaque(false);
        stars.forEach(star -> {
            SwingUtilities.updateComponentTreeUI(star);
            starRow.add(star);
        });
        starRow.add(javax.swing.Box.createHorizontalStrut(12));
        SwingUtilities.updateComponentTreeUI(ratingText);
        starRow.add(ratingText);
        Theme.addToCard(card, starRow, 8);

        JPanel contentHeader = new JPanel(new BorderLayout());
        contentHeader.setOpaque(false);
        contentHeader.add(Theme.label("내용 (선택)", 1f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        SwingUtilities.updateComponentTreeUI(contentCount);
        contentHeader.add(contentCount, BorderLayout.EAST);
        Theme.addToCard(card, contentHeader, 28);
        SwingUtilities.updateComponentTreeUI(contentArea);
        Theme.addToCard(card, contentArea, 8);

        JPanel photoHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        photoHeader.setOpaque(false);
        photoHeader.add(Theme.label("사진 (선택, " + MAX_PHOTOS + "장까지, JPG·PNG)", 1f, Font.BOLD, Theme.TEXT));
        photoHeader.add(javax.swing.Box.createHorizontalStrut(16));
        SwingUtilities.updateComponentTreeUI(photoError);
        photoHeader.add(photoError);
        Theme.addToCard(card, photoHeader, 28);
        Theme.addToCard(card, photoRow(), 8);

        Theme.addToCard(card, Theme.label("내 체형 (리뷰에 함께 보여요)", 1f, Font.BOLD, Theme.TEXT), 28);
        String body = bodyInfo(context.member.currentMember().getBodyProfile());
        JPanel bodyRow = new JPanel(new BorderLayout(16, 0));
        bodyRow.setOpaque(false);
        bodyRow.add(Theme.label(body == null ? "아직 체형 정보가 없어요" : body, 1f, Font.PLAIN,
                body == null ? Theme.HINT : Theme.TEXT), BorderLayout.CENTER);
        JButton editBody = Theme.linkButton("내 정보에서 바꾸기");
        editBody.addActionListener(e -> frame.navigate(MyInfoPanel.NAME));
        bodyRow.add(editBody, BorderLayout.EAST);
        Theme.addToCard(card, bodyRow, 4);
        return card;
    }

    /** 고른 사진 작은 그림들([빼기])과 [+ 사진 추가]. */
    private JPanel photoRow() {
        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 8));
        row.setOpaque(false);
        for (String path : photos) {
            JPanel item = new JPanel(new BorderLayout(0, 4));
            item.setOpaque(false);
            item.add(new ProductThumbnail(path), BorderLayout.CENTER);
            JButton remove = Theme.linkButton("빼기");
            remove.getAccessibleContext().setAccessibleName(new File(path).getName() + " 사진 빼기");
            remove.addActionListener(e -> {
                photos.remove(path);
                photoError.setText("");
                render();
            });
            item.add(remove, BorderLayout.SOUTH);
            row.add(item);
        }
        if (photos.size() < MAX_PHOTOS) {
            JButton add = Theme.secondaryButton("+ 사진 추가");
            add.addActionListener(e -> choosePhotos());
            row.add(add);
        }
        return row;
    }

    private void choosePhotos() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("리뷰 사진 고르기");
        chooser.setMultiSelectionEnabled(true);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("사진 (JPG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        photoError.setText("");
        for (File file : chooser.getSelectedFiles()) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
                photoError.setText("JPG나 PNG 사진만 올릴 수 있어요");
                continue;
            }
            if (photos.size() >= MAX_PHOTOS) {
                photoError.setText("사진은 " + MAX_PHOTOS + "장까지 올릴 수 있어요");
                break;
            }
            if (!photos.contains(file.getAbsolutePath())) {
                photos.add(file.getAbsolutePath());
            }
        }
        render();
    }

    // ── 등록 (3~4단계, 3a) ──

    private void submit() {
        if (rating == 0) {
            ratingError.setText("별점을 골라 주세요"); // 3a
            stars.get(0).scrollRectToVisible(stars.get(0).getBounds());
            stars.get(0).requestFocusInWindow();
            return;
        }
        Member member = context.member.currentMember();
        try {
            fetchWrite(member, contentArea.getText().trim());
        } catch (AppException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "리뷰를 등록하지 못했어요", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "리뷰를 등록했어요.\n상품 상세에서 확인할 수 있어요.");
        Long registered = productId;
        resetForm();
        frame.openProductReviews(registered); // 4단계: 상품 상세의 리뷰로
    }

    /** 체형 정보는 Model 이 회원 정보에서 채우므로 넘기지 않는다. */
    private void fetchWrite(Member member, String content) {
        context.review.write(order.getId(), productId, rating, content.isEmpty() ? null : content, photos);
    }

    // ── 작은 부품 ──

    private void setRating(int value) {
        rating = value;
        ratingText.setText(value == 0 ? " " : value + "점");
        if (value > 0) {
            ratingError.setText("");
        }
        stars.forEach(star -> star.repaint());
    }

    private void updateCount() {
        contentCount.setText(contentArea.getDocument().getLength() + " / " + MAX_CONTENT + "자");
    }

    private void fitSubmitButton() {
        submitButton.setPreferredSize(null);
        submitButton.setPreferredSize(new Dimension(columnWidth(), submitButton.getPreferredSize().height));
        submitBar.revalidate();
    }

    private static int columnWidth() {
        float fontSize = UIManager.getFont("Label.font").getSize2D();
        return Math.max(640, Math.round(fontSize * 40));
    }

    private static JPanel whiteCard() {
        JPanel card = Theme.card();
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return card;
    }

    /** "50대 · 160cm · M (66) · 보통 체형". 하나도 없으면 null. */
    private static String bodyInfo(BodyProfile body) {
        if (body == null) {
            return null;
        }
        String joined = Stream.of(body.ageGroup(), body.heightCm() == null ? null : body.heightCm() + "cm",
                body.usualSize(), body.bodyType()).filter(Objects::nonNull).collect(Collectors.joining(" · "));
        return joined.isEmpty() ? null : joined;
    }

    /** 별 하나. 고른 별점까지는 검정 ★, 나머지는 회색 ☆. 누르는 영역 48px 이상 (NFR-02). */
    private final class StarButton extends JButton {
        private final int value;

        StarButton(int value) {
            this.value = value;
            getAccessibleContext().setAccessibleName("별점 " + value + "점");
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
        }

        @Override
        public Dimension getPreferredSize() {
            int size = Math.max(48, Math.round(UIManager.getFont("Label.font").getSize2D() * 2.8f));
            return new Dimension(size, size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean on = value <= rating;
            String glyph = on ? "★" : "☆";
            g2.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, getHeight() * 0.7f));
            g2.setColor(on ? Theme.TEXT : Theme.HINT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(glyph, (getWidth() - fm.stringWidth(glyph)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            if (isFocusOwner()) {
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 12, 12));
            }
            g2.dispose();
        }
    }

    /** 글자 수 제한. 붙여 넣은 글이 길면 넘치는 부분은 잘라 넣는다. */
    private static final class LengthLimit extends DocumentFilter {
        private final int max;

        LengthLimit(int max) {
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            int room = max - (fb.getDocument().getLength() - length);
            String fitted = text == null ? null : text.substring(0, Math.max(0, Math.min(text.length(), room)));
            super.replace(fb, offset, length, fitted, attrs);
        }
    }
}
