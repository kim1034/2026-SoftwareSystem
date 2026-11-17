package com.sadamda.view.product;

import com.sadamda.common.AccessDeniedException;
import com.sadamda.common.AppException;
import com.sadamda.common.LoginRequiredException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Product;
import com.sadamda.view.MainFrame;
import com.sadamda.view.member.LoginPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 상품·룩북·홈 화면이 함께 쓰는 화면 도구: 색, 글씨, 버튼, 상품 열기, 오류 표시.
 *
 * 색 값은 공통 디자인(view/Theme, 강준서 feat/product-view 의 흑백 테마)과 같은 값으로 맞췄다.
 * 브랜치를 합친 뒤 아래 색 상수를 Theme 상수로 바꿔도 화면은 그대로다.
 * 글씨 크기 조절(UC-15) 때 updateComponentTreeUI 가 UI 를 다시 깔기 때문에, 글꼴은 updateUI 에서 정한다.
 */
public final class ShopUi {
    public static final Color PAGE = new Color(0xF2F2F2);      // 화면 배경
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(0x1F1F1F);      // 흰 배경 대비 16.6:1 (NFR-01)
    public static final Color HINT = new Color(0x666666);      // 흰 배경 대비 5.7:1
    public static final Color LINE = new Color(0xD4D4D4);
    public static final Color PRIMARY = new Color(0x262626);   // 흰 글씨 대비 15.1:1
    public static final Color PRIMARY_SOFT = new Color(0xE6E6E6);
    public static final Color SECONDARY = new Color(0xE6E6E6);
    public static final Color ERROR = new Color(0xC62828);
    public static final Color TREND = new Color(0x262626);     // "유행" 표시. 흑백 테마라 검정

    /** 버튼·누르는 영역 최소 높이 (NFR-02). */
    public static final int MIN_TOUCH = 48;

    private static final NumberFormat PRICE = NumberFormat.getNumberInstance(Locale.KOREA);

    private ShopUi() {
    }

    // ---------- 글씨 ----------

    /** 기본 글씨 크기의 scale 배 크기 라벨. 글씨 크기를 바꾸면 함께 바뀐다. */
    public static JLabel label(String text, float scale, int style, Color color) {
        JLabel label = new JLabel(text) {
            @Override
            public void updateUI() {
                super.updateUI();
                Font base = UIManager.getFont("Label.font");
                setFont(base.deriveFont(style, base.getSize2D() * scale));
            }
        };
        label.setForeground(color);
        return label;
    }

    public static JLabel title(String text) {
        return label(text, 1.45f, Font.BOLD, TEXT);
    }

    public static JLabel hint(String text) {
        return label(text, 0.95f, Font.PLAIN, HINT);
    }

    /** 158000 → "158,000원" */
    public static String price(int won) {
        return PRICE.format(won) + "원";
    }

    /** HTML 특수 문자를 바꾼다. 상품명 등 데이터 글자를 JLabel HTML 에 넣을 때 쓴다. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ---------- 버튼 ----------

    /** 파란 바탕 흰 글씨 버튼 (가장 중요한 동작). */
    public static JButton primaryButton(String text) {
        return new RoundButton(text, PRIMARY, Color.WHITE, PRIMARY);
    }

    /** 흰 바탕 파란 테두리 버튼. */
    public static JButton secondaryButton(String text) {
        return new RoundButton(text, CARD, PRIMARY, PRIMARY);
    }

    /** 카테고리처럼 여러 개 중 하나를 고르는 버튼. 고른 상태는 setSelected 로 바꾼다. */
    public static JButton choiceButton(String text) {
        return new RoundButton(text, CARD, TEXT, LINE);
    }

    /** 둥근 모서리 버튼. 높이 48px 이상, 글씨는 본문 +1px, 키보드 포커스가 오면 굵은 테두리. */
    private static final class RoundButton extends JButton {
        private final Color background;
        private final Color foreground;
        private final Color border;

