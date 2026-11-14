package com.sadamda.view;

import com.sadamda.controller.AppContext;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** 모든 화면의 공통 부모. 제목과 아직 구현되지 않은 Use Case 를 표시한다. */
public abstract class ScreenPanel extends JPanel {
    protected final AppContext context;
    protected final MainFrame frame;
    private final JLabel titleLabel;
    private JComponent content;

    protected ScreenPanel(AppContext context, MainFrame frame, String title, String useCases) {
        super(new BorderLayout(0, 16));
        this.context = context;
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        titleLabel = new JLabel("<html><b>" + title + "</b></html>");
        add(titleLabel, BorderLayout.NORTH);
        setContent(new JLabel("TODO: " + useCases + " 화면 구현"));
    }

    /** 화면이 제목을 직접 그릴 때 공통 제목을 숨긴다. */
    protected void hideTitle() {
        titleLabel.setVisible(false);
    }

    /** 화면 가운데 내용을 바꾼다. 화면을 구현하면 TODO 라벨 대신 실제 내용을 넣는다. */
    protected void setContent(JComponent newContent) {
        if (content != null) {
            remove(content);
        }
        content = newContent;
        add(content, BorderLayout.CENTER);
    }
}
