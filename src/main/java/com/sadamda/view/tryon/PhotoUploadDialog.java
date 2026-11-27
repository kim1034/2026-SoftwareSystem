package com.sadamda.view.tryon;

import com.sadamda.common.AppException;
import com.sadamda.controller.DressUpController;
import com.sadamda.model.tryon.UserPhoto;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * [내 사진으로 입어보기] 사진 올리기 창 (UC-09 1~3단계).
 * - 잘 나오는 사진 안내와 예시, 고른 사진 미리보기
 * - 형식·용량이 맞지 않으면 그 자리에서 빨간 글씨로 알린다 (3a)
 * - 사진 이용 동의(필수)에 체크해야 [이 사진으로 입어보기]가 눌린다 (2a)
 * 고른 파일 내용은 이 창이 열려 있는 동안 메모리에만 있고, 어디에도 저장하지 않는다 (NFR-08).
 */
class PhotoUploadDialog extends JDialog {
    private static File lastDirectory;

    private final DressUpController controller;
    private final Preview preview = new Preview();
    private final JLabel error = Theme.errorLabel();
    private final JCheckBox consent = Theme.checkBox("사진 이용에 동의합니다 (필수)");
    private final JButton confirm = TryOnUi.primary("이 사진으로 입어보기");
    private String fileName;
    private byte[] bytes;
    private UserPhoto result;

