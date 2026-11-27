package com.sadamda.view.recommend;

import com.sadamda.model.product.Product;
import com.sadamda.view.ProductImages;
import com.sadamda.view.Theme;
import com.sadamda.view.product.ShopUi;
import com.sadamda.view.product.WishActions;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** 홈 화면의 부품: 큰 배너, 카테고리 칸, 프로모션 카드, 베스트 순위 카드, 골드 알약. */
final class HomeParts {
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();

    private HomeParts() {
    }

    /** src/main/resources/banners 의 그림. 없으면 null (배경색만 칠한다). */
    static BufferedImage banner(String file) {
        return resource("/banners/" + file);
    }

    /** src/main/resources/icons 의 아이콘 그림 (카테고리·프로모션). 없으면 null. */
    static BufferedImage icon(String file) {
        return resource("/icons/" + file);
    }

    private static BufferedImage resource(String path) {
        return IMAGES.computeIfAbsent(path, name -> {
            try (InputStream in = HomeParts.class.getResourceAsStream(name)) {
                return in == null ? null : ImageIO.read(in);
            } catch (IOException e) {
                return null;
            }
        });
    }

    /** 그림을 칸에 꽉 차게 그린다. focusX: 잘릴 때 남길 가로 위치 (0 왼쪽 ~ 1 오른쪽). */
    static void drawCover(Graphics2D g, BufferedImage image, int x, int y, int w, int h, double focusX, double focusY) {
        double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
        int iw = (int) Math.ceil(image.getWidth() * scale);
        int ih = (int) Math.ceil(image.getHeight() * scale);
        int ix = x + (int) Math.round((w - iw) * focusX);
        int iy = y + (int) Math.round((h - ih) * focusY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, ix, iy, iw, ih, null);
    }

    static Font font(float scale, int style) {
        Font base = UIManager.getFont("Label.font");
        return base.deriveFont(style, base.getSize2D() * scale);
    }

    // ---------- 큰 배너 ----------

    /**
     * 창 폭 가득한 배너. 오른쪽에 사진, 왼쪽 여백에 글과 버튼을 둔다 (사진의 왼쪽이 밝은 벽이라 진한 글씨가 잘 보인다).
     * text 패널은 왼쪽 가운데에 놓인다.
     */
    static final class Banner extends JPanel {
        private final String file;
        private final Color fallback;
        private final int height;
        private final double focusX;

        Banner(String file, Color fallback, int height, double focusX, JComponent text) {
            super(new GridBagLayout());
            this.file = file;
            this.fallback = fallback;
            this.height = height;
            this.focusX = focusX;
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(24, 72, 24, 72));
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1;
            c.weighty = 1;
            add(text, c);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width, Math.max(height, size.height));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        /**
         * 사진을 배너 높이에 맞춰 오른쪽에 붙이고, 왼쪽은 사진 왼쪽 끝과 같은 색으로 이어 칠한다.
         * 사람이 있는 세로 사진이 잘리지 않게 할 때 쓴다.
         */
        Banner fitHeight() {
            fitHeight = true;
            return this;
        }