        RoundButton(String text, Color background, Color foreground, Color border) {
            super(text);
            this.background = background;
            this.foreground = foreground;
            this.border = border;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(foreground);
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
            Font base = UIManager.getFont("Button.font");
            setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 1));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.max(size.width, MIN_TOUCH), Math.max(size.height, MIN_TOUCH));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            boolean chosen = isSelected();
            Color fill = !isEnabled() ? new Color(0xF3F4F6)
                    : chosen ? PRIMARY
                    : getModel().isRollover() && background == CARD ? PRIMARY_SOFT
                    : background;
            g2.setColor(fill);
            g2.fill(shape);
            g2.setColor(!isEnabled() ? LINE : chosen ? PRIMARY : border);
            g2.setStroke(new BasicStroke(isFocusOwner() ? 3f : 1.5f));
            g2.draw(shape);
            g2.dispose();
            setForeground(!isEnabled() ? HINT : chosen ? Color.WHITE : foreground);
            super.paintComponent(g);
        }
    }

    // ---------- 배치 ----------

    /** 흰 바탕 둥근 카드. */
    public static JPanel card(LayoutManager layout) {
        JPanel card = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.setColor(CARD);
                g2.fill(shape);
                g2.setColor(LINE);
                g2.draw(shape);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    /** 왼쪽에 제목·설명, 오른쪽에 버튼(없으면 null)을 둔 구역 머리줄. */
    public static JPanel sectionHeader(String title, String description, JComponent action) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel texts = new JPanel(new GridLayout(0, 1, 0, 2));
        texts.setOpaque(false);
        texts.add(label(title, 1.25f, Font.BOLD, TEXT));
        if (description != null) {
            texts.add(hint(description));
        }
        header.add(texts, BorderLayout.CENTER);
        if (action != null) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(action);
            header.add(right, BorderLayout.EAST);
        }
        return header;
    }

    /** 왼쪽부터 차례로 놓는 줄. */
    public static JPanel row(int gap, Component... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        row.setOpaque(false);
        for (Component component : components) {
            row.add(component);
        }
        return row;
    }

    /**
     * 세로로 길게 쌓이는 화면 내용을 담는 스크롤. 내용 폭은 창 폭에 맞추고 세로로만 스크롤한다.
     * 글씨를 키우거나 창이 작아도 모든 내용에 닿을 수 있다.
     */
    public static JScrollPane verticalScroll(JComponent content) {
        return verticalScroll(content, PAGE);
    }

    /** background: 내용 아래 남는 곳의 색 (대화상자처럼 흰 바탕이면 CARD). */
    public static JScrollPane verticalScroll(JComponent content, Color background) {
        JPanel holder = new TrackWidthPanel();
        holder.setBackground(background);
        holder.add(content, BorderLayout.NORTH); // 내용이 짧아도 위에 붙여 둔다
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(background);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private static final class TrackWidthPanel extends JPanel implements Scrollable {
        TrackWidthPanel() {
            super(new BorderLayout());
            setBackground(PAGE);
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
            return Math.max(visibleRect.height - 48, 48);
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

    // ---------- 화면 연결 ----------

    /** 화면이 보일 때마다(처음 켤 때, 메뉴로 돌아올 때) action 을 부른다. 로그인 상태가 바뀐 뒤에도 새 내용을 보여준다. */
    public static void whenShown(JComponent component, Runnable action) {
        component.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && component.isShowing()) {
                action.run();
            }
        });
    }

    /** 메인 창에 들어 있는 화면 객체를 찾는다. 없으면 null. 다른 화면에 "이 카테고리를 보여줘" 같은 부탁을 할 때 쓴다. */
    public static <T extends Component> T findScreen(MainFrame frame, Class<T> type) {
        return find(frame.getContentPane(), type);
    }

    private static <T extends Component> T find(Container container, Class<T> type) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container inner) {
                T found = find(inner, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 상품 카드를 눌렀을 때 상품 상세를 연다 (UC-06).
     * 상품 상세 화면(ProductDetailPanel)은 강준서 담당이고, 그 브랜치의 MainFrame 에 openProduct(Long) 가 있다.
     * 브랜치를 합치기 전에는 그 메서드가 없으므로 임시 상품 정보 창(ProductInfoDialog)을 연다.
     * 이렇게 하면 이 파일을 고치지 않고도 합친 뒤 자동으로 상세 화면이 열린다.
     */
    public static void openProduct(MainFrame frame, AppContext context, Product product) {
        if (callIfPresent(frame, "openProduct", Long.class, product.getId())) {
            return;
        }
        try {
            Product fresh = context.product.detail(product.getId());
            new ProductInfoDialog(frame, context, fresh).setVisible(true);
        } catch (RuntimeException e) {
            showError(frame, frame, e);
        }
    }

    /**
     * 다른 담당자의 화면에 public 메서드 name(type) 이 있으면 부르고 true. 없으면 아무것도 하지 않고 false.
     * 브랜치를 합치기 전·후 모두 컴파일되도록, 아직 없는 메서드를 직접 부르지 않을 때 쓴다.
     */
    public static boolean callIfPresent(Object target, String name, Class<?> type, Object arg) {
        if (target == null) {
            return false;
        }
        java.lang.reflect.Method method;
        try {
            method = target.getClass().getMethod(name, type);
        } catch (NoSuchMethodException e) {
            return false;
        }
        try {
            method.invoke(target, arg);
            return true;
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /**
     * Controller 오류를 docs/controller-spec.md 규칙대로 처리한다.
     * - 로그인 필요: 안내 후 로그인 화면으로 이동 (로그인 후 원래 화면으로 돌아온다)
     * - 권한 없음·검증 실패: 메시지를 그대로 보여준다
     * - 아직 구현되지 않은 기능: "준비 중" 안내
     */
    public static void showError(Component parent, MainFrame frame, RuntimeException error) {
        if (error instanceof LoginRequiredException) {
            JOptionPane.showMessageDialog(parent, "로그인이 필요한 기능입니다. 로그인 화면으로 이동합니다.",
                    "로그인 필요", JOptionPane.INFORMATION_MESSAGE);
            closeDialogOf(parent);
            frame.navigate(LoginPanel.NAME);
        } else if (error instanceof AccessDeniedException) {
            JOptionPane.showMessageDialog(parent, message(error, "이용 권한이 없습니다."), "알림",
                    JOptionPane.WARNING_MESSAGE);
        } else if (error instanceof AppException) {
            JOptionPane.showMessageDialog(parent, error.getMessage(), "알림", JOptionPane.WARNING_MESSAGE);
        } else if (error instanceof UnsupportedOperationException) {
            JOptionPane.showMessageDialog(parent, "아직 준비 중인 기능입니다. 조금만 기다려 주세요.", "준비 중",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, "문제가 생겼습니다. 잠시 후 다시 시도해 주세요.", "알림",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String message(RuntimeException error, String fallback) {
        return error.getMessage() == null || error.getMessage().isBlank() ? fallback : error.getMessage();
    }

    /** 대화상자 안에서 로그인이 필요해지면 대화상자를 닫고 로그인 화면을 보여준다. */
    private static void closeDialogOf(Component component) {
        java.awt.Window window = component instanceof java.awt.Window self ? self
                : component == null ? null : SwingUtilities.getWindowAncestor(component);
        if (window instanceof javax.swing.JDialog dialog) {
            dialog.dispose();
        }
    }
}
