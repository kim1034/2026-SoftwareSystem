package com.sadamda.view.tryon;

import com.sadamda.common.LoginRequiredException;
import com.sadamda.controller.AppContext;
import com.sadamda.controller.CodiBookController;
import com.sadamda.external.DressUpException;
import com.sadamda.external.DressUpSettings;
import com.sadamda.external.RemoteImages;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.recommend.OutfitSuggestion;
import com.sadamda.model.tryon.SavedCodi;
import com.sadamda.model.tryon.Background;
import com.sadamda.model.tryon.BackgroundCatalog;
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.model.tryon.UserPhoto;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.Toast;
import com.sadamda.view.WrapLayout;
import com.sadamda.view.cart.CartPanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 옷 입혀보기 (UC-09): 캐릭터 또는 내 사진에 상품을 입혀 본다.
 *
 * 화면 구성 (2026-11-20 승인한 시안 기준)
 * - 왼쪽 위: 그림체 4가지(실사·디즈니 캐릭터·3D 게임 캐릭터·애니메이션 캐릭터) + [내 사진으로 입어보기].
 *   폭이 넓으면 한 줄, 좁으면 줄을 바꾼다 (TryOnUi.Rows)
 * - 왼쪽 가운데: 큰 그림 칸. 캐릭터, 올린 사진, 또는 입혀 본 결과. 내 사진이면 [사진 바꾸기]·[사진 지우기].
 *   누르거나 [크게 보기]를 누르면 크게 보기 창(입혀보기가 끝나면 자동으로 열림). 좌우 여백은 배경으로 채운다
 * - 왼쪽 아래: 캐릭터면 [신체 스펙], 내 사진이면 배경 고르기 (계절 칩·[모두 보기] 한 줄 + 옆으로 넘기는 배경 사진 버튼 줄)
 * - 오른쪽: 입힐 아이템 14칸을 옷 · 머리·목 · 주얼리 · 발·소품으로 묶어 보여준다. 누르면 아이템 고르기 창
 * - 맨 아래: [입혀보기] 와 결과 처리 버튼 (UC-09 9단계). 스크롤 밖에 고정해서 늘 보인다
 * 창이 작거나 글씨가 크면 맨 아래 버튼 줄을 뺀 나머지가 세로로 스크롤된다. 스크롤 막대는 앱 모양에 맞춘 얇고 둥근 막대(ModernScrollBar).
 */
public class TryOnPanel extends ScreenPanel {
    public static final String NAME = "tryOn";
    private static final int SLOT_THUMBNAIL = 48;
    private static final String ALL_SEASONS = "전체";

    private final Outfit outfit = new Outfit();
    private final Map<DressUpSlot, SlotCard> slotCards = new EnumMap<>(DressUpSlot.class);
    private final Map<CharacterStyle, JToggleButton> styleChips = new EnumMap<>(CharacterStyle.class);
    private final Map<String, JToggleButton> seasonChips = new LinkedHashMap<>();
    private final List<BackgroundTile> tiles = new ArrayList<>();
    private final List<Background> backgrounds;
    private final CharacterView characterView = new CharacterView();
    private final TagLabel stageTag = new TagLabel();
    private final JPanel photoButtons = new JPanel();
    /** 큰 그림 칸. 창이 작으면 다른 것보다 먼저 줄어든다 (Page). */
    private final JButton zoomButton = TryOnUi.secondary("크게 보기");
    private final Stage stage = new Stage(characterView, stageTag, photoButtons, zoomButton);
    private final JButton changePhotoButton = TryOnUi.secondary("사진 바꾸기");
    private final JButton clearPhotoButton = TryOnUi.secondary("사진 지우기");
    private final JToggleButton photoChip = Theme.chip("내 사진으로 입어보기");
    private final JPanel below = new JPanel(new BorderLayout());
    private final JPanel backgroundCard;
    private final BackgroundSlider slider = new BackgroundSlider();
    /** [원래 배경] 버튼. 배경 줄을 다시 만들 때 새로 만든다. */
    private BackgroundTile keepTile;
    private final JButton allBackgroundsButton;
    private final JButton pickAllButton = TryOnUi.secondary("한꺼번에 고르기");
    private final JLabel serviceLabel = Theme.label(" ", 1f, Font.PLAIN, Theme.HINT);
    private final JButton specButton = TryOnUi.secondary("");
    private final JLabel status = Theme.label(" ", 1f, Font.PLAIN, Theme.TEXT);
    private final JLabel total = Theme.label(" ", 1.1f, Font.BOLD, Theme.TEXT);
    private final JProgressBar progress = new JProgressBar();
    private final JButton generateButton = TryOnUi.primary("입혀보기");
    private final JButton retryButton = TryOnUi.secondary("다시 시도");
    private final JButton undressButton = TryOnUi.secondary("모두 벗기");
    private final JButton lockerButton = TryOnUi.secondary("보관함에 저장");
    private final JButton openLockerButton = TryOnUi.secondary("내 보관함");
    private final JButton downloadButton = TryOnUi.secondary("내 컴퓨터에 저장");
    private final JButton cartButton = TryOnUi.secondary("장바구니 담기");
    private final JButton settingsButton = TryOnUi.secondary("AI 연결 설정");
    private final JButton helperButton = TryOnUi.primary("어디 가세요? 코디 추천받기");
    private final JButton codiSaveButton = TryOnUi.secondary("코디북에 저장");
    private final JButton codiBookButton = TryOnUi.secondary("내 코디북");

    /** 고른 옷이 바뀔 때마다 1씩 는다. 그림이 지금 옷차림으로 그린 것인지 확인할 때 쓴다 (UC-19). */
    private int outfitVersion;
    /** resultImage 를 그릴 때의 outfitVersion. */
    private int resultVersion = -1;
    /** resultImage 가 올린 사진으로 만든 그림인지. 사진 그림은 코디북에 남기지 않는다 (NFR-08). */
    private boolean resultFromPhoto;
    /** 코디북에서 다시 연 코디 (UC-19). [코디북에 저장]이 덮어쓸지 묻는다. 없으면 null. */
    private SavedCodi openedCodi;
    /** 코디 도우미(UC-20)에서 고른 코디 이름. 코디북에 저장할 때 이름 칸에 먼저 넣어 둔다. */
    private String suggestedTitle;

    private CharacterStyle style = CharacterStyle.REAL;
    private CharacterSpec spec;
    /** 올린 사진. 메모리에만 있다 (NFR-08). */
    private UserPhoto photo;
    private boolean photoMode;
    private boolean photoAgreed;
    /** 고른 배경. null 이면 사진의 원래 배경. */
    private Background background;
    private String season = ALL_SEASONS;
    private byte[] resultImage;
    private boolean working;

