package com.sadamda.view.family;

import com.sadamda.common.AppException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.family.AskRequest;
import com.sadamda.model.order.Order;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ProductThumbnail;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.Toast;
import com.sadamda.view.cart.CartPanel;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.product.ProductListPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 가족 쇼핑: 가족이 나에게 물어본 옷(답하기·대신 결제하기)과 내가 가족에게 물어본 옷(답 확인·장바구니 담기).
 * 사담다의 차별 기능 "가족에게 물어보기"의 화면.
 */
public class FamilyPanel extends ScreenPanel {
    public static final String NAME = "family";

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final String[] PAYMENT_METHODS = {"카드 결제", "계좌이체", "휴대폰 결제"};
    private static final Color LIKED = new Color(0x1B5E20);   // 흰 배경 대비 7:1 이상
    private static final Color DISLIKED = new Color(0x8D3B00);

    private final JPanel column = new JPanel(new GridBagLayout()) {
        @Override
        public Dimension getPreferredSize() {
            float fontSize = UIManager.getFont("Label.font").getSize2D();
            return new Dimension(Math.max(680, Math.round(fontSize * 42)), super.getPreferredSize().height);
        }
    };
    private final JScrollPane page;

    public FamilyPanel(AppContext context, MainFrame frame) {
        super(context, frame, "가족 쇼핑", "가족에게 물어보기");
        hideTitle();
        setBackground(Theme.PAGE);
        setBorder(BorderFactory.createEmptyBorder());
        column.setOpaque(false);
        page = Theme.centeredPage(column);
        page.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        setContent(page);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (context.member.currentMember() == null) {
                    frame.navigate(LoginPanel.NAME);
                    return;
                }
                render();
            }
        });
    }

    private void render() {
        column.removeAll();
        JLabel title = Theme.label("가족과 함께 고르기", 1.6f, Font.BOLD, Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.addToCard(column, title, 0);
        JLabel intro = Theme.label("고른 옷을 가족에게 \"어때?\" 물어보고, 가족이 대신 결제해 줄 수도 있어요.",
                1f, Font.PLAIN, Theme.HINT);
        intro.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.addToCard(column, intro, 8);

        List<AskRequest> toMe = context.family.asksToMe();
        List<AskRequest> mine = context.family.myAsks();
        if (!toMe.isEmpty()) {
            Theme.addToCard(column, section("가족이 물어본 옷", toMe.size()), 32);
            for (AskRequest request : toMe) {
                Theme.addToCard(column, incomingCard(request), 12);
            }
        }
        Theme.addToCard(column, section("내가 물어본 옷", mine.size()), 32);
        if (mine.isEmpty()) {
            Theme.addToCard(column, emptyCard(), 12);
        }
        for (AskRequest request : mine) {
            Theme.addToCard(column, outgoingCard(request), 12);
        }
        frame.refreshFamilyBadge(); // 열어 보면 새 답 표시가 지워진다
        column.revalidate();
        column.repaint();
        page.getViewport().setViewPosition(new Point(0, 0));
    }

    private static JLabel section(String text, int count) {
        return Theme.label(text + " " + count + "건", 1.25f, Font.BOLD, Theme.TEXT);
    }

    private JPanel emptyCard() {
        JPanel card = Theme.card();
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        Theme.addToCard(card, Theme.label("아직 물어본 옷이 없어요", 1.1f, Font.BOLD, Theme.TEXT), 0);
        Theme.addToCard(card, Theme.label("상품 상세나 장바구니에서 [가족에게 물어보기]를 눌러 보세요.",
                1f, Font.PLAIN, Theme.HINT), 8);
        JButton browse = Theme.secondaryButton("상품 보러 가기");
        browse.addActionListener(e -> frame.navigate(ProductListPanel.NAME));
        Theme.addToCard(card, browse, 16);
        return card;
    }

    // ── 가족이 나에게 물어본 옷 ──

    private JPanel incomingCard(AskRequest request) {
        JPanel card = baseCard(request, context.family.nameOf(request.getRequesterId()) + "님이 물어봤어요");
        JTextField reply = Theme.textField("한마디 남기기 (선택) 예: 색이 정말 잘 어울려요!");
        switch (request.getStatus()) {
            case WAITING -> {
                Theme.addToCard(card, reply, 20);
                JButton like = Theme.primaryButton("좋아요");
                like.addActionListener(e -> respond(request, true, reply.getText()));
                JButton dislike = Theme.secondaryButton("다른 걸 추천해요");
                dislike.addActionListener(e -> respond(request, false, reply.getText()));
                Theme.addToCard(card, twoButtons(dislike, like), 12);
                Theme.addToCard(card, payButton(request, reply), 12);
            }
            case LIKED, DISLIKED -> {
                Theme.addToCard(card, answerLabel(request, "내 답"), 20);
                Theme.addToCard(card, payButton(request, reply), 16);
            }
            case PAID -> Theme.addToCard(card, Theme.label("대신 결제했어요. 선물로 보내 드려요.",
                    1.05f, Font.BOLD, LIKED), 20);
        }
        return card;
    }

    private JButton payButton(AskRequest request, JTextField reply) {
        JButton pay = Theme.secondaryButton("대신 결제해 드리기");
        pay.addActionListener(e -> payFor(request, reply.getText(), pay));
        return pay;
    }

    private void respond(AskRequest request, boolean liked, String reply) {
        try {
            context.family.respond(request.getId(), liked, reply);
        } catch (AppException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "답하지 못했어요", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Toast.show(this, context.family.nameOf(request.getRequesterId()) + "님에게 답을 보냈어요");
        render();
    }

    /** 결제 수단을 고르고, 묻는 사람에게 선물 주문으로 결제한다. 결제 대기는 화면 스레드 밖에서. */
    private void payFor(AskRequest request, String reply, JButton button) {
        String name = context.family.nameOf(request.getRequesterId());
        int total;
        try {
            total = context.family.totalOf(request);
        } catch (AppException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "결제할 수 없어요", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object method = JOptionPane.showInputDialog(this,
                name + "님께 선물로 보내 드려요.\n결제 금액 " + price(total) + " (배송비 포함)\n결제 수단을 골라 주세요.",
                "대신 결제해 드리기", JOptionPane.PLAIN_MESSAGE, null, PAYMENT_METHODS, PAYMENT_METHODS[0]);
        if (method == null) {
            return;
        }
        button.setEnabled(false);
        button.setText("결제 중…");
        new SwingWorker<Order, Void>() {
            @Override
            protected Order doInBackground() {
                return context.family.payFor(request.getId(), (String) method, reply);
            }

            @Override
            protected void done() {
                try {
                    get();
                    Toast.show(FamilyPanel.this, "결제했어요. " + name + "님께 선물로 보내 드려요");
                } catch (ExecutionException e) {
                    String message = e.getCause() instanceof AppException
                            ? e.getCause().getMessage() : "결제 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.";
                    JOptionPane.showMessageDialog(FamilyPanel.this, message, "결제하지 못했어요",
                            JOptionPane.WARNING_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                render();
            }
        }.execute();
    }

    // ── 내가 물어본 옷 ──

    private JPanel outgoingCard(AskRequest request) {
        String name = context.family.nameOf(request.getResponderId());
        JPanel card = baseCard(request, name + "님에게 물어봤어요");
        switch (request.getStatus()) {
            case WAITING -> Theme.addToCard(card, Theme.label("답을 기다리고 있어요", 1.05f, Font.PLAIN, Theme.HINT), 20);
            case LIKED, DISLIKED -> {
                Theme.addToCard(card, answerLabel(request, name + "님의 답"), 20);
                JButton toCart = Theme.secondaryButton("장바구니에 담기");
                toCart.addActionListener(e -> addToCart(request));
                Theme.addToCard(card, toCart, 16);
            }
            case PAID -> Theme.addToCard(card, Theme.label("<html>" + name + "님이 대신 결제해 줬어요!"
                    + (request.getReply() == null || request.getReply().isBlank() ? ""
                    : "<br>“" + request.getReply() + "”") + "<br>주문 내역에서 배송을 확인할 수 있어요.</html>",
                    1.05f, Font.BOLD, LIKED), 20);
        }
        return card;
    }

    private void addToCart(AskRequest request) {
        try {
            for (OrderLine line : request.getLines()) {
                context.cart.add(line);
            }
        } catch (AppException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "담지 못했어요", JOptionPane.WARNING_MESSAGE);
            return;
        }
        frame.refreshCartBadge();
        Toast.show(this, "장바구니에 담았어요", "장바구니 보기", () -> frame.navigate(CartPanel.NAME));
    }

    // ── 공통 ──

    /** 누가·언제 + 하고 싶은 말 + 상품 줄 + 합계. */
    private JPanel baseCard(AskRequest request, String who) {
        JPanel card = Theme.card();
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Theme.label(who, 1.1f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        head.add(Theme.label(request.getAskedAt().format(TIME), 1f, Font.PLAIN, Theme.HINT), BorderLayout.EAST);
        Theme.addToCard(card, head, 0);
        if (!request.getMessage().isBlank()) {
            JLabel bubble = Theme.label("<html>“" + request.getMessage() + "”</html>", 1.05f, Font.PLAIN, Theme.TEXT);
            bubble.setOpaque(true);
            bubble.setBackground(Theme.SECONDARY);
            bubble.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            Theme.addToCard(card, bubble, 12);
        }
        int sum = 0;
        for (OrderLine line : request.getLines()) {
            Product product = productOrNull(line.productId());
            if (product == null) {
                continue;
            }
            sum += product.getPrice() * line.quantity();
            Theme.addToCard(card, itemRow(product, line), 16);
        }
        Theme.addToCard(card, Theme.label("상품 합계 " + price(sum), 1.1f, Font.BOLD, Theme.TEXT), 16);
        return card;
    }

    private Product productOrNull(Long id) {
        try {
            return context.product.detail(id);
        } catch (AppException e) {
            return null;
        }
    }

    private JPanel itemRow(Product product, OrderLine line) {
        ProductOption option = product.option(line.optionId());
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        ProductThumbnail thumb = new ProductThumbnail(product.getMainImageUrl());
        thumb.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        thumb.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                frame.openProduct(product.getId());
            }
        });
        row.add(thumb, BorderLayout.WEST);
        JPanel text = new JPanel(new GridBagLayout());
        text.setOpaque(false);
        Theme.addToCard(text, Theme.label(product.getName(), 1f, Font.BOLD, Theme.TEXT), 0);
        String optionText = option == null ? "" : option.size() + " / " + option.color() + " · ";
        Theme.addToCard(text, Theme.label(optionText + line.quantity() + "개", 1f, Font.PLAIN, Theme.HINT), 6);
        row.add(text, BorderLayout.CENTER);
        row.add(Theme.label(price(product.getPrice() * line.quantity()), 1.05f, Font.BOLD, Theme.TEXT),
                BorderLayout.EAST);
        return row;
    }

    private static JLabel answerLabel(AskRequest request, String whose) {
        boolean liked = request.getStatus() == AskRequest.Status.LIKED;
        String verdict = liked ? "♥ 좋아요" : "다른 걸 추천해요";
        String reply = request.getReply() == null || request.getReply().isBlank() ? "" : "<br>“" + request.getReply() + "”";
        return Theme.label("<html>" + whose + ": <b>" + verdict + "</b>" + reply + "</html>", 1.05f, Font.PLAIN,
                liked ? LIKED : DISLIKED);
    }

    private static JPanel twoButtons(JButton left, JButton right) {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.add(left);
        row.add(right);
        return row;
    }

    private static String price(int won) {
        return String.format("%,d원", won);
    }
}
