package com.sadamda.view.recommend;

import com.sadamda.controller.AppContext;
import com.sadamda.model.attendance.AttendanceService;
import com.sadamda.model.attendance.AttendanceWeek;
import com.sadamda.model.product.Product;
import com.sadamda.model.recommend.OutfitSuggestion;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ProductThumbnail;
import com.sadamda.view.Theme;
import com.sadamda.view.member.LoginPanel;
import com.sadamda.view.product.ShopUi;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 홈 화면의 "옷장 출석" 칸 (UC-21): 이번 주 월~일 7칸, [오늘 출석하고 코디 받기], 무료배송 쿠폰 수.
 * 출석한 날 칸에는 그날 코디의 상의 사진이 보이고, 누르면 그날의 코디 창(AttendanceDialog)이 열린다.
 * 비회원에게는 로그인 안내만 보여준다.
 */
class AttendanceRow extends JPanel {
    private final AppContext context;
    private final MainFrame frame;

    AttendanceRow(AppContext context, MainFrame frame) {
        super(new BorderLayout());
        this.context = context;
        this.frame = frame;
        setOpaque(false);
    }

    void refresh() {
        removeAll();
        add(context.attendance.isLoggedIn() ? memberView() : guestView(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JComponent guestView() {
        JPanel card = ShopUi.card(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.add(ShopUi.label("로그인하면 매일 오늘의 코디를 받고 출석 도장을 모을 수 있어요. 한 주에 "
                + AttendanceService.COUPON_DAYS + "칸을 채우면 무료배송 쿠폰을 드려요.", 1.05f, Font.PLAIN, ShopUi.TEXT),
                BorderLayout.CENTER);
        JButton login = ShopUi.secondaryButton("로그인하기");
        login.addActionListener(e -> frame.navigate(LoginPanel.NAME));
        card.add(login, BorderLayout.EAST);
        return card;
    }

    private JComponent memberView() {
        AttendanceWeek week;
        try {
            week = context.attendance.week();
        } catch (RuntimeException e) {
            return ShopUi.label("출석 정보를 불러오지 못했어요. " + e.getMessage(), 1f, Font.PLAIN, ShopUi.ERROR);
        }

        JPanel days = new JPanel(new GridLayout(1, 7, 10, 0));
        days.setOpaque(false);
        for (LocalDate day : week.days()) {
            days.add(dayCell(week, day));
        }

        String progress = week.couponDoneThisWeek()
                ? "이번 주 " + week.stampedCount() + "/7칸 · 이번 주 무료배송 쿠폰을 받았어요!"
                : "이번 주 " + week.stampedCount() + "/7칸 · " + week.daysLeftForCoupon() + "칸 더 채우면 무료배송 쿠폰";
        JLabel status = ShopUi.label(progress + "   (가진 쿠폰 " + week.coupons() + "장)", 1.05f, Font.BOLD, ShopUi.TEXT);

        JButton action = week.checkedToday() ? ShopUi.secondaryButton("오늘의 코디 다시 보기")
                : ShopUi.primaryButton("오늘 출석하고 코디 받기");
        action.addActionListener(e -> {
            if (week.checkedToday()) {
                openDay(week.today(), null);
            } else {
                checkIn();
            }
        });

        JPanel bottom = new JPanel(new BorderLayout(16, 0));
        bottom.setOpaque(false);
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(action, BorderLayout.EAST);

        JPanel card = ShopUi.card(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.add(days, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    /** 하루 칸: 요일·날짜와, 출석했으면 그날 코디의 첫 옷 사진. */
    private JComponent dayCell(AttendanceWeek week, LocalDate day) {
        boolean stamped = week.isStamped(day);
        boolean today = day.equals(week.today());
        JPanel cell = new JPanel(new BorderLayout(0, 6));
        cell.setBackground(stamped ? ShopUi.CARD : ShopUi.PAGE);
        Color edge = today ? Theme.PRIMARY : Theme.LINE;
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(edge, today ? 2 : 1),
                BorderFactory.createEmptyBorder(8, 6, 8, 6)));

        String name = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        JLabel head = ShopUi.label(name + " " + day.getDayOfMonth() + "일" + (today ? " (오늘)" : ""),
                1f, Font.BOLD, day.getDayOfWeek() == DayOfWeek.SUNDAY ? ShopUi.ERROR : ShopUi.TEXT);
        head.setHorizontalAlignment(SwingConstants.CENTER);
        cell.add(head, BorderLayout.NORTH);

        if (stamped) {
            OutfitSuggestion outfit = outfitOrNull(day);
            Product first = outfit == null ? null : outfit.products().stream()
                    .filter(p -> p.getCategory() != com.sadamda.model.product.Category.HAT)
                    .findFirst().orElse(null);
            JPanel photo = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            photo.setOpaque(false);
            if (first != null) {
                photo.add(new ProductThumbnail(first.mainImageUrl()));
            }
            cell.add(photo, BorderLayout.CENTER);
            JLabel stampLabel = ShopUi.label("출석 ✔", 1f, Font.BOLD, ShopUi.TEXT);
            stampLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cell.add(stampLabel, BorderLayout.SOUTH);
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cell.setToolTipText("눌러서 이날의 코디 보기");
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    openDay(day, null);
                }
            });
        } else {
            String text = today ? "아직 안 했어요" : day.isBefore(week.today()) ? "쉬었어요" : " ";
            JLabel empty = ShopUi.label(text, 1f, Font.PLAIN, ShopUi.HINT);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            cell.add(empty, BorderLayout.CENTER);
        }
        return cell;
    }

    private OutfitSuggestion outfitOrNull(LocalDate day) {
        try {
            return context.attendance.outfitOf(day);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** UC-21 2~4단계 오늘 출석 → 도장 → 오늘의 코디 창. 쿠폰을 받았으면 함께 알린다. */
    private void checkIn() {
        AttendanceWeek week;
        try {
            week = context.attendance.checkIn();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "출석하지 못했어요.\n" + e.getMessage());
            return;
        }
        refresh();
        String message = "출석 도장을 찍었어요! 이번 주 " + week.stampedCount() + "/7칸";
        if (week.couponJustEarned()) {
            message += "\n이번 주 " + AttendanceService.COUPON_DAYS + "칸을 채워 무료배송 쿠폰 1장을 드렸어요. "
                    + "주문서에서 쓸 수 있어요.";
        }
        openDay(week.today(), message);
    }

    private void openDay(LocalDate day, String message) {
        OutfitSuggestion outfit = outfitOrNull(day);
        if (outfit == null) {
            if (message != null) {
                JOptionPane.showMessageDialog(this, message);
            }
            return;
        }
        new AttendanceDialog(SwingUtilities.getWindowAncestor(this), context, frame, outfit, message).setVisible(true);
    }
}