    public TryOnPanel(AppContext context, MainFrame frame) {
        super(context, frame, "AI 입어보기", "UC-09 AI 착용 이미지 생성");
        removeAll(); // 공통 부모가 넣어 둔 "TODO" 안내를 걷어내고 실제 화면을 넣는다
        setBackground(Theme.PAGE);
        setBorder(BorderFactory.createEmptyBorder());
        spec = context.dressUp.initialSpec();
        backgrounds = context.dressUp.backgrounds();
        allBackgroundsButton = Theme.linkButton("배경 " + backgrounds.size() + "개 모두 보기 ›");
        backgroundCard = backgroundCard();

        JPanel middle = new JPanel(new BorderLayout(24, 0));
        middle.setOpaque(false);
        middle.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        middle.add(characterColumn(), BorderLayout.CENTER);
        middle.add(slotColumn(), BorderLayout.EAST);

        Page page = new Page(stage);
        page.setBorder(BorderFactory.createEmptyBorder(20, 48, 12, 48));
        page.add(header(), BorderLayout.NORTH);
        page.add(middle, BorderLayout.CENTER);

        JScrollPane scroll = ModernScrollBar.apply(new JScrollPane(page));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        JPanel actions = actions();
        add(actions, BorderLayout.SOUTH); // 스크롤 밖: 창이 작아도 [입혀보기]와 결과 버튼이 늘 보인다
        // 줄 바꿈 배치(WrapLayout·Rows)는 바뀌기 전 폭으로 높이를 재서, 창을 키운 뒤 빈칸이 남는다. 폭이 바뀌면 한 번 더 잰다.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            private int lastWidth = -1;

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (getWidth() != lastWidth) {
                    lastWidth = getWidth();
                    // revalidate 는 위쪽만 무효로 해서 안쪽의 줄 바꿈 칸이 예전 높이를 그대로 쓴다. 안쪽까지 모두 다시 잰다.
                    SwingUtilities.invokeLater(() -> {
                        invalidateTree(TryOnPanel.this);
                        revalidate();
                        repaint();
                    });
                }
            }
        });

        showService();
        showSpec();
        refreshStrip();
        showBase();
        refreshSlots();
        setStatus(context.dressUp.dressUpConnected()
                ? "옷을 고르고 [입혀보기]를 눌러 주세요. [내 사진으로 입어보기]로 내 사진에도 입혀 볼 수 있어요."
                : "그림 만드는 곳(AI)에 연결되지 않았습니다. [AI 연결 설정]에서 이용 열쇠를 넣어 주세요.");
    }

    private JPanel header() {
        // 상품 화면 머리말과 같은 모양: 큰 제목 + 아래 구분선
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(0, 0, 12, 0)));
        header.add(Theme.label("옷 입혀보기", 1.6f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        header.add(serviceLabel, BorderLayout.EAST);
        return header;
    }

    /** 왼쪽: 그림체·내 사진 고르기 + 큰 그림 칸 + (신체 스펙 또는 배경 고르기). */
    private JPanel characterColumn() {
        // 그림체 4개 + [내 사진으로 입어보기]를 왼쪽부터 늘어놓고, 폭이 모자라면 다음 줄로 넘긴다.
        // 넓은 창(최대화)에서는 한 줄이라 화면 높이를 아끼고, 좁거나 글씨가 크면 두세 줄이 되어 이름이 잘리지 않는다.
        JPanel choose = new JPanel(new TryOnUi.Rows(8));
        choose.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (CharacterStyle option : CharacterStyle.values()) {
            JToggleButton button = Theme.chip(option.label());
            button.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            button.setSelected(option == style);
            button.addActionListener(e -> selectStyle(option));
            group.add(button);
            choose.add(button);
            styleChips.put(option, button);
        }
        photoChip.setIcon(TryOnUi.cameraIcon(22));
        photoChip.setIconTextGap(10);
        photoChip.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        photoChip.addActionListener(e -> selectPhotoMode());
        group.add(photoChip);
        choose.add(photoChip);

        specButton.addActionListener(e -> editSpec());
        zoomButton.addActionListener(e -> openViewer());
        characterView.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openViewer();
            }
        });
        changePhotoButton.addActionListener(e -> changePhoto());
        clearPhotoButton.addActionListener(e -> clearPhoto());
        photoButtons.setLayout(new BoxLayout(photoButtons, BoxLayout.Y_AXIS));
        photoButtons.setOpaque(false);
        for (JButton button : List.of(changePhotoButton, clearPhotoButton)) {
            button.setAlignmentX(Component.RIGHT_ALIGNMENT);
            photoButtons.add(button);
            photoButtons.add(javax.swing.Box.createVerticalStrut(8));
        }
        below.setOpaque(false);

        JPanel column = new JPanel(new BorderLayout(0, 12));
        column.setOpaque(false);
        column.add(choose, BorderLayout.NORTH);
        column.add(stage, BorderLayout.CENTER);
        column.add(below, BorderLayout.SOUTH);
        return column;
    }

    /** 내 사진일 때 [신체 스펙] 자리에 보이는 배경 고르기 카드. */
    private JPanel backgroundCard() {
        JPanel chips = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        JLabel title = Theme.label("배경 고르기", 1.15f, Font.BOLD, Theme.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        chips.add(title);
        ButtonGroup group = new ButtonGroup();
        List<String> options = new ArrayList<>();
        options.add(ALL_SEASONS);
        options.addAll(BackgroundCatalog.SEASONS);
        for (String option : options) {
            JToggleButton chip = Theme.chip(option);
            chip.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            chip.setSelected(option.equals(season));
            chip.addActionListener(e -> {
                season = option;
                refreshStrip();
            });
            chip.getAccessibleContext().setAccessibleName(option.equals(ALL_SEASONS) ? "모든 계절 배경" : option + " 배경");
            group.add(chip);
            chips.add(chip);
            seasonChips.put(option, chip);
        }

        // [배경 모두 보기]는 계절 칩과 같은 줄 오른쪽 끝에 둔다 (따로 한 줄을 쓰지 않아 화면 높이를 아낀다)
        allBackgroundsButton.addActionListener(e -> openAllBackgrounds());
        allBackgroundsButton.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 4));
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(chips, BorderLayout.CENTER);
        top.add(allBackgroundsButton, BorderLayout.EAST);

        JPanel card = TryOnUi.card(new BorderLayout(0, 6), 12);
        card.add(top, BorderLayout.NORTH);
        card.add(slider, BorderLayout.CENTER);
        return card;
    }

    /** 오른쪽: 입힐 아이템 14칸 (부위별 묶음). 칸이 많아 넘치면 이 부분만 스크롤한다. */
    private JPanel slotColumn() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        DressUpSlot.Group group = null;
        JPanel grid = null;
        for (DressUpSlot slot : DressUpSlot.values()) {
            if (slot.group() != group) {
                group = slot.group();
                JLabel heading = Theme.label(group.label(), 0.9f, Font.BOLD, Theme.HINT);
                heading.setBorder(BorderFactory.createEmptyBorder(list.getComponentCount() == 0 ? 0 : 12, 4, 6, 0));
                heading.setAlignmentX(Component.LEFT_ALIGNMENT);
                list.add(heading);
                grid = new JPanel(new GridLayout(0, 2, 8, 8));
                grid.setOpaque(false);
                grid.setAlignmentX(Component.LEFT_ALIGNMENT);
                list.add(grid);
            }
            SlotCard card = new SlotCard(slot);
            card.addActionListener(e -> openPicker(slot));
            slotCards.put(slot, card);
            grid.add(card);
        }
        JScrollPane scroll = ModernScrollBar.apply(new JScrollPane(list));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(420, 240)); // 높이는 왼쪽 열에 맞춰 늘어난다

        pickAllButton.addActionListener(e -> openPicker(null));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Theme.label("입힐 아이템", 1.15f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        head.add(pickAllButton, BorderLayout.EAST);

        // UC-20: 무엇을 입힐지 막막할 때 상황만 고르면 한 벌을 채워 준다
        helperButton.addActionListener(e -> openHelper(null));
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(helperButton, BorderLayout.NORTH);
        top.add(head, BorderLayout.CENTER);

        JPanel column = new JPanel(new BorderLayout(0, 8));
        column.setOpaque(false);
        column.add(top, BorderLayout.NORTH);
        column.add(scroll, BorderLayout.CENTER);
        return column;
    }

    private JPanel actions() {
        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setPreferredSize(new Dimension(160, 24));
        retryButton.setVisible(false);

        generateButton.addActionListener(e -> generate());
        retryButton.addActionListener(e -> generate());
        undressButton.addActionListener(e -> {
            outfit.clear();
            outfitChanged();
            openedCodi = null;
            suggestedTitle = null;
            refreshSlots();
            showBase();
            setStatus("옷을 모두 벗겼습니다.");
        });
        codiSaveButton.addActionListener(e -> saveToCodiBook());
        codiBookButton.addActionListener(e -> openCodiBook());
        lockerButton.addActionListener(e -> saveToLocker());
        openLockerButton.addActionListener(e -> openLocker());
        downloadButton.addActionListener(e -> download());
        cartButton.addActionListener(e -> addToCart());
        settingsButton.addActionListener(e -> editSettings());

        JPanel statusRow = new JPanel(new BorderLayout(12, 0));
        statusRow.setOpaque(false);
        statusRow.add(status, BorderLayout.CENTER);
        statusRow.add(progress, BorderLayout.WEST);
        statusRow.add(total, BorderLayout.EAST);

        // 버튼이 많아 창이 좁거나 글씨가 크면 다음 줄로 넘긴다
        JPanel buttons = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        buttons.setOpaque(false);
        buttons.add(generateButton);
        buttons.add(retryButton);
        buttons.add(undressButton);
        buttons.add(codiSaveButton);
        buttons.add(codiBookButton);
        buttons.add(lockerButton);
        buttons.add(openLockerButton);
        buttons.add(downloadButton);
        buttons.add(cartButton);
        buttons.add(settingsButton);

        JPanel actions = new JPanel(new BorderLayout(0, 8));
        actions.setOpaque(false);
        actions.setOpaque(true);
        actions.setBackground(Theme.PAGE);
        actions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(10, 48, 12, 48)));
        actions.add(statusRow, BorderLayout.NORTH);
        actions.add(buttons, BorderLayout.CENTER);
        return actions;
    }

    // ── 캐릭터 / 내 사진 ──

    private void selectStyle(CharacterStyle option) {
        if (working) {
            return;
        }
        boolean changed = photoMode || style != option;
        style = option;
        photoMode = false;
        if (changed) {
            showBase();
            setStatus("그림체를 바꿨습니다. [입혀보기]를 누르면 이 캐릭터가 옷을 입습니다.");
        }
    }

    /** [내 사진으로 입어보기]: 사진이 없으면 사진 올리기 창을 연다. 취소하면 전에 고른 그림체로 돌아간다. */
    private void selectPhotoMode() {
        if (working) {
            return;
        }
        if (photo == null && !uploadPhoto()) {
            styleChips.get(style).setSelected(true);
            return;
        }
        if (!photoMode) {
            photoMode = true;
            showBase();
        }
        setStatus("사진에 입힐 옷과 배경을 고르고 [입혀보기]를 눌러 주세요.");
    }

    /** 사진 올리기 창. 사진을 고르고 [이 사진으로 입어보기]를 누르면 true. */
    private boolean uploadPhoto() {
        PhotoUploadDialog dialog = new PhotoUploadDialog(SwingUtilities.getWindowAncestor(this), context.dressUp, photoAgreed);
        dialog.setVisible(true);
        photoAgreed = dialog.agreed();
        if (dialog.result() == null) {
            return false;
        }
        photo = dialog.result();
        keepTile.setPhoto(photo.image());
        return true;
    }

    private void changePhoto() {
        if (!working && uploadPhoto()) {
            showBase();
            setStatus("사진을 바꿨습니다. [입혀보기]를 누르면 이 사진에 입힙니다.");
        }
    }

    /** [사진 지우기]: 올린 사진과 그 결과를 메모리에서 지우고 캐릭터로 돌아간다 (NFR-08). */
    private void clearPhoto() {
        if (working) {
            return;
        }
        photo = null;
        keepTile.setPhoto(null);
        photoMode = false;
        styleChips.get(style).setSelected(true);
        showBase();
        setStatus("올린 사진을 지웠습니다.");
    }

    /** 입히기 전 모습: 캐릭터(신체 스펙 반영) 또는 올린 사진. 전에 만든 결과는 치운다. */
    private void showBase() {
        resultImage = null;
        retryButton.setVisible(false);
        below.removeAll();
        if (photoMode) {
            characterView.setImage(photo.image());
            stageTag.setText("내가 올린 사진");
            below.add(backgroundCard, BorderLayout.CENTER);
        } else {
            try {
                characterView.setImage(context.dressUp.characterImage(style, spec));
            } catch (RuntimeException e) {
                characterView.setImage(null);
                characterView.setMessage("캐릭터 그림을 불러오지 못했습니다");
            }
            stageTag.setText(null);
            below.add(specButton, BorderLayout.CENTER);
        }
        photoButtons.setVisible(photoMode);
        updateScene();
        below.revalidate();
        below.repaint();
        updateButtons();
    }

    /**
     * 그림 칸 좌우 여백에 깔 장소. 내 사진에 배경을 골랐으면 그 배경 사진을 흐리게 깐다 (입혀 보기 전에는 미리보기,
     * 뒤에는 결과 사진의 장소와 이어진다). 그 밖(캐릭터, 원래 배경)에는 null → 그림 자체를 흐리게 늘려 채운다.
     */
    private void updateScene() {
        if (!photoMode || background == null) {
            characterView.setScene(null);
            return;
        }
        String url = background.imageUrl();
        BufferedImage scene = com.sadamda.view.ProductImages.get(url);
        if (scene == null && !com.sadamda.view.ProductImages.failed(url)) {
            com.sadamda.view.ProductImages.request(url, this::updateScene); // 받으면 다시
        }
        characterView.setScene(scene);
    }

    /** [크게 보기] 또는 그림 칸 누르기: 지금 보이는 그림을 크게 본다. */
    private void openViewer() {
        BufferedImage shown = characterView.image();
        if (shown == null) {
            return;
        }
        String title = resultImage != null ? "입혀 본 결과" : photoMode ? "내가 올린 사진" : style.label() + " 캐릭터";
        new ImageViewerDialog(SwingUtilities.getWindowAncestor(this), shown, title).setVisible(true);
    }

    // ── 배경 ──

    /**
     * 배경 줄: [원래 배경] + 고른 계절의 배경 전부 ("전체"면 20개). 옆으로 넘겨 본다.
     * 고른 배경이 이 줄에 있으면 그 버튼이 보이게, 없으면 처음으로 넘긴다.
     */
    private void refreshStrip() {
        tiles.clear();
        List<BackgroundTile> row = new ArrayList<>();
        List<Runnable> picks = new ArrayList<>();
        keepTile = sliderTile(null);
        keepTile.setPhoto(photo == null ? null : photo.image());
        row.add(keepTile);
        picks.add(() -> chooseBackground(null));
        BackgroundTile chosenTile = background == null ? keepTile : null;
        for (Background each : backgrounds) {
            if (ALL_SEASONS.equals(season) || each.season().equals(season)) {
                BackgroundTile tile = sliderTile(each);
                tiles.add(tile);
                row.add(tile);
                picks.add(() -> chooseBackground(each));
                if (each.equals(background)) {
                    chosenTile = tile;
                }
            }
        }
        slider.setTiles(row, picks);
        updateChosen();
        if (chosenTile != null && chosenTile != keepTile) {
            slider.show(chosenTile);
        } else {
            slider.scrollToStart();
        }
        updateButtons();
    }

    private BackgroundTile sliderTile(Background each) {
        BackgroundTile tile = new BackgroundTile(each);
        tile.setPreferredSize(new Dimension(132, 104));
        return tile;
    }

    /** 고른 배경 버튼에만 굵은 테두리와 ✓. 줄은 그대로 두어 넘겨 둔 위치가 바뀌지 않는다. */
    private void updateChosen() {
        keepTile.setChosen(background == null);
        tiles.forEach(tile -> tile.setChosen(tile.background().equals(background)));
    }

    private void chooseBackground(Background chosen) {
        if (working) {
            return;
        }
        background = chosen;
        updateChosen();
        updateScene();
        updateButtons();
        setStatus(chosen == null ? "사진의 원래 배경을 그대로 둡니다."
                : "배경: " + chosen.name() + ". [입혀보기]를 누르면 이 장소에서 찍은 사진처럼 바꿉니다.");
    }

    private void openAllBackgrounds() {
        if (working) {
            return;
        }
        BackgroundDialog dialog = new BackgroundDialog(SwingUtilities.getWindowAncestor(this), backgrounds, background,
                photo == null ? null : photo.image());
        dialog.setVisible(true);
        if (dialog.picked()) {
            if (dialog.chosen() != null && !ALL_SEASONS.equals(season)) {
                season = dialog.chosen().season();
                seasonChips.get(season).setSelected(true);
            }
            chooseBackground(dialog.chosen());
            refreshStrip(); // 고른 배경이 보이게 넘긴다
        }
    }

    // ── 아이템 ──

    /** 칸이나 [한꺼번에 고르기]: 아이템 고르기 창을 열고, 닫으면 고른 것을 반영한다. */
    private void openPicker(DressUpSlot start) {
        if (working) {
            return;
        }
        List<Product> before = outfit.products();
        ItemPickerDialog dialog = new ItemPickerDialog(SwingUtilities.getWindowAncestor(this), outfit, start,
                context.dressUp::productsFor);
        dialog.setVisible(true);
        outfit.replaceWith(dialog.result());
        refreshSlots();
        if (!before.equals(outfit.products())) {
            outfitChanged();
            setStatus(outfit.isEmpty() ? "입힐 옷을 골라 주세요."
                    : "고른 아이템을 반영했습니다. [입혀보기]를 누르면 " + (photoMode ? "내 사진에" : "캐릭터에") + " 입힙니다.");
        }
    }

    /**
     * 상품 상세의 [AI로 입어보기]에서 부른다. 이 상품을 맞는 칸에 넣고, AI 가 연결되어 있으면 바로 입혀 본다.
     * 맞는 칸이 없는 상품(종류를 알 수 없는 주얼리)이면 아무것도 하지 않고 false.
     */
    public boolean wear(Long productId) {
        Product product = context.product.detail(productId);
        DressUpSlot slot = DressUpSlot.slotFor(product);
        if (slot == null) {
            return false;
        }
        if (working) {
            return true; // 그리는 중에는 고른 옷을 바꾸지 않는다
        }
        List<DressUpSlot> removed = outfit.put(slot, product);
        outfitChanged();
        refreshSlots();
        if (context.dressUp.dressUpConnected()) {
            generate();
        } else {
            setStatus(slot.label() + " 칸에 넣었습니다." + takenOff(slot, removed)
                    + " 그림 만드는 곳(AI)에 연결되지 않아 입혀 볼 수 없습니다. [AI 연결 설정]에서 이용 열쇠를 넣어 주세요.");
        }
        return true;
    }

    private static String takenOff(DressUpSlot slot, List<DressUpSlot> removed) {
        if (removed.isEmpty()) {
            return "";
        }
        String last = removed.get(removed.size() - 1).label();
        return " " + slot.label() + ItemPickerDialog.object(slot.label()) + " 입혀서 "
                + String.join("·", removed.stream().map(DressUpSlot::label).toList()) + ItemPickerDialog.object(last)
                + " 벗겼습니다.";
    }

    private void refreshSlots() {
        slotCards.values().forEach(SlotCard::refreshText);
        total.setText(outfit.isEmpty() ? " " : "고른 옷 합계 " + String.format("%,d원", outfit.totalPrice()));
        updateButtons();
    }

    // ── 신체 스펙·AI 설정 ──

    /** [캐릭터 신체 스펙 정함]: 키·몸무게·체형·연령대. 바꾸면 캐릭터 모습에 바로 반영된다. */
    private void editSpec() {
        JSpinner height = new JSpinner(new SpinnerNumberModel(spec.heightCm(),
                CharacterSpec.MIN_HEIGHT, CharacterSpec.MAX_HEIGHT, 1));
        JSpinner weight = new JSpinner(new SpinnerNumberModel(spec.weightKg(),
                CharacterSpec.MIN_WEIGHT, CharacterSpec.MAX_WEIGHT, 1));
        JComboBox<String> bodyType = Theme.comboBox(CharacterSpec.BODY_TYPES.toArray(new String[0]));
        bodyType.setSelectedItem(spec.bodyType());
        JComboBox<String> ageGroup = Theme.comboBox(CharacterSpec.AGE_GROUPS.toArray(new String[0]));
        ageGroup.setSelectedItem(spec.ageGroup());

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.add(Theme.label("키 (cm)", 1f, Font.BOLD, Theme.TEXT));
        form.add(height);
        form.add(Theme.label("몸무게 (kg)", 1f, Font.BOLD, Theme.TEXT));
        form.add(weight);
        form.add(Theme.label("체형", 1f, Font.BOLD, Theme.TEXT));
        form.add(bodyType);
        form.add(Theme.label("연령대", 1f, Font.BOLD, Theme.TEXT));
        form.add(ageGroup);

        Object[] choices = {"확인", "취소"};
        int answer = JOptionPane.showOptionDialog(this, form, "캐릭터 신체 스펙",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
        if (answer == 0) {
            CharacterSpec changed = new CharacterSpec((Integer) height.getValue(), (Integer) weight.getValue(),
                    (String) bodyType.getSelectedItem(), (String) ageGroup.getSelectedItem());
            if (!changed.equals(spec)) {
                spec = changed;
                showSpec();
                showBase();
                setStatus("신체 스펙을 캐릭터에 반영했습니다. [입혀보기]를 누르면 이 몸에 맞게 옷을 입힙니다.");
            }
        }
    }

    private void showSpec() {
        specButton.setText("캐릭터 신체 스펙 정함: " + spec.summary());
    }

    private void showService() {
        serviceLabel.setText("그림 만드는 곳: " + context.dressUp.dressUpServiceName());
    }

    /** [AI 연결 설정]: 이용 열쇠·서비스 주소·모델을 이 PC 에만 저장한다. */
    private void editSettings() {
        DressUpSettings saved = context.dressUp.dressUpSettings();
        JPasswordField key = Theme.passwordField("");
        key.setText(saved.apiKey() == null ? "" : saved.apiKey());
        JTextField baseUrl = Theme.textField("비우면 OpenAI 공식 주소 (타임리 키는 비워 두기)");
        baseUrl.setText(saved.baseUrl() == null ? "" : saved.baseUrl());
        JTextField model = Theme.textField("비우면 기본 모델");
        model.setText(saved.model() == null ? "" : saved.model());
        for (JTextField field : List.of(key, baseUrl, model)) {
            field.setColumns(28);
        }

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(Theme.label("이용 열쇠 (API 키)", 1f, Font.BOLD, Theme.TEXT));
        form.add(key);
        form.add(Theme.label("서비스 주소 (중계 서비스를 쓸 때만)", 1f, Font.BOLD, Theme.TEXT));
        form.add(baseUrl);
        form.add(Theme.label("모델 이름", 1f, Font.BOLD, Theme.TEXT));
        form.add(model);
        form.add(Theme.label(context.dressUp.dressUpKeyFromEnvironment()
                ? "환경 변수에도 열쇠가 있습니다. 여기에 열쇠를 넣으면 이 열쇠를 먼저 씁니다."
                : "이 PC 에만 저장되고, 다른 사람에게 보내는 파일에는 들어가지 않습니다.", 1f, Font.PLAIN, Theme.HINT));

        Object[] choices = {"저장", "취소"};
        int answer = JOptionPane.showOptionDialog(this, form, "AI 연결 설정",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
        if (answer == 0) {
            context.dressUp.configureDressUp(new String(key.getPassword()), baseUrl.getText(), model.getText());
            showService();
            setStatus(context.dressUp.dressUpConnected()
                    ? "AI 연결 설정을 저장했습니다. [입혀보기]를 눌러 확인해 주세요."
                    : "이용 열쇠가 비어 있어 아직 연결되지 않았습니다.");
            updateButtons();
        }
    }

    // ── 입혀보기·결과 ──

    /** UC-09 5~8단계: 합성 요청 → 진행 상태 표시 → 결과를 크게 보여준다. 실패하면 [다시 시도] (7b). */
    private void generate() {
        if (working) {
            return;
        }
        if (!context.dressUp.dressUpConnected()) {
            setStatus("그림 만드는 곳(AI)에 연결되지 않았습니다. 이용 열쇠를 넣어 주세요.");
            editSettings();
            if (!context.dressUp.dressUpConnected()) {
                return;
            }
        }
        boolean requestedPhotoMode = photoMode && photo != null;
        UserPhoto requestedPhoto = photo;
        Background requestedBackground = background;
        CharacterStyle requestedStyle = style;
        CharacterSpec requestedSpec = spec;
        Outfit requestedOutfit = outfit.copy();
        int requestedVersion = outfitVersion;

        working = true;
        retryButton.setVisible(false);
        progress.setVisible(true);
        String doing = !requestedPhotoMode ? "캐릭터에게 옷을 입히는 중입니다."
                : requestedOutfit.isEmpty() ? "사진의 배경을 바꾸는 중입니다."
                : requestedBackground == null ? "내 사진에 옷을 입히는 중입니다."
                : "내 사진에 옷을 입히고 배경을 바꾸는 중입니다.";
        setStatus(doing + " 잠시만 기다려 주세요 (최대 " + context.dressUp.dressUpTimeoutSeconds() + "초)");
        updateButtons();

        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() throws Exception {
                return requestedPhotoMode
                        ? context.dressUp.dressUpPhoto(requestedPhoto, requestedBackground, requestedOutfit)
                        : context.dressUp.dressUp(requestedStyle, requestedSpec, requestedOutfit);
            }

            @Override
            protected void done() {
                working = false;
                progress.setVisible(false);
                try {
                    byte[] image = get();
                    BufferedImage decoded = RemoteImages.decode(image);
                    if (decoded == null) {
                        throw new DressUpException("받은 그림을 열 수 없습니다. 다시 시도해 주세요.");
                    }
                    resultImage = image;
                    resultVersion = requestedVersion;
                    resultFromPhoto = requestedPhotoMode;
                    characterView.setImage(decoded);
                    updateScene();
                    SwingUtilities.invokeLater(() -> openViewer()); // 결과는 바로 크게 보여준다
                    stageTag.setText(requestedPhotoMode ? "입혀 본 결과" : null);
                    setStatus("다 입혔습니다. 옷" + (requestedPhotoMode ? "과 배경" : "") + "을 바꿔 가며 계속 입혀 보세요.");
                } catch (ExecutionException e) {
                    fail(e.getCause() instanceof DressUpException
                            ? e.getCause().getMessage() : "그림을 만드는 중에 문제가 생겼습니다.");
                } catch (DressUpException e) {
                    fail(e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("그림 만들기를 멈췄습니다.");
                }
                updateButtons();
            }
        }.execute();
    }

    private void fail(String reason) {
        setStatus("입혀보기에 실패했습니다. " + reason);
        retryButton.setVisible(true);
    }

    // ── 내 코디북 (UC-19) · 코디 도우미 (UC-20) ──

    private void outfitChanged() {
        outfitVersion++;
    }

    /**
     * UC-20 상황별 코디 도우미를 연다. occasion 을 주면 그 상황을 골라 둔 채로 연다 (홈 화면의 상황 버튼).
     * [이 코디 입혀보기]를 고르면 칸을 그 코디로 바꾸고, AI 가 연결되어 있으면 바로 입혀 본다.
     */
    public void openHelper(String occasion) {
        if (working) {
            return;
        }
        CodiHelperDialog dialog = new CodiHelperDialog(SwingUtilities.getWindowAncestor(this),
                context.recommend, spec, occasion);
        dialog.setVisible(true);
        if (dialog.chosen() != null) {
            wearOutfit(dialog.chosen());
        }
    }

    /**
     * 추천 코디 한 벌을 칸에 넣고, AI 가 연결되어 있으면 바로 입혀 본다.
     * 코디 도우미(UC-20)와 옷장 출석의 오늘의 코디(UC-21)에서 부른다.
     */
    public void wearOutfit(OutfitSuggestion chosen) {
        if (working || chosen == null) {
            return;
        }
        outfit.clear();
        for (Product product : chosen.products()) {
            DressUpSlot slot = DressUpSlot.slotFor(product);
            if (slot != null) {
                outfit.put(slot, product);
            }
        }
        outfitChanged();
        openedCodi = null;
        suggestedTitle = chosen.title();
        refreshSlots();
        showBase();
        if (context.dressUp.dressUpConnected()) {
            generate();
        } else {
            setStatus("'" + chosen.title() + "'를 칸에 넣었습니다. 마음에 들면 [코디북에 저장]으로 모아 두세요.");
        }
    }

    /** UC-19 [코디북에 저장]: 고른 옷과 (입혀 봤으면) 그림을 이름을 붙여 저장한다. 다시 연 코디면 덮어쓸지 묻는다. */
    private void saveToCodiBook() {
        if (!context.codiBook.isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "코디북은 로그인한 뒤에 쓸 수 있습니다.");
            return;
        }
        // 지금 옷차림으로 그린 캐릭터 그림만 함께 남긴다. 올린 사진으로 만든 그림은 남기지 않는다 (NFR-08).
        byte[] image = resultImage != null && resultVersion == outfitVersion && !resultFromPhoto ? resultImage : null;
        try {
            if (openedCodi != null) {
                Object[] choices = {"'" + openedCodi.title() + "'에 덮어쓰기", "새 코디로 저장", "취소"};
                int answer = JOptionPane.showOptionDialog(this,
                        "코디북에서 연 코디입니다. 고친 내용을 어떻게 저장할까요?", "코디북에 저장",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
                if (answer == 0) {
                    openedCodi = context.codiBook.overwrite(openedCodi.id(), style, spec, outfit, image);
                    setStatus("'" + openedCodi.title() + "' 코디를 고친 내용으로 저장했습니다.");
                    return;
                }
                if (answer != 1) {
                    return;
                }
            }
            String defaultTitle = suggestedTitle != null ? suggestedTitle
                    : "내 코디 " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("M월 d일"));
            Object title = JOptionPane.showInputDialog(this,
                    "코디 이름을 넣어 주세요. 예: 딸 결혼식 룩, 제주 여행 첫날", "코디북에 저장",
                    JOptionPane.PLAIN_MESSAGE, null, null, defaultTitle);
            if (title == null) {
                return;
            }
            openedCodi = context.codiBook.save(title.toString(), style, spec, outfit, image);
            suggestedTitle = null;
            setStatus("'" + openedCodi.title() + "'를 코디북에 저장했습니다."
                    + (image == null ? " 그림은 캐릭터로 [입혀보기]를 한 뒤 다시 저장하면 함께 남습니다." : ""));
        } catch (LoginRequiredException e) {
            JOptionPane.showMessageDialog(this, "코디북은 로그인한 뒤에 쓸 수 있습니다.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "코디북에 저장하지 못했습니다.\n" + e.getMessage());
        }
    }

    /** UC-19 [내 코디북]: 저장한 코디를 고르면 그림체·신체 스펙·옷·그림을 그대로 되살린다 (캐릭터로 돌아간다). */
    private void openCodiBook() {
        if (!context.codiBook.isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "코디북은 로그인한 뒤에 쓸 수 있습니다.");
            return;
        }
        SavedCodi chosen;
        try {
            CodiBookDialog dialog = new CodiBookDialog(SwingUtilities.getWindowAncestor(this), context.codiBook);
            dialog.setVisible(true);
            chosen = dialog.chosen();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "코디북을 열지 못했습니다.\n" + e.getMessage());
            return;
        }
        if (chosen == null) {
            return;
        }
        CodiBookController.Opened opened;
        try {
            opened = context.codiBook.open(chosen.id());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "코디를 열지 못했습니다.\n" + e.getMessage());
            return;
        }
        style = opened.codi().style();
        photoMode = false;
        JToggleButton styleChip = styleChips.get(style);
        if (styleChip != null) {
            styleChip.setSelected(true);
        }
        spec = opened.codi().spec();
        showSpec();
        outfit.replaceWith(opened.outfit());
        outfitChanged();
        openedCodi = opened.codi();
        suggestedTitle = null;
        refreshSlots();
        showBase();
        BufferedImage decoded = opened.image() == null ? null : RemoteImages.decode(opened.image());
        if (decoded != null) {
            resultImage = opened.image();
            resultVersion = outfitVersion;
            resultFromPhoto = false;
            characterView.setImage(decoded);
            updateScene();
            updateButtons();
        }
        setStatus(opened.missing() > 0
                ? "'" + opened.codi().title() + "' 코디를 열었습니다. 판매가 끝난 상품 " + opened.missing() + "개는 빼고 넣었습니다."
                : "'" + opened.codi().title() + "' 코디를 열었습니다. 칸을 바꿔 새 코디를 만들어 보세요.");
    }

    /** UC-09 9단계 [보관함에 저장] */
    private void saveToLocker() {
        if (!context.dressUp.isLoggedIn()) {
            JOptionPane.showMessageDialog(this,
                    "보관함은 로그인한 뒤에 쓸 수 있습니다.\n지금은 [내 컴퓨터에 저장]을 이용해 주세요.");
            return;
        }
        try {
            Long productId = outfit.isEmpty() ? null : outfit.products().get(0).getId();
            context.tryOn.saveToLocker(productId, resultImage);
            setStatus("보관함에 저장했습니다.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "보관함에 저장하지 못했습니다.\n" + e.getMessage());
        }
    }

    /** FR-16 보관함 보기·지우기 (NFR-08: 사용자가 언제든 삭제 가능) */
    private void openLocker() {
        if (!context.dressUp.isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "보관함은 로그인한 뒤에 쓸 수 있습니다.");
            return;
        }
        try {
            new LockerDialog(SwingUtilities.getWindowAncestor(this), context.tryOn).setVisible(true);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "보관함을 열지 못했습니다.\n" + e.getMessage());
        }
    }

    /** UC-09 9단계 [다운로드] */
    private void download() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(photoMode ? "사담다_내사진_입어보기.png" : "사담다_옷입혀보기.png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.exists() && JOptionPane.showConfirmDialog(this, "같은 이름의 파일이 있습니다. 바꿔 저장할까요?",
                    "저장", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                Files.write(file.toPath(), resultImage);
                setStatus("저장했습니다: " + file.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "파일을 저장하지 못했습니다.\n다른 위치를 골라 주세요.");
            }
        }
    }

    /** UC-09 9단계 [장바구니 담기]: 입힌 옷을 재고가 있는 첫 옵션으로 1개씩 담는다. 사이즈는 장바구니에서 바꾼다. */
    private void addToCart() {
        List<OrderLine> lines = new ArrayList<>();
        for (Product product : outfit.products()) {
            ProductOption option = product.getOptions().stream()
                    .filter(candidate -> candidate.stock() > 0).findFirst().orElse(null);
            if (option != null) {
                lines.add(new OrderLine(product.getId(), option.id(), 1));
            }
        }
        if (lines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "담을 수 있는 상품이 없습니다. 모두 품절입니다.");
            return;
        }
        try {
            context.cart.addAll(lines); // Controller 명세: 여러 상품은 addAll(List<OrderLine>)
            setStatus(lines.size() + "개 상품을 장바구니에 담았습니다. 사이즈는 장바구니에서 바꿀 수 있습니다.");
            // 상품 상세와 같은 방식: 상단 [장바구니 n]을 바로 고치고, 잠깐 뜨는 알림에 [장바구니 보기]
            frame.refreshCartBadge();
            Toast.show(this, "장바구니에 담았어요", "장바구니 보기", () -> frame.navigate(CartPanel.NAME));
        } catch (LoginRequiredException e) {
            JOptionPane.showMessageDialog(this, "장바구니는 로그인한 뒤에 쓸 수 있습니다.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "장바구니에 담지 못했습니다.\n" + e.getMessage());
        }
    }

    private void updateButtons() {
        boolean hasResult = resultImage != null;
        boolean hasOutfit = !outfit.isEmpty();
        boolean canGenerate = photoMode ? photo != null && (hasOutfit || background != null) : hasOutfit;
        generateButton.setEnabled(!working && canGenerate);
        retryButton.setEnabled(!working && canGenerate);
        specButton.setEnabled(!working);
        settingsButton.setEnabled(!working);
        undressButton.setEnabled(!working && (hasOutfit || hasResult));
        lockerButton.setEnabled(!working && hasResult);
        downloadButton.setEnabled(!working && hasResult);
        openLockerButton.setEnabled(!working);
        cartButton.setEnabled(!working && hasOutfit);
        helperButton.setEnabled(!working);
        codiSaveButton.setEnabled(!working && hasOutfit);
        codiBookButton.setEnabled(!working);
        pickAllButton.setEnabled(!working);
        changePhotoButton.setEnabled(!working);
        zoomButton.setEnabled(!working);
        clearPhotoButton.setEnabled(!working);
        allBackgroundsButton.setEnabled(!working);
        photoChip.setEnabled(!working);
        styleChips.values().forEach(chip -> chip.setEnabled(!working));
        seasonChips.values().forEach(chip -> chip.setEnabled(!working));
        if (keepTile != null) {
            keepTile.setEnabled(!working);
        }
        tiles.forEach(tile -> tile.setEnabled(!working));
        slider.setEnabled(!working);
        slotCards.values().forEach(card -> card.setEnabled(!working));
    }

    private static void invalidateTree(Component component) {
        component.invalidate();
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                invalidateTree(child);
            }
        }
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    /** 큰 그림 칸 위에 이름표(왼쪽 위)와 사진 버튼(오른쪽 아래)을 겹쳐 둔다. */
    private static class Stage extends JLayeredPane {
        private final JComponent view;
        private final JComponent tag;
        private final JComponent buttons;
        private final JComponent zoom;

        Stage(JComponent view, JComponent tag, JComponent buttons, JComponent zoom) {
            this.view = view;
            this.tag = tag;
            this.buttons = buttons;
            this.zoom = zoom;
            add(view, JLayeredPane.DEFAULT_LAYER);
            add(tag, JLayeredPane.PALETTE_LAYER);
            add(buttons, JLayeredPane.PALETTE_LAYER);
            add(zoom, JLayeredPane.PALETTE_LAYER);
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            int h = getHeight();
            view.setBounds(0, 0, w, h);
            Dimension tagSize = tag.getPreferredSize();
            tag.setBounds(16, 16, tagSize.width, tagSize.height);
            Dimension buttonsSize = buttons.getPreferredSize();
            buttons.setBounds(w - buttonsSize.width - 16, h - buttonsSize.height - 8, buttonsSize.width, buttonsSize.height);
            Dimension zoomSize = zoom.getPreferredSize();
            zoom.setBounds(w - zoomSize.width - 16, 16, zoomSize.width, zoomSize.height);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(480, 260);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(300, 180);
        }
    }

    /** 그림 칸 왼쪽 위의 검정 이름표 ("내가 올린 사진" / "입혀 본 결과"). 글이 없으면 보이지 않는다. */
    private static class TagLabel extends JLabel {
        TagLabel() {
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        }

        @Override
        public void updateUI() {
            super.updateUI();
            Font base = UIManager.getFont("Label.font");
            setFont(base.deriveFont(Font.BOLD, base.getSize2D() * 0.9f));
        }

        @Override
        public void setText(String text) {
            super.setText(text);
            setVisible(text != null && !text.isEmpty());
            if (getParent() != null) {
                getParent().doLayout();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = TryOnUi.smooth(graphics);
            g.setColor(Theme.PRIMARY);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /**
     * 입힐 아이템 한 칸: 작은 사진, 자리 이름(굵게), 상품명, 가격. 비어 있으면 "+" 와 "눌러서 고르기".
     * 입힌 칸은 굵은 검정 테두리. 누르면 그 자리로 아이템 고르기 창이 열린다.
     */
    private class SlotCard extends JButton {
        private final DressUpSlot slot;

        SlotCard(DressUpSlot slot) {
            this.slot = slot;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
            setFont(UIManager.getFont("Label.font"));
        }

        @Override
        public Dimension getPreferredSize() {
            Font font = getFont();
            FontMetrics bold = getFontMetrics(font.deriveFont(Font.BOLD));
            FontMetrics small = getFontMetrics(font.deriveFont(font.getSize2D() * 0.8f));
            return new Dimension(200, Math.max(68, bold.getHeight() + small.getHeight() * 2 + 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = TryOnUi.smooth(graphics);
            Product product = outfit.get(slot);
            boolean worn = product != null;
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 18, 18);
            g.setColor(getModel().isPressed() ? Theme.SECONDARY : Theme.CARD);
            g.fill(shape);
            g.setColor(worn || isFocusOwner() ? Theme.PRIMARY : Theme.LINE);
            g.setStroke(new BasicStroke(worn ? 2.4f : isFocusOwner() ? 2f : 1f));
            g.draw(shape);

            int thumbX = 10;
            int thumbY = (h - SLOT_THUMBNAIL) / 2;
            if (worn) {
                ImageIcon icon = Thumbnails.get(product.mainImageUrl(), SLOT_THUMBNAIL, this::repaint);
                Graphics2D clipped = (Graphics2D) g.create();
                clipped.clip(new RoundRectangle2D.Float(thumbX, thumbY, SLOT_THUMBNAIL, SLOT_THUMBNAIL, 10, 10));
                icon.paintIcon(this, clipped, thumbX, thumbY);
                clipped.dispose();
            } else {
                g.setColor(new Color(0xEBEBEB));
                g.fill(new RoundRectangle2D.Float(thumbX, thumbY, SLOT_THUMBNAIL, SLOT_THUMBNAIL, 10, 10));
                g.setColor(Theme.HINT);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                float c = SLOT_THUMBNAIL / 2f;
                g.draw(new Line2D.Float(thumbX + c - 7, thumbY + c, thumbX + c + 7, thumbY + c));
                g.draw(new Line2D.Float(thumbX + c, thumbY + c - 7, thumbX + c, thumbY + c + 7));
            }

            Font base = getFont();
            Font bold = base.deriveFont(Font.BOLD);
            Font small = base.deriveFont(Font.PLAIN, base.getSize2D() * 0.8f);
            FontMetrics bm = g.getFontMetrics(bold);
            FontMetrics sm = g.getFontMetrics(small);
            int textX = thumbX + SLOT_THUMBNAIL + 10;
            int textWidth = w - textX - 10;
            int lines = worn ? 3 : 2;
            int blockHeight = bm.getHeight() + sm.getHeight() * (lines - 1);
            int y = (h - blockHeight) / 2 + bm.getAscent();
            g.setColor(isEnabled() ? Theme.TEXT : Theme.HINT);
            g.setFont(bold);
            g.drawString(TryOnUi.ellipsize(slot.label(), bm, textWidth), textX, y);
            g.setFont(small);
            y += bm.getDescent() + sm.getAscent();
            if (worn) {
                g.drawString(TryOnUi.ellipsize(product.getName(), sm, textWidth), textX, y);
                y += sm.getHeight();
                g.drawString(String.format("%,d원", product.getPrice()), textX, y);
            } else {
                g.setColor(Theme.HINT);
                g.drawString("눌러서 고르기", textX, y);
            }
            g.dispose();
        }

        /** 마우스를 올렸을 때의 설명과 화면 읽기 프로그램이 읽을 이름. */
        void refreshText() {
            Product product = outfit.get(slot);
            setToolTipText(product == null ? null : product.getName() + " · " + String.format("%,d원", product.getPrice()));
            getAccessibleContext().setAccessibleName(slot.label()
                    + (product == null ? ": 비어 있음, 눌러서 고르기" : ": " + product.getName()));
            repaint();
        }
    }

    /**
     * 맨 아래 버튼 줄을 뺀 화면 내용. 폭은 창에 맞춘다. 높이는 창이 크면 큰 그림 칸이 늘어나고,
     * 작으면 그림 칸이 최소 높이까지 줄어든 다음에야 세로 스크롤이 생긴다.
     */
    private static class Page extends JPanel implements Scrollable {
        private final JComponent flexible;

        /** @param flexible 창이 작을 때 먼저 줄어드는 칸 (큰 그림 칸) */
        Page(JComponent flexible) {
            super(new BorderLayout());
            this.flexible = flexible;
            setBackground(Theme.PAGE);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 48);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        /** 그림 칸을 최소 높이까지 줄이면 창에 다 들어가는 동안은 창 높이에 맞춘다 (스크롤 없음). */
        @Override
        public boolean getScrollableTracksViewportHeight() {
            if (!(getParent() instanceof JViewport viewport)) {
                return false;
            }
            int slack = Math.max(0, flexible.getPreferredSize().height - flexible.getMinimumSize().height);
            return viewport.getHeight() >= getPreferredSize().height - slack;
        }
    }
}