        private boolean fitHeight;

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Theme.smooth(graphics);
            BufferedImage image = banner(file);
            int w = getWidth();
            int h = getHeight();
            if (image != null && fitHeight) {
                // 사진 가장자리는 어두워서, 사람이 있는 오른쪽 일부만 쓰고 그 왼쪽 끝 색으로 나머지를 칠한다
                image = image.getSubimage((int) (image.getWidth() * 0.4), 0, (int) (image.getWidth() * 0.6),
                        image.getHeight());
                Color edge = lighter(edgeColor(image), 0.3f);
                g.setColor(edge);
                g.fillRect(0, 0, w, h);
                double scale = (double) h / image.getHeight();
                int iw = (int) Math.round(image.getWidth() * scale);
                int ih = (int) Math.round(image.getHeight() * scale);
                int x = w - iw;
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, x, (h - ih) / 2, iw, ih, null);
                int blend = iw / 2; // 이음매가 보이지 않게 흐리게 잇는다
                g.setPaint(new java.awt.GradientPaint(x, 0, edge,
                        x + blend, 0, new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 0)));
                g.fillRect(x, 0, blend, h);
            } else {
                g.setColor(fallback);
                g.fillRect(0, 0, w, h);
                if (image != null) {
                    drawCover(g, image, 0, 0, w, h, focusX, 0.5);
                }
            }
            g.dispose();
        }

        /** 흰색을 amount 만큼 섞어 밝게 한다. */
        private static Color lighter(Color color, float amount) {
            return new Color(Math.round(color.getRed() + (255 - color.getRed()) * amount),
                    Math.round(color.getGreen() + (255 - color.getGreen()) * amount),
                    Math.round(color.getBlue() + (255 - color.getBlue()) * amount));
        }

        /** 사진 왼쪽 끝 한 줄의 평균 색. */
        private static Color edgeColor(BufferedImage image) {
            long r = 0;
            long gr = 0;
            long b = 0;
            int n = image.getHeight();
            for (int y = 0; y < n; y++) {
                int rgb = image.getRGB(2, y);
                r += (rgb >> 16) & 0xFF;
                gr += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
            }
            return new Color((int) (r / n), (int) (gr / n), (int) (b / n));
        }
    }

    /** 글과 버튼을 세로로 쌓은 투명 패널 (배너 위 글). */
    static JPanel stack(JComponent... rows) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < rows.length; i++) {
            c.insets = new Insets(i == 0 ? 0 : 16, 0, 0, 0);
            panel.add(rows[i], c);
        }
        return panel;
    }

    /** 작은 골드 꼬리표 (배너의 "2026 F/W"). */
    static JLabel goldTag(String text) {
        JLabel tag = new JLabel(text) {
            @Override
            public void updateUI() {
                super.updateUI();
                setFont(font(0.9f, Font.BOLD));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = Theme.smooth(graphics);
                g.setColor(Theme.GOLD);
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        tag.setForeground(Theme.TEXT);
        tag.setBorder(BorderFactory.createEmptyBorder(5, 16, 5, 16));
        return tag;
    }

    // ---------- 카테고리 칸 ----------

    /** 둥근 사각 아이콘 칸과 아래 이름. 아이콘은 src/main/resources/icons/category_*.png. */
    static final class CategoryTile extends JButton {
        private final BufferedImage image;

        CategoryTile(String label, String iconFile) {
            super(label);
            this.image = icon(iconFile);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName(label + " 상품 보기");
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
            setFont(font(1f, Font.PLAIN));
        }

        private int box() {
            return Math.max(64, Math.min(getWidth(), 104));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(88, 104 + 10 + fm.getHeight());
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(64, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Theme.smooth(graphics);
            int s = box();
            int x = (getWidth() - s) / 2;
            RoundRectangle2D square = new RoundRectangle2D.Float(x, 0, s, s, 18, 18);
            g.setColor(Theme.SOFT);
            g.fill(square);
            if (image != null) {
                Graphics2D clip = (Graphics2D) g.create();
                clip.clip(square);
                clip.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                clip.drawImage(image, x, 0, s, s, null);
                clip.dispose();
            }
            boolean active = getModel().isRollover() || isFocusOwner();
            g.setColor(active ? Theme.GOLD_BORDER : Theme.LINE);
            g.setStroke(new BasicStroke(active ? 2.4f : 1.2f));
            g.draw(new RoundRectangle2D.Float(x + 1, 1, s - 2, s - 2, 18, 18));
            g.setFont(getFont());
            g.setColor(Theme.TEXT);
            FontMetrics fm = g.getFontMetrics();
            String text = getText();
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, s + 10 + fm.getAscent());
            g.dispose();
        }
    }

    // ---------- 프로모션 카드 ----------

    /** 홈의 2열 프로모션 카드: 왼쪽 큰 제목, 오른쪽 아이콘 그림 (src/main/resources/icons/promo_*.png). 카드 전체를 누른다. */
    static final class PromoCard extends JButton {
        private final Color fill;
        private final BufferedImage image;

        PromoCard(String title, Color fallback, String iconFile) {
            super(title);
            this.image = icon(iconFile);
            // 아이콘 그림 모서리의 바탕색으로 카드를 칠해 그림과 카드가 한 장처럼 보이게 한다
            this.fill = image == null ? fallback : new Color(image.getRGB(1, 1));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setHorizontalAlignment(LEFT);
            setForeground(Theme.TEXT);
            setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
            setFont(font(1.3f, Font.BOLD));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width + 120, Math.max(116, size.height));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Theme.smooth(graphics);
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g.setColor(fill);
            g.fill(shape);
            if (image != null) {
                int s = Math.min(getHeight() - 8, 112);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(image, getWidth() - s - 28, (getHeight() - s) / 2, s, s, null);
            }
            if (getModel().isRollover() || isFocusOwner()) {
                g.setColor(Theme.HINT);
                g.setStroke(new BasicStroke(1.6f));
                g.draw(shape);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    // ---------- 골드 알약 ----------

    /** 베스트 분류 알약. 고른 것은 골드 바탕, 나머지는 흰 바탕에 테두리. */
    static JToggleButton goldPill(String text) {
        JToggleButton pill = new JToggleButton(text) {
            @Override
            public void updateUI() {
                setUI(new BasicToggleButtonUI());
                setFont(font(1f, Font.BOLD));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = Theme.smooth(graphics);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
                g.setColor(isSelected() ? Theme.GOLD : Theme.CARD);
                g.fill(shape);
                if (!isSelected()) {
                    g.setColor(getModel().isRollover() ? Theme.HINT : Theme.LINE);
                    g.setStroke(new BasicStroke(1.4f));
                    g.draw(shape);
                }
                if (isFocusOwner()) {
                    g.setColor(Theme.TEXT);
                    g.setStroke(new BasicStroke(2f));
                    g.draw(new RoundRectangle2D.Float(3, 3, getWidth() - 7, getHeight() - 7,
                            getHeight() - 7, getHeight() - 7));
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        pill.setForeground(Theme.TEXT);
        pill.setContentAreaFilled(false);
        pill.setFocusPainted(false);
        pill.setRolloverEnabled(true);
        pill.setBorder(BorderFactory.createEmptyBorder(12, 28, 12, 28)); // 48px 이상 (NFR-02)
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return pill;
    }

    // ---------- 베스트 순위 카드 ----------

    /**
     * 홈의 상품 카드: 정사각 사진 칸, 상품명 한 줄, 가격과 찜 하트. 카드를 누르면 상품 상세.
     * rank 가 1 이상이면 사진 왼쪽 위에 순위 뱃지 (지금 뜨는 베스트).
     */
    static final class RankCard extends JPanel {
        RankCard(Product product, int rank, Consumer<Product> onOpen, WishActions wish) {
            super(new BorderLayout(0, 10));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String name = ShopUi.displayName(product.getName());
            getAccessibleContext().setAccessibleName((rank > 0 ? rank + "위 " : "") + name + ", "
                    + ShopUi.price(product.getPrice()));
            setToolTipText(product.getName());

            String url = product.mainImageUrl();
            JComponent photo = new JComponent() {
                {
                    if (url != null) {
                        ProductImages.request(url, this::repaint);
                    }
                }

                @Override
                public Dimension getPreferredSize() {
                    int w = Math.max(160, getWidth());
                    return new Dimension(200, w);
                }

                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = Theme.smooth(graphics);
                    int w = getWidth();
                    int h = getHeight();
                    RoundRectangle2D box = new RoundRectangle2D.Float(0, 0, w, h, 18, 18);
                    g.setColor(Theme.SOFT);
                    g.fill(box);
                    BufferedImage image = url == null ? null : ProductImages.get(url);
                    if (image != null) {
                        Graphics2D clip = (Graphics2D) g.create();
                        clip.clip(box);
                        drawCover(clip, image, 0, 0, w, h, 0.5, 0.25);
                        clip.dispose();
                    }
                    if (rank <= 0) {
                        g.dispose();
                        return;
                    }
                    // 순위 뱃지
                    String text = String.format("%02d", rank);
                    g.setFont(font(0.95f, Font.BOLD));
                    FontMetrics fm = g.getFontMetrics();
                    int d = Math.max(40, fm.stringWidth(text) + 16);
                    g.setColor(Theme.GOLD);
                    g.fill(new Ellipse2D.Float(12, 12, d, d));
                    g.setColor(Theme.TEXT);
                    g.drawString(text, 12 + (d - fm.stringWidth(text)) / 2, 12 + (d - fm.getHeight()) / 2 + fm.getAscent());
                    g.dispose();
                }
            };

            JLabel nameLabel = new JLabel(name) {
                @Override
                public void updateUI() {
                    super.updateUI();
                    setFont(font(1f, Font.PLAIN));
                }
            };
            nameLabel.setForeground(Theme.TEXT);
            JLabel priceLabel = Theme.label(ShopUi.price(product.getPrice()), 1.15f, Font.BOLD, Theme.TEXT);
            HeartToggle heart = new HeartToggle(wish.isWished(product.getId()));
            heart.addActionListener(e -> {
                wish.toggleWish(product.getId());
                heart.setWished(wish.isWished(product.getId()));
            });

            JPanel priceRow = new JPanel(new BorderLayout());
            priceRow.setOpaque(false);
            priceRow.add(priceLabel, BorderLayout.CENTER);
            priceRow.add(heart, BorderLayout.EAST);
            JPanel texts = new JPanel(new BorderLayout(0, 2));
            texts.setOpaque(false);
            texts.add(nameLabel, BorderLayout.NORTH);
            texts.add(priceRow, BorderLayout.CENTER);

            add(photo, BorderLayout.CENTER);
            add(texts, BorderLayout.SOUTH);
            MouseAdapter open = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onOpen.accept(product);
                }
            };
            addMouseListener(open);
            photo.addMouseListener(open);
            nameLabel.addMouseListener(open);
        }
    }

    /** 선으로 그린 작은 찜 하트. 찜했으면 빨갛게 채운다. 누르는 칸은 44px. */
    static final class HeartToggle extends JButton {
        private boolean wished;

        HeartToggle(boolean wished) {
            this.wished = wished;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(44, 44));
            updateName();
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
        }

        void setWished(boolean wished) {
            this.wished = wished;
            updateName();
            repaint();
        }

        private void updateName() {
            getAccessibleContext().setAccessibleName(wished ? "찜 해제하기" : "찜하기");
            setToolTipText(wished ? "찜 해제" : "찜하기");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Theme.smooth(graphics);
            float s = 22;
            float x = (getWidth() - s) / 2f;
            float y = (getHeight() - s) / 2f + 1;
            Path2D heart = new Path2D.Float();
            heart.moveTo(x + s / 2, y + s * 0.90f);
            heart.curveTo(x - s * 0.08f, y + s * 0.50f, x + s * 0.02f, y - s * 0.02f, x + s / 2, y + s * 0.24f);
            heart.curveTo(x + s * 0.98f, y - s * 0.02f, x + s * 1.08f, y + s * 0.50f, x + s / 2, y + s * 0.90f);
            heart.closePath();
            if (wished) {
                g.setColor(Theme.ERROR);
                g.fill(heart);
            }
            g.setColor(wished ? Theme.ERROR : Theme.HINT);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(heart);
            if (isFocusOwner()) {
                g.setColor(Theme.TEXT);
                g.draw(new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 10, 10));
            }
            g.dispose();
        }
    }
}
