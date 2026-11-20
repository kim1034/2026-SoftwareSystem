package com.sadamda.view.tryon;

import com.sadamda.common.LoginRequiredException;
import com.sadamda.controller.AppContext;
import com.sadamda.controller.CodiBookController;
import com.sadamda.model.recommend.OutfitSuggestion;
import com.sadamda.model.tryon.SavedCodi;
import com.sadamda.external.DressUpException;
import com.sadamda.external.DressUpSettings;
import com.sadamda.external.RemoteImages;
import com.sadamda.model.order.OrderLine;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.view.MainFrame;
import com.sadamda.view.ScreenPanel;
import com.sadamda.view.Theme;
import com.sadamda.view.Toast;
import com.sadamda.view.WrapLayout;
import com.sadamda.view.cart.CartPanel;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 옷 입혀보기 (UC-09 확장): 여성 캐릭터에 모자·상의·하의·주얼리·신발을 입혀 본다.
 *
 * 화면 구성 (구상 그림 기준)
 * - 왼쪽: 큰 캐릭터 칸. 그 위에 그림체 4가지(실사·디즈니 캐릭터·3D 게임 캐릭터·애니메이션 캐릭터)
 * - 왼쪽 아래: [캐릭터 신체 스펙 정함] 키·몸무게·체형·연령대. 바꾸면 캐릭터 모습이 바로 바뀐다
 * - 오른쪽: 모자 → 상의 → 하의 → 주얼리 → 신발 칸. 누르면 상품을 고른다
 * - 맨 아래: [입혀보기] 와 결과 처리 버튼 (UC-09 9단계)
 *
 * 캐릭터 칸에는 옷을 입히기 전에는 기본 캐릭터가, [입혀보기] 뒤에는 AI 가 옷을 입힌 그림이 보인다.
 */
public class TryOnPanel extends ScreenPanel {
    public static final String NAME = "tryOn";
    private static final int SLOT_THUMBNAIL = 84;

    private final Outfit outfit = new Outfit();
    private final Map<DressUpSlot, JButton> slotButtons = new EnumMap<>(DressUpSlot.class);
    private final CharacterView characterView = new CharacterView();
    private final JLabel serviceLabel = Theme.label(" ", 1f, Font.PLAIN, Theme.HINT);
    private final JButton specButton = bigButton("");
    private final JLabel status = Theme.label(" ", 1f, Font.PLAIN, Theme.TEXT);
    private final JLabel total = Theme.label(" ", 1.1f, Font.BOLD, Theme.TEXT);
    private final JProgressBar progress = new JProgressBar();
    private final JButton generateButton = compact(Theme.primaryButton("입혀보기"));
    private final JButton retryButton = bigButton("다시 시도");
    private final JButton undressButton = bigButton("모두 벗기");
    private final JButton lockerButton = bigButton("보관함에 저장");
    private final JButton openLockerButton = bigButton("내 보관함");
    private final JButton downloadButton = bigButton("내 컴퓨터에 저장");
    private final JButton cartButton = bigButton("장바구니 담기");
    private final JButton settingsButton = bigButton("AI 연결 설정");
    private final JButton helperButton = compact(Theme.primaryButton("어디 가세요? 코디 추천받기"));
    private final JButton codiSaveButton = bigButton("코디북에 저장");
    private final JButton codiBookButton = bigButton("내 코디북");
    private final Map<CharacterStyle, JToggleButton> styleButtons = new EnumMap<>(CharacterStyle.class);

    private CharacterStyle style = CharacterStyle.REAL;
    private CharacterSpec spec;
    private byte[] resultImage;
    private boolean working;
    /** 고른 옷이 바뀔 때마다 1씩 는다. 그림이 지금 옷차림으로 그린 것인지 확인할 때 쓴다. */
    private int outfitVersion;
    /** resultImage 를 그릴 때의 outfitVersion. */
    private int resultVersion = -1;
    /** 코디북에서 다시 연 코디 (UC-19). [코디북에 저장]이 덮어쓸지 묻는다. 없으면 null. */
    private SavedCodi openedCodi;
    /** 코디 도우미(UC-20)에서 고른 코디 이름. 코디북에 저장할 때 이름 칸에 먼저 넣어 둔다. */
    private String suggestedTitle;

