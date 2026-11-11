package com.sadamda.view;

import com.sadamda.controller.AppContext;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** 모든 화면의 공통 부모. 제목과 아직 구현되지 않은 Use Case 를 표시한다. */
public abstract class ScreenPanel extends JPanel {
    protected final AppContext context;
    protected final MainFrame frame;

    protected ScreenPanel(AppContext context, MainFrame frame, String title, String useCases) {
        super(new BorderLayout(0, 16));
        this.context = context;
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        add(new JLabel("<html><b>" + title + "</b></html>"), BorderLayout.NORTH);
        add(new JLabel("TODO: " + useCases + " 화면 구현"), BorderLayout.CENTER);
    }
}
