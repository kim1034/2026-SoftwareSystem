package com.sadamda.view.product;

import com.sadamda.common.AppException;
import com.sadamda.controller.AppContext;
import com.sadamda.model.review.Review;
import com.sadamda.view.MainFrame;
import com.sadamda.view.Toast;
import com.sadamda.view.cart.CartPanel;
import com.sadamda.view.member.LoginPanel;

import javax.swing.JComponent;
import java.util.List;

/**
 * 상품 카드의 찜 하트와 "♥ 찜 수 ★ 평점 (리뷰 수)" 줄 (UC-10 1a, FR-23). 상품 목록과 홈 카드가 함께 쓴다.
 * 비회원이 하트를 누르면 로그인 화면으로 보내고 (UC-06 4a 처럼 로그인 뒤 돌아온다), 찜하면 아래에 안내를 띄운다.
 */
public final class WishActions implements ProductCard.Actions {
    private final AppContext context;
    private final MainFrame frame;
    private final JComponent toastAnchor;

    public WishActions(AppContext context, MainFrame frame, JComponent toastAnchor) {
        this.context = context;
        this.frame = frame;
        this.toastAnchor = toastAnchor;
    }

    @Override
    public boolean isWished(Long productId) {
        return context.cart.isWished(productId);
    }

    @Override
    public int wishCount(Long productId) {
        return context.cart.wishCount(productId);
    }

    @Override
    public void toggleWish(Long productId) {
        if (context.member.currentMember() == null) {
            frame.navigate(LoginPanel.NAME);
            return;
        }
        try {
            boolean wished = context.cart.toggleWish(productId);
            Toast.show(toastAnchor, wished ? "찜했어요" : "찜을 해제했어요", wished ? "찜 목록 보기" : null,
                    () -> frame.navigate(CartPanel.NAME));
        } catch (AppException e) {
            Toast.show(toastAnchor, e.getMessage());
        }
    }

    @Override
    public List<Review> reviews(Long productId) {
        return context.product.reviews(productId);
    }
}