    public TryOnPanel(AppContext context, MainFrame frame) {
        super(context, frame, "AI 입어보기", "UC-09 AI 착용 이미지 생성");
        removeAll(); // 공통 부모가 넣어 둔 "TODO" 안내를 걷어내고 실제 화면을 넣는다
        setBackground(Theme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(24, 48, 24, 48));
        spec = context.dressUp.initialSpec();

        add(header(), BorderLayout.NORTH);
        add(characterColumn(), BorderLayout.CENTER);
        add(slotColumn(), BorderLayout.EAST);
        add(actions(), BorderLayout.SOUTH);

        showService();
        showSpec();
        showBaseCharacter();
        refreshSlots();
        setStatus(context.dressUp.dressUpConnected()
                ? "옷을 고르고 [입혀보기]를 눌러 주세요."
                : "그림 만드는 곳(AI)에 연결되지 않았습니다. [AI 연결 설정]에서 이용 열쇠를 넣어 주세요.");
    }

    private JPanel header() {
        // 상품 화면 머리말과 같은 모양: 큰 제목 + 아래 구분선
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(0, 0, 16, 0)));
        header.add(Theme.label("옷 입혀보기", 1.6f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        header.add(serviceLabel, BorderLayout.EAST);
        return header;
    }

    /** 왼쪽: 그림체 고르기 + 캐릭터 + 신체 스펙. */
    private JPanel characterColumn() {
        // 2×2: "애니메이션 캐릭터"처럼 긴 이름도 글씨를 키운 상태에서 잘리지 않게
        JPanel styles = new JPanel(new GridLayout(0, 2, 8, 8));
        styles.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (CharacterStyle option : CharacterStyle.values()) {
            JToggleButton button = Theme.chip(option.label());
            button.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            button.setSelected(option == style);
            styleButtons.put(option, button);
            button.addActionListener(e -> {
                if (style != option && !working) {
                    style = option;
                    showBaseCharacter();
                    setStatus("그림체를 바꿨습니다. [입혀보기]를 누르면 이 캐릭터가 옷을 입습니다.");
                }
            });
            group.add(button);
            styles.add(button);
        }
        specButton.addActionListener(e -> editSpec());

        JPanel column = new JPanel(new BorderLayout(0, 12));
        column.setOpaque(false);
        column.add(styles, BorderLayout.NORTH);
        column.add(characterView, BorderLayout.CENTER);
        column.add(specButton, BorderLayout.SOUTH);
        return column;
    }

    /** 오른쪽: [코디 추천받기] + 모자·상의·하의·주얼리·신발 칸. */
    private JPanel slotColumn() {
        JPanel slots = new JPanel(new GridLayout(0, 1, 0, 12));
        slots.setOpaque(false);
        for (DressUpSlot slot : DressUpSlot.values()) {
            JButton button = slotButton();
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setIconTextGap(12);
            button.setPreferredSize(new Dimension(380, 104));
            button.addActionListener(e -> pick(slot));
            slotButtons.put(slot, button);
            slots.add(button);
        }
        // UC-20: 무엇을 입힐지 막막할 때 상황만 고르면 한 벌을 채워 준다
        helperButton.addActionListener(e -> openHelper(null));

        JPanel column = new JPanel(new BorderLayout(0, 12));
        column.setOpaque(false);
        column.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        column.add(helperButton, BorderLayout.NORTH);
        column.add(slots, BorderLayout.CENTER);
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
            showBaseCharacter();
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
        actions.add(statusRow, BorderLayout.NORTH);
        actions.add(buttons, BorderLayout.CENTER);
        return actions;
    }

    /** 옷을 입히기 전 모습: 그림체의 기본 캐릭터에 신체 스펙을 반영해 보여준다. 전에 만든 합성 결과는 치운다. */
    private void showBaseCharacter() {
        resultImage = null;
        retryButton.setVisible(false);
        try {
            characterView.setImage(context.dressUp.characterImage(style, spec));
        } catch (RuntimeException e) {
            characterView.setImage(null);
            characterView.setMessage("캐릭터 그림을 불러오지 못했습니다");
        }
        updateButtons();
    }

    private void showService() {
        serviceLabel.setText("그림 만드는 곳: " + context.dressUp.dressUpServiceName());
    }

    /** 자리 칸을 누르면 그 카테고리 상품을 고르는 창을 연다. */
    private void pick(DressUpSlot slot) {
        if (working) {
            return;
        }
        ProductPickerDialog dialog = new ProductPickerDialog(SwingUtilities.getWindowAncestor(this), slot,
                context.dressUp.productsFor(slot), outfit.get(slot) != null);
        dialog.setVisible(true);
        if (dialog.takeOff()) {
            outfit.remove(slot);
        } else if (dialog.chosen() != null) {
            outfit.put(slot, dialog.chosen());
        } else {
            return;
        }
        outfitChanged();
        refreshSlots();
        setStatus(outfit.isEmpty() ? "입힐 옷을 골라 주세요."
                : "옷을 바꿨습니다. [입혀보기]를 누르면 캐릭터가 입습니다.");
    }

    /**
     * 상품 상세의 [AI로 입어보기]에서 부른다. 이 상품을 맞는 칸에 넣고, AI 가 연결되어 있으면 바로 입혀 본다.
     * 맞는 칸이 없는 카테고리(원피스·가방 등)면 아무것도 하지 않고 false.
     */
    public boolean wear(Long productId) {
        Product product = context.product.detail(productId);
        DressUpSlot slot = null;
        for (DressUpSlot each : DressUpSlot.values()) {
            if (each.category() == product.getCategory()) {
                slot = each;
            }
        }
        if (slot == null) {
            return false;
        }
        if (working) {
            return true; // 그리는 중에는 고른 옷을 바꾸지 않는다
        }
        outfit.put(slot, product);
        outfitChanged();
        refreshSlots();
        if (context.dressUp.dressUpConnected()) {
            generate();
        } else {
            setStatus(slot.label() + " 칸에 넣었습니다. 그림 만드는 곳(AI)에 연결되지 않아 입혀 볼 수 없습니다. "
                    + "[AI 연결 설정]에서 이용 열쇠를 넣어 주세요.");
        }
        return true;
    }

    private void refreshSlots() {
        for (DressUpSlot slot : DressUpSlot.values()) {
            JButton button = slotButtons.get(slot);
            Product product = outfit.get(slot);
            if (product == null) {
                button.setIcon(Thumbnails.get(null, SLOT_THUMBNAIL, this::refreshSlots));
                button.setText("<html><b>" + slot.label() + "</b><br>눌러서 고르기</html>");
            } else {
                button.setIcon(Thumbnails.get(product.mainImageUrl(), SLOT_THUMBNAIL, this::refreshSlots));
                button.setText("<html><b>" + slot.label() + "</b><br>" + ProductPickerDialog.escape(shorten(product.getName()))
                        + "<br>" + String.format("%,d원", product.getPrice()) + "</html>");
            }
        }
        total.setText(outfit.isEmpty() ? " " : "고른 옷 합계 " + String.format("%,d원", outfit.totalPrice()));
        updateButtons();
    }

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
                showBaseCharacter();
                setStatus("신체 스펙을 캐릭터에 반영했습니다. [입혀보기]를 누르면 이 몸에 맞게 옷을 입힙니다.");
            }
        }
    }

    private void showSpec() {
        specButton.setText("캐릭터 신체 스펙 정함: " + spec.summary());
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
        working = true;
        retryButton.setVisible(false);
        progress.setVisible(true);
        setStatus("캐릭터에게 옷을 입히는 중입니다. 잠시만 기다려 주세요 (최대 "
                + context.dressUp.dressUpTimeoutSeconds() + "초)");
        updateButtons();

        CharacterStyle requestedStyle = style;
        CharacterSpec requestedSpec = spec;
        Outfit requestedOutfit = outfit.copy();
        int requestedVersion = outfitVersion;
        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() throws Exception {
                return context.dressUp.dressUp(requestedStyle, requestedSpec, requestedOutfit);
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
                    characterView.setImage(decoded);
                    setStatus("다 입혔습니다. 옷을 바꿔 가며 계속 입혀 보세요.");
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
        OutfitSuggestion chosen = dialog.chosen();
        if (chosen == null) {
            return;
        }
        outfit.clear();
        for (Product product : chosen.products()) {
            for (DressUpSlot slot : DressUpSlot.values()) {
                if (slot.category() == product.getCategory()) {
                    outfit.put(slot, product);
                }
            }
        }
        outfitChanged();
        openedCodi = null;
        suggestedTitle = chosen.title();
        refreshSlots();
        showBaseCharacter();
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
        byte[] image = resultImage != null && resultVersion == outfitVersion ? resultImage : null;
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
                    + (image == null ? " 그림은 [입혀보기]를 한 뒤 다시 저장하면 함께 남습니다." : ""));
        } catch (LoginRequiredException e) {
            JOptionPane.showMessageDialog(this, "코디북은 로그인한 뒤에 쓸 수 있습니다.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "코디북에 저장하지 못했습니다.\n" + e.getMessage());
        }
    }

    /** UC-19 [내 코디북]: 저장한 코디를 고르면 그림체·신체 스펙·옷·그림을 그대로 되살린다. */
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
        JToggleButton styleButton = styleButtons.get(style);
        if (styleButton != null) {
            styleButton.setSelected(true);
        }
        spec = opened.codi().spec();
        showSpec();
        outfit.clear();
        for (DressUpSlot slot : DressUpSlot.values()) {
            Product product = opened.outfit().get(slot);
            if (product != null) {
                outfit.put(slot, product);
            }
        }
        outfitChanged();
        openedCodi = opened.codi();
        suggestedTitle = null;
        refreshSlots();
        showBaseCharacter();
        BufferedImage decoded = opened.image() == null ? null : RemoteImages.decode(opened.image());
        if (decoded != null) {
            resultImage = opened.image();
            resultVersion = outfitVersion;
            characterView.setImage(decoded);
            updateButtons();
        }
        String message = "'" + opened.codi().title() + "' 코디를 열었습니다. 칸을 바꿔 새 코디를 만들어 보세요.";
        if (opened.missing() > 0) {
            message = "'" + opened.codi().title() + "' 코디를 열었습니다. 판매가 끝난 상품 " + opened.missing()
                    + "개는 빼고 넣었습니다.";
        }
        setStatus(message);
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
        chooser.setSelectedFile(new File("사담다_옷입혀보기.png"));
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
        generateButton.setEnabled(!working && hasOutfit);
        retryButton.setEnabled(!working && hasOutfit);
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
        slotButtons.values().forEach(button -> button.setEnabled(!working));
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    private static String shorten(String name) {
        return name.length() > 24 ? name.substring(0, 23) + "…" : name;
    }

    private static JButton bigButton(String text) {
        return compact(Theme.secondaryButton(text));
    }

    /** Theme 버튼은 좌우 여백이 넓어서, 버튼이 많은 이 화면에서는 장바구니 화면처럼 좁힌다. 높이는 48px 그대로 (NFR-02). */
    private static JButton compact(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        return button;
    }

    /** 옷 칸: 흰 바탕 둥근 카드 모양 버튼. 키보드 포커스가 오면 검정 테두리. */
    private static JButton slotButton() {
        JButton button = new JButton() {
            @Override
            public void updateUI() {
                setUI(new BasicButtonUI());
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 24, 24);
                g2.setColor(getModel().isPressed() ? Theme.SECONDARY : Theme.CARD);
                g2.fill(shape);
                g2.setColor(isFocusOwner() ? Theme.PRIMARY : Theme.LINE);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1f));
                g2.draw(shape);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setForeground(Theme.TEXT);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