    PhotoUploadDialog(Window owner, DressUpController controller, boolean agreedBefore) {
        super(owner, "내 사진 올리기", ModalityType.APPLICATION_MODAL);
        this.controller = controller;

        consent.setSelected(agreedBefore);
        consent.setFont(consent.getFont().deriveFont(Font.BOLD));
        consent.addActionListener(e -> updateConfirm());

        JPanel guide = new JPanel();
        guide.setLayout(new BoxLayout(guide, BoxLayout.Y_AXIS));
        guide.setOpaque(false);
        add(guide, Theme.label("이런 사진이 잘 나와요", 1.15f, Font.BOLD, Theme.TEXT), 0);
        add(guide, tip("머리부터 발끝까지 <b>전신</b>이 다 나온 사진"), 10);
        add(guide, tip("<b>정면</b>으로 똑바로 선 자세"), 6);
        add(guide, tip("밝은 곳에서 <b>혼자</b> 찍은 사진"), 6);
        add(guide, tip("JPG·PNG 파일, <b>10MB 이하</b>"), 6);

        JPanel examples = new JPanel(new GridLayout(1, 4, 10, 0));
        examples.setOpaque(false);
        examples.add(example(Example.Kind.FULL_BODY, "전신·정면"));
        examples.add(example(Example.Kind.FACE_ONLY, "얼굴만"));
        examples.add(example(Example.Kind.GROUP, "여러 명"));
        examples.add(example(Example.Kind.SITTING, "앉은 자세"));
        add(guide, examples, 14);
        add(guide, error, 10);

        JPanel consentBox = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = TryOnUi.smooth(graphics);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16);
                g.setColor(Theme.PAGE);
                g.fill(shape);
                g.setColor(Theme.LINE);
                g.draw(shape);
                g.dispose();
            }
        };
        consentBox.setOpaque(false);
        consentBox.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        consentBox.add(consent, BorderLayout.NORTH);
        consentBox.add(Theme.label("<html><div style='width:430px'>올린 사진은 옷을 입힌 그림을 만드는 데만 쓰며, "
                + "그림을 만들 때 그림 만드는 곳(AI 서비스)으로 보냅니다. 이 PC 에 따로 저장하지 않고, "
                + "[사진 지우기]를 누르거나 앱을 닫으면 사라집니다.</div></html>", 0.95f, Font.PLAIN, Theme.TEXT),
                BorderLayout.CENTER);
        add(guide, consentBox, 12);

        JPanel body = new JPanel(new BorderLayout(28, 0));
        body.setBackground(Theme.CARD);
        body.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        body.add(preview, BorderLayout.WEST);
        body.add(guide, BorderLayout.CENTER);

        JButton choose = TryOnUi.secondary("사진 고르기…");
        choose.addActionListener(e -> choose());
        JButton cancel = TryOnUi.secondary("취소");
        cancel.addActionListener(e -> close());
        confirm.addActionListener(e -> confirm());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(cancel);
        right.add(confirm);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(14, 28, 18, 28)));
        footer.add(choose, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.CARD);
        content.add(body, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().registerKeyboardAction(e -> close(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        updateConfirm();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    /** 확인한 사진. 취소했으면 null. */
    UserPhoto result() {
        return result;
    }

    /** 사진 이용에 동의했는지 (다음에 창을 열 때 미리 체크해 둔다). */
    boolean agreed() {
        return consent.isSelected();
    }

    private void choose() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setDialogTitle("전신 사진 고르기");
        chooser.setFileFilter(new FileNameExtensionFilter("사진 (JPG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        lastDirectory = file.getParentFile();
        fileName = file.getName();
        bytes = null;
        preview.setImage(null);
        try {
            if (file.length() > UserPhoto.MAX_BYTES) {
                throw new AppException("사진 용량은 10MB 이하여야 합니다.");
            }
            byte[] read = Files.readAllBytes(file.toPath());
            UserPhoto photo = controller.checkPhoto(fileName, read, true); // 형식·용량 먼저 확인 (동의는 [입어보기] 때)
            bytes = read;
            preview.setImage(photo.image());
            error.setText(" ");
        } catch (AppException e) {
            error.setText(e.getMessage());
        } catch (IOException | RuntimeException e) {
            error.setText("사진 파일을 읽지 못했습니다. 다른 사진을 골라 주세요.");
        }
        updateConfirm();
    }

    private void confirm() {
        try {
            result = controller.checkPhoto(fileName, bytes, consent.isSelected());
            bytes = null;
            dispose();
        } catch (AppException e) {
            error.setText(e.getMessage());
        }
    }

    private void close() {
        bytes = null;
        dispose();
    }

    private void updateConfirm() {
        confirm.setEnabled(bytes != null && consent.isSelected());
        confirm.setToolTipText(bytes == null ? "먼저 [사진 고르기…]로 사진을 골라 주세요."
                : consent.isSelected() ? null : "사진 이용에 동의해 주세요.");
    }

    private static void add(JPanel column, JComponent component, int gap) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(gap, 0, 0, 0));
        row.add(component, BorderLayout.CENTER);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(row);
    }

    private static JLabel tip(String html) {
        JLabel label = Theme.label("<html>" + html + "</html>", 1.05f, Font.PLAIN, Theme.TEXT);
        label.setIcon(TryOnUi.checkIcon(20));
        label.setIconTextGap(8);
        return label;
    }

    private static JPanel example(Example.Kind kind, String caption) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(new Example(kind), BorderLayout.CENTER);
        JLabel label = Theme.label(caption, 0.9f, Font.PLAIN, Theme.TEXT);
        label.setHorizontalAlignment(JLabel.CENTER);
        panel.add(label, BorderLayout.SOUTH);
        return panel;
    }

    /** 고른 사진 미리보기. 쓸 수 있는 사진이면 왼쪽 위에 초록 "✓ 쓸 수 있는 사진". */
    private static class Preview extends JComponent {
        private BufferedImage image;

        Preview() {
            setPreferredSize(new Dimension(300, 420));
        }

        void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = TryOnUi.smooth(graphics);
            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 20, 20);
            g.setColor(Theme.PAGE);
            g.fill(shape);
            if (image != null) {
                Graphics2D clipped = (Graphics2D) g.create();
                clipped.clip(shape);
                TryOnUi.drawContain(clipped, image, 4, 4, getWidth() - 8, getHeight() - 8);
                clipped.dispose();

                Font font = getFont() == null ? new Font(Font.DIALOG, Font.BOLD, 14) : getFont().deriveFont(Font.BOLD);
                g.setFont(font);
                FontMetrics metrics = g.getFontMetrics();
                String text = "쓸 수 있는 사진";
                int iconSize = metrics.getAscent();
                int w = metrics.stringWidth(text) + iconSize + 26;
                int h = metrics.getHeight() + 8;
                g.setColor(TryOnUi.OK);
                g.fill(new RoundRectangle2D.Float(12, 12, w, h, h, h));
                TryOnUi.drawCheck(g, 20, 12 + (h - iconSize) / 2f, iconSize, Color.WHITE, 2.4f);
                g.setColor(Color.WHITE);
                g.drawString(text, 24 + iconSize, 12 + 4 + metrics.getAscent());
            } else {
                g.setColor(Theme.HINT);
                g.setFont(getFont());
                FontMetrics metrics = g.getFontMetrics();
                String[] lines = {"[사진 고르기…]를 눌러", "전신 사진을 골라 주세요"};
                int y = getHeight() / 2 - metrics.getHeight() / 2;
                for (String line : lines) {
                    g.drawString(line, (getWidth() - metrics.stringWidth(line)) / 2, y);
                    y += metrics.getHeight();
                }
            }
            g.setColor(Theme.LINE);
            g.draw(shape);
            g.dispose();
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setFont(javax.swing.UIManager.getFont("Label.font"));
        }
    }

    /** 좋은 사진·나쁜 사진 예시 그림 (○ 전신·정면, × 얼굴만·여러 명·앉은 자세). */
    private static class Example extends JComponent {
        enum Kind { FULL_BODY, FACE_ONLY, GROUP, SITTING }

        private final Kind kind;

        Example(Kind kind) {
            this.kind = kind;
            setPreferredSize(new Dimension(92, 110));
            setToolTipText(kind == Kind.FULL_BODY ? "좋은 예" : "나쁜 예");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = TryOnUi.smooth(graphics);
            int w = getWidth();
            int h = getHeight();
            g.setColor(Theme.SOFT);
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            g.setColor(new Color(0x6B6B6B));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2f;
            float s = Math.min(w / 92f, h / 110f);
            switch (kind) {
                case FULL_BODY -> person(g, cx, h * 0.12f, s);
                case FACE_ONLY -> {
                    float r = 26 * s;
                    float cy = h * 0.55f;
                    g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                    g.fill(new Ellipse2D.Float(cx - 10 * s, cy - 8 * s, 5 * s, 5 * s));
                    g.fill(new Ellipse2D.Float(cx + 5 * s, cy - 8 * s, 5 * s, 5 * s));
                    g.draw(new Arc2D.Float(cx - 11 * s, cy - 6 * s, 22 * s, 18 * s, 200, 140, Arc2D.OPEN));
                }
                case GROUP -> {
                    person(g, cx - 17 * s, h * 0.18f, s * 0.85f);
                    person(g, cx + 17 * s, h * 0.18f, s * 0.85f);
                }
                case SITTING -> {
                    float top = h * 0.2f;
                    float hx = cx - 6 * s;
                    g.draw(new Ellipse2D.Float(hx - 8 * s, top, 16 * s, 16 * s));
                    g.draw(new Line2D.Float(hx, top + 16 * s, hx, top + 46 * s));
                    g.draw(new Line2D.Float(hx, top + 24 * s, hx + 14 * s, top + 38 * s));
                    g.draw(new Line2D.Float(hx, top + 46 * s, hx + 22 * s, top + 46 * s));
                    g.draw(new Line2D.Float(hx + 22 * s, top + 46 * s, hx + 22 * s, top + 72 * s));
                    g.setColor(new Color(0x9A9A9A));
                    g.draw(new Line2D.Float(hx - 10 * s, top + 50 * s, hx + 26 * s, top + 50 * s));
                    g.draw(new Line2D.Float(hx - 10 * s, top + 16 * s, hx - 10 * s, top + 76 * s));
                    g.draw(new Line2D.Float(hx + 26 * s, top + 50 * s, hx + 26 * s, top + 76 * s));
                }
            }
            // 오른쪽 위 ○(좋은 예) 또는 ×(나쁜 예)
            float m = 16;
            float mx = w - m - 8;
            float my = 8;
            g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (kind == Kind.FULL_BODY) {
                g.setColor(TryOnUi.OK);
                g.draw(new Ellipse2D.Float(mx, my, m, m));
            } else {
                g.setColor(Theme.ERROR);
                g.draw(new Line2D.Float(mx, my, mx + m, my + m));
                g.draw(new Line2D.Float(mx + m, my, mx, my + m));
            }
            g.dispose();
        }

        /** 서 있는 사람 (머리 위 y 부터). */
        private static void person(Graphics2D g, float cx, float top, float s) {
            g.draw(new Ellipse2D.Float(cx - 8 * s, top, 16 * s, 16 * s));
            g.draw(new Line2D.Float(cx, top + 16 * s, cx, top + 50 * s));
            g.draw(new Line2D.Float(cx, top + 24 * s, cx - 13 * s, top + 42 * s));
            g.draw(new Line2D.Float(cx, top + 24 * s, cx + 13 * s, top + 42 * s));
            g.draw(new Line2D.Float(cx, top + 50 * s, cx - 10 * s, top + 78 * s));
            g.draw(new Line2D.Float(cx, top + 50 * s, cx + 10 * s, top + 78 * s));
        }
    }
}
