package com.sadamda;

import com.sadamda.controller.AppContext;
import com.sadamda.view.MainFrame;

import javax.swing.SwingUtilities;

/** 사담다 애플리케이션 진입점. */
public class App {
    public static void main(String[] args) {
        AppContext context = new AppContext();
        SwingUtilities.invokeLater(() -> new MainFrame(context).setVisible(true));
    }
}
