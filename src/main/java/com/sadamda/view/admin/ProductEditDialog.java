package com.sadamda.view.admin;

import com.sadamda.controller.AppContext;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductOption;
import com.sadamda.view.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * UC-16 4~5단계 상품 등록·수정 창. 옵션은 한 줄에 "사이즈 / 색상 / 재고".
 * 저장은 AdminController.saveProduct 가 검사하고, 빠진 항목이 있으면 그 문장을 창 안에 보여 준다 (5a).
 */
final class ProductEditDialog extends JDialog {
    private final AppContext context;
    private final Product original;
    private final JTextField name = Theme.textField("예: 캐시미어 니트 가디건");
    private final JComboBox<String> category;
    private final JTextField price = Theme.textField("숫자만, 예: 59000");
    private final JTextField material = Theme.textField("예: 면 100%");
    private final JTextArea images = Theme.textArea("사진 주소를 한 줄에 하나씩", 3);
    private final JTextArea options = Theme.textArea("한 줄에 하나씩: M (66) / 네이비 / 10", 4);
    private final JTextArea description = Theme.textArea("코디·트렌드 포인트 (선택)", 2);
    private final JLabel error = Theme.errorLabel();
    private boolean saved;

    ProductEditDialog(Window owner, AppContext context, Product product) {
        super(owner, product == null ? "새 상품 등록" : "상품 수정", Dialog.ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.original = product;
        category = Theme.comboBox(Arrays.stream(Category.values()).map(Category::label).toArray(String[]::new));

        JPanel form = new JPanel(new java.awt.GridBagLayout());
        form.setBackground(Theme.CARD);
        form.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        Theme.addToCard(form, AdminUi.heading(getTitle()), 0);
        Theme.addToCard(form, Theme.field("상품명", null, name), 16);
        JPanel pair = new JPanel(new GridLayout(1, 2, 16, 0));
        pair.setOpaque(false);
        pair.add(Theme.field("카테고리", null, category));
        pair.add(Theme.field("가격(원)", null, price));
        Theme.addToCard(form, pair, 16);
        Theme.addToCard(form, Theme.field("소재", null, material), 16);
        Theme.addToCard(form, Theme.field("사진 주소", null, scroll(images)), 16);
        Theme.addToCard(form, Theme.field("옵션·재고 (사이즈 / 색상 / 재고)", null, scroll(options)), 16);
        Theme.addToCard(form, Theme.field("설명", null, scroll(description)), 16);
        Theme.addToCard(form, error, 12);

        JButton cancel = Theme.secondaryButton("닫기");
        cancel.addActionListener(e -> dispose());
        JButton save = Theme.primaryButton("저장");
        save.addActionListener(e -> save());
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        buttons.setBorder(BorderFactory.createEmptyBorder(12, 32, 20, 32));

        if (product != null) {
            name.setText(product.getName());
            if (product.getCategory() != null) {
                category.setSelectedItem(product.getCategory().label());
            }
            price.setText(String.valueOf(product.getPrice()));
            material.setText(product.getMaterial() == null ? "" : product.getMaterial());
            images.setText(String.join("\n", product.getImageUrls()));
            List<String> lines = new ArrayList<>();
            for (ProductOption o : product.getOptions()) {
                lines.add(o.size() + " / " + o.color() + " / " + o.stock());
            }
            options.setText(String.join("\n", lines));
            description.setText(product.getDescription() == null ? "" : product.getDescription());
        }

        setContentPane(AdminUi.dialogContent(form, buttons));
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        Dimension screen = getToolkit().getScreenSize();
        setSize(Math.min(Math.max(getWidth() + 24, 760), screen.width - 80), Math.min(getHeight(), screen.height - 120));
        setLocationRelativeTo(owner);
    }

    boolean saved() {
        return saved;
    }

    private static JScrollPane scroll(JTextArea area) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        return scroll;
    }

    private void save() {
        error.setText("");
        Product product = original == null ? new Product() : copyOf(original);
        product.setName(name.getText().trim());
        product.setCategory(Category.fromLabel((String) category.getSelectedItem()));
        String priceText = price.getText().replace(",", "").replace("원", "").trim();
        if (priceText.isEmpty()) {
            product.setPrice(0); // Model 이 "가격" 누락으로 알려 준다
        } else {
            try {
                product.setPrice(Integer.parseInt(priceText));
            } catch (NumberFormatException e) {
                error.setText("가격은 숫자만 입력해 주세요.");
                return;
            }
        }
        product.setMaterial(material.getText().trim());
        product.setDescription(description.getText().trim());
        product.setImageUrls(lines(images.getText()));
        List<ProductOption> parsed = new ArrayList<>();
        for (String line : lines(options.getText())) {
            String[] parts = line.split("/");
            if (parts.length != 3) {
                error.setText("옵션은 '사이즈 / 색상 / 재고' 모양으로 적어 주세요: " + line);
                return;
            }
            int stock;
            try {
                stock = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException e) {
                error.setText("재고는 숫자로 적어 주세요: " + line);
                return;
            }
            String size = parts[0].trim();
            String color = parts[1].trim();
            parsed.add(new ProductOption(existingOptionId(size, color), size, color, stock));
        }
        product.setOptions(parsed);
        Product result = AdminUi.call(this, getTitle(), () -> context.admin.saveProduct(product));
        if (result != null) {
            saved = true;
            dispose();
        }
    }

    /** 수정할 때 같은 사이즈·색상이면 원래 옵션 번호를 지켜 장바구니·주문과 이어지게 한다. */
    private Long existingOptionId(String size, String color) {
        if (original == null) {
            return null;
        }
        return original.getOptions().stream().filter(o -> o.size().equals(size) && o.color().equals(color))
                .map(ProductOption::id).findFirst().orElse(null);
    }

    private static List<String> lines(String text) {
        return Arrays.stream(text.split("\\R")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** 원본을 바로 고치지 않도록 복사한다 (저장이 실패해도 원본은 그대로). */
    static Product copyOf(Product p) {
        Product copy = new Product();
        copy.setId(p.getId());
        copy.setCode(p.getCode());
        copy.setName(p.getName());
        copy.setBrand(p.getBrand());
        copy.setCountry(p.getCountry());
        copy.setCategory(p.getCategory());
        copy.setSubCategory(p.getSubCategory());
        copy.setPrice(p.getPrice());
        copy.setMaterial(p.getMaterial());
        copy.setDescription(p.getDescription());
        copy.setProductUrl(p.getProductUrl());
        copy.setOptions(p.getOptions());
        copy.setImageUrls(p.getImageUrls());
        copy.setAgeGroupTags(p.getAgeGroupTags());
        copy.setBodyTypeTags(p.getBodyTypeTags());
        copy.setTrending(p.isTrending());
        copy.setSalesCount(p.getSalesCount());
        copy.setStatus(p.getStatus());
        return copy;
    }
}
