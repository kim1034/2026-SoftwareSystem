package com.sadamda.view.admin;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.member.LoginPanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * 관리자 화면: UC-16 상품 관리, UC-17 룩북·유행 아이템 관리, UC-18 주문 관리를 탭으로 나눈다.
 * 관리자만 들어올 수 있다 (아니면 로그인 화면으로). 담당: 김대원
 */
public class AdminPanel extends ScreenPanel {
    public static final String NAME = "admin";

    private final ProductAdminTab products;
    private final LookbookAdminTab lookbooks;
    private final OrderAdminTab orders;

    public AdminPanel(AppContext context, MainFrame frame) {
        super(context, frame, "관리자", "UC-16 상품 관리, UC-17 룩북·유행 아이템 관리, UC-18 주문 관리");
        hideTitle();
        setBackground(Theme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        products = new ProductAdminTab(context);
        lookbooks = new LookbookAdminTab(context);
        orders = new OrderAdminTab(context);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("상품 관리", products);
        tabs.addTab("룩북·유행 아이템", lookbooks);
        tabs.addTab("주문 관리", orders);
        tabs.addChangeListener(e -> refresh(tabs.getSelectedIndex()));

        JPanel screen = new JPanel(new BorderLayout(0, 16));
        screen.setOpaque(false);
        screen.add(Theme.label("관리자", 1.6f, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        screen.add(tabs, BorderLayout.CENTER);
        setContent(screen);

        // 화면을 열 때마다 지금 탭을 새로 불러온다. 관리자가 아니면 로그인 화면으로.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (!context.session.isAdmin()) {
                    frame.navigate(LoginPanel.NAME);
                    return;
                }
                refresh(tabs.getSelectedIndex());
            }
        });
    }

    private void refresh(int tab) {
        if (!context.session.isAdmin()) {
            return;
        }
        switch (tab) {
            case 0 -> products.refresh();
            case 1 -> lookbooks.refresh();
            default -> orders.refresh();
        }
    }
}
