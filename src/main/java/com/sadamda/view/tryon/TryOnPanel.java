package com.sadamda.view.tryon;

import com.sadamda.common.LoginRequiredException;
import com.sadamda.controller.AppContext;
import com.sadamda.external.AiImageClient;
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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
    private final JLabel serviceLabel = new JLabel(" ", SwingConstants.RIGHT);
    private final JButton specButton = bigButton("");
    private final JLabel status = new JLabel(" ");
    private final JLabel total = new JLabel(" ", SwingConstants.RIGHT);
    private final JProgressBar progress = new JProgressBar();
    private final JButton generateButton = bigButton("입혀보기");
    private final JButton retryButton = bigButton("다시 시도");
    private final JButton undressButton = bigButton("모두 벗기");
    private final JButton lockerButton = bigButton("보관함에 저장");
    private final JButton openLockerButton = bigButton("내 보관함");
    private final JButton downloadButton = bigButton("내 컴퓨터에 저장");
    private final JButton cartButton = bigButton("장바구니 담기");
    private final JButton settingsButton = bigButton("AI 연결 설정");

    private CharacterStyle style = CharacterStyle.REAL;
    private CharacterSpec spec;
    private byte[] resultImage;
    private boolean working;

    public TryOnPanel(AppContext context, MainFrame frame) {
        super(context, frame, "AI 입어보기", "UC-09 AI 착용 이미지 생성");
        removeAll(); // 공통 부모가 넣어 둔 "TODO" 안내를 걷어내고 실제 화면을 넣는다
        spec = context.tryOn.initialSpec();

        add(header(), BorderLayout.NORTH);
        add(characterColumn(), BorderLayout.CENTER);
        add(slotColumn(), BorderLayout.EAST);
        add(actions(), BorderLayout.SOUTH);

        showService();
        showSpec();
        showBaseCharacter();
        refreshSlots();
        setStatus(context.tryOn.dressUpConnected()
                ? "옷을 고르고 [입혀보기]를 눌러 주세요."
                : "그림 만드는 곳(AI)에 연결되지 않았습니다. [AI 연결 설정]에서 이용 열쇠를 넣어 주세요.");
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("<html><b>옷 입혀보기</b></html>"), BorderLayout.WEST);
        header.add(serviceLabel, BorderLayout.EAST);
        return header;
    }

    /** 왼쪽: 그림체 고르기 + 캐릭터 + 신체 스펙. */
    private JPanel characterColumn() {
        JPanel styles = new JPanel(new GridLayout(1, 0, 8, 0));
        ButtonGroup group = new ButtonGroup();
        for (CharacterStyle option : CharacterStyle.values()) {
            JToggleButton button = new JToggleButton(option.label(), option == style);
            button.setPreferredSize(new Dimension(120, 52));
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
        column.add(styles, BorderLayout.NORTH);
        column.add(characterView, BorderLayout.CENTER);
        column.add(specButton, BorderLayout.SOUTH);
        return column;
    }

    /** 오른쪽: 모자·상의·하의·주얼리·신발 칸. */
    private JPanel slotColumn() {
        JPanel column = new JPanel(new GridLayout(0, 1, 0, 12));
        column.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        for (DressUpSlot slot : DressUpSlot.values()) {
            JButton button = new JButton();
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setIconTextGap(12);
            button.setPreferredSize(new Dimension(380, 104));
            button.addActionListener(e -> pick(slot));
            slotButtons.put(slot, button);
            column.add(button);
        }
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
            refreshSlots();
            showBaseCharacter();
            setStatus("옷을 모두 벗겼습니다.");
        });
        lockerButton.addActionListener(e -> saveToLocker());
        openLockerButton.addActionListener(e -> openLocker());
        downloadButton.addActionListener(e -> download());
        cartButton.addActionListener(e -> addToCart());
        settingsButton.addActionListener(e -> editSettings());

        JPanel statusRow = new JPanel(new BorderLayout(12, 0));
        statusRow.add(status, BorderLayout.CENTER);
        statusRow.add(progress, BorderLayout.WEST);
        statusRow.add(total, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(generateButton);
        buttons.add(retryButton);
        buttons.add(undressButton);
        buttons.add(lockerButton);
        buttons.add(openLockerButton);
        buttons.add(downloadButton);
        buttons.add(cartButton);
        buttons.add(settingsButton);

        JPanel actions = new JPanel(new BorderLayout(0, 8));
        actions.add(statusRow, BorderLayout.NORTH);
        actions.add(buttons, BorderLayout.CENTER);
        return actions;
    }

    /** 옷을 입히기 전 모습: 그림체의 기본 캐릭터에 신체 스펙을 반영해 보여준다. 전에 만든 합성 결과는 치운다. */
    private void showBaseCharacter() {
        resultImage = null;
        retryButton.setVisible(false);
        try {
            characterView.setImage(context.tryOn.characterImage(style, spec));
        } catch (RuntimeException e) {
            characterView.setImage(null);
            characterView.setMessage("캐릭터 그림을 불러오지 못했습니다");
        }
        updateButtons();
    }

    private void showService() {
        serviceLabel.setText("그림 만드는 곳: " + context.tryOn.dressUpServiceName());
    }

    /** 자리 칸을 누르면 그 카테고리 상품을 고르는 창을 연다. */
    private void pick(DressUpSlot slot) {
        if (working) {
            return;
        }
        ProductPickerDialog dialog = new ProductPickerDialog(SwingUtilities.getWindowAncestor(this), slot,
                context.tryOn.productsFor(slot), outfit.get(slot) != null);
        dialog.setVisible(true);
        if (dialog.takeOff()) {
            outfit.remove(slot);
        } else if (dialog.chosen() != null) {
            outfit.put(slot, dialog.chosen());
        } else {
            return;
        }
        refreshSlots();
        setStatus(outfit.isEmpty() ? "입힐 옷을 골라 주세요."
                : "옷을 바꿨습니다. [입혀보기]를 누르면 캐릭터가 입습니다.");
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
        JComboBox<String> bodyType = new JComboBox<>(CharacterSpec.BODY_TYPES.toArray(new String[0]));
        bodyType.setSelectedItem(spec.bodyType());
        JComboBox<String> ageGroup = new JComboBox<>(CharacterSpec.AGE_GROUPS.toArray(new String[0]));
        ageGroup.setSelectedItem(spec.ageGroup());

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.add(new JLabel("키 (cm)"));
        form.add(height);
        form.add(new JLabel("몸무게 (kg)"));
        form.add(weight);
        form.add(new JLabel("체형"));
        form.add(bodyType);
        form.add(new JLabel("연령대"));
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
        DressUpSettings saved = context.tryOn.dressUpSettings();
        JPasswordField key = new JPasswordField(saved.apiKey() == null ? "" : saved.apiKey(), 28);
        JTextField baseUrl = new JTextField(saved.baseUrl() == null ? "" : saved.baseUrl(), 28);
        JTextField model = new JTextField(saved.model() == null ? "" : saved.model(), 28);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("이용 열쇠 (API 키)"));
        form.add(key);
        form.add(new JLabel("서비스 주소 (중계 서비스를 쓸 때만. 비우면 OpenAI 공식 주소)"));
        form.add(baseUrl);
        form.add(new JLabel("모델 이름 (비우면 기본 모델)"));
        form.add(model);
        form.add(new JLabel(context.tryOn.dressUpKeyFromEnvironment()
                ? "지금은 환경 변수에 넣은 열쇠가 먼저 쓰입니다."
                : "이 PC 에만 저장되고, 다른 사람에게 보내는 파일에는 들어가지 않습니다."));

        Object[] choices = {"저장", "취소"};
        int answer = JOptionPane.showOptionDialog(this, form, "AI 연결 설정",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
        if (answer == 0) {
            context.tryOn.configureDressUp(new String(key.getPassword()), baseUrl.getText(), model.getText());
            showService();
            setStatus(context.tryOn.dressUpConnected()
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
        if (!context.tryOn.dressUpConnected()) {
            setStatus("그림 만드는 곳(AI)에 연결되지 않았습니다. 이용 열쇠를 넣어 주세요.");
            editSettings();
            if (!context.tryOn.dressUpConnected()) {
                return;
            }
        }
        working = true;
        retryButton.setVisible(false);
        progress.setVisible(true);
        setStatus("캐릭터에게 옷을 입히는 중입니다. 잠시만 기다려 주세요 (최대 "
                + context.tryOn.dressUpTimeoutSeconds() + "초)");
        updateButtons();

        CharacterStyle requestedStyle = style;
        CharacterSpec requestedSpec = spec;
        Outfit requestedOutfit = outfit.copy();
        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() throws Exception {
                return context.tryOn.dressUp(requestedStyle, requestedSpec, requestedOutfit);
            }

            @Override
            protected void done() {
                working = false;
                progress.setVisible(false);
                try {
                    byte[] image = get();
                    BufferedImage decoded = RemoteImages.decode(image);
                    if (decoded == null) {
                        throw new AiImageClient.AiImageException("받은 그림을 열 수 없습니다. 다시 시도해 주세요.");
                    }
                    resultImage = image;
                    characterView.setImage(decoded);
                    setStatus("다 입혔습니다. 옷을 바꿔 가며 계속 입혀 보세요.");
                } catch (ExecutionException e) {
                    fail(e.getCause() instanceof AiImageClient.AiImageException
                            ? e.getCause().getMessage() : "그림을 만드는 중에 문제가 생겼습니다.");
                } catch (AiImageClient.AiImageException e) {
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

    /** UC-09 9단계 [보관함에 저장] */
    private void saveToLocker() {
        if (!context.tryOn.isLoggedIn()) {
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
        if (!context.tryOn.isLoggedIn()) {
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
        } catch (LoginRequiredException e) {
            JOptionPane.showMessageDialog(this, "장바구니는 로그인한 뒤에 쓸 수 있습니다.");
        } catch (UnsupportedOperationException e) {
            JOptionPane.showMessageDialog(this, "장바구니는 아직 준비 중입니다.");
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
        slotButtons.values().forEach(button -> button.setEnabled(!working));
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    private static String shorten(String name) {
        return name.length() > 24 ? name.substring(0, 23) + "…" : name;
    }

    private static JButton bigButton(String text) {
        JButton button = new JButton(text);
        button.setMinimumSize(new Dimension(48, 48)); // NFR-02
        button.setMargin(new Insets(10, 14, 10, 14));
        return button;
    }
}
