package com.sadamda.controller;

import com.sadamda.common.AppException;
import com.sadamda.common.LoginRequiredException;
import com.sadamda.common.RetryableException;
import com.sadamda.external.AiImageClient;
import com.sadamda.external.AiImageClient.AiImageException;
import com.sadamda.model.member.Member;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.tryon.TryOnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TryOnControllerTest {
    private static final byte[] PERSON = {1, 2, 3};
    private static final byte[] TEMPLATE_BG = {7};
    private static final byte[] RESULT = {9, 9};
    private static final String CLOTH_URL = "images/blouse.png";

    private final Session session = new Session();
    private final AiImageClient ai = mock(AiImageClient.class);
    private final TryOnService tryOnService = mock(TryOnService.class);
    private final ProductService productService = mock(ProductService.class);
    private final TryOnController controller =
            new TryOnController(session, ai, tryOnService, productService, Duration.ofSeconds(5));

    @BeforeEach
    void setUp() {
        session.login(SessionTest.member(1L, Member.Role.MEMBER));
        Product product = mock(Product.class);
        when(product.getMainImageUrl()).thenReturn(CLOTH_URL);
        when(productService.findById(10L)).thenReturn(product);
        when(tryOnService.loadBackgroundTemplate("beach")).thenReturn(TEMPLATE_BG);
    }

    private static TryOnRequest withTemplate(String fileName, byte[] person) {
        return new TryOnRequest(10L, person, fileName, "beach", null, true);
    }

    private static TryOnRequest valid() {
        return withTemplate("me.jpg", PERSON);
    }

    @Test
    void success_passesPersonTemplateAndClothingUrl() throws Exception {
        when(ai.synthesize(PERSON, TEMPLATE_BG, CLOTH_URL)).thenReturn(RESULT);
        assertArrayEquals(RESULT, controller.generate(valid()));
    }

    @Test
    void uploadedBackground_usedInsteadOfTemplate() throws Exception {
        byte[] uploaded = {5, 5};
        when(ai.synthesize(PERSON, uploaded, CLOTH_URL)).thenReturn(RESULT);
        TryOnRequest req = new TryOnRequest(10L, PERSON, "me.png", null, uploaded, true);

        assertArrayEquals(RESULT, controller.generate(req));
        verify(tryOnService, never()).loadBackgroundTemplate(anyString());
    }

    @Test
    void notAgreed_throws_andAiNotCalled() {
        TryOnRequest req = new TryOnRequest(10L, PERSON, "me.jpg", "beach", null, false);
        AppException e = assertThrows(AppException.class, () -> controller.generate(req));
        assertEquals("사진 이용에 동의해야 AI 입어보기를 쓸 수 있습니다.", e.getMessage());
        verifyNoInteractions(ai);
    }

    @Test
    void emptyPerson_rejected() {
        AppException e = assertThrows(AppException.class,
                () -> controller.generate(withTemplate("me.jpg", new byte[0])));
        assertEquals("인물 사진을 올려 주세요.", e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"PHOTO.JPG,true", "a.png,true", "b.jpeg,true", "photo.jpg.exe,false", "photo,false", "gif.gif,false"})
    void fileNameCases(String name, boolean ok) throws Exception {
        when(ai.synthesize(any(), any(), anyString())).thenReturn(RESULT);
        if (ok) {
            assertArrayEquals(RESULT, controller.generate(withTemplate(name, PERSON)));
        } else {
            AppException e = assertThrows(AppException.class, () -> controller.generate(withTemplate(name, PERSON)));
            assertEquals("JPG 또는 PNG 사진만 올릴 수 있습니다.", e.getMessage());
        }
    }

    @Test
    void nullFileName_rejected() {
        AppException e = assertThrows(AppException.class, () -> controller.generate(withTemplate(null, PERSON)));
        assertEquals("JPG 또는 PNG 사진만 올릴 수 있습니다.", e.getMessage());
    }

    @Test
    void tooLarge_rejected() {
        byte[] big = new byte[(int) TryOnController.MAX_IMAGE_BYTES + 1];
        AppException e = assertThrows(AppException.class, () -> controller.generate(withTemplate("me.jpg", big)));
        assertEquals("사진 용량은 10MB 이하여야 합니다.", e.getMessage());
    }

    @Test
    void tooLargeBackground_rejected() {
        byte[] big = new byte[(int) TryOnController.MAX_IMAGE_BYTES + 1];
        TryOnRequest req = new TryOnRequest(10L, PERSON, "me.jpg", null, big, true);
        AppException e = assertThrows(AppException.class, () -> controller.generate(req));
        assertEquals("사진 용량은 10MB 이하여야 합니다.", e.getMessage());
    }

    @Test
    void noBackground_rejected() {
        TryOnRequest req = new TryOnRequest(10L, PERSON, "me.jpg", null, null, true);
        AppException e = assertThrows(AppException.class, () -> controller.generate(req));
        assertEquals("배경을 골라 주세요.", e.getMessage());
    }

    @Test
    void personNotFound_mapsTo7aMessage() throws Exception {
        when(ai.synthesize(PERSON, TEMPLATE_BG, CLOTH_URL))
                .thenThrow(new AiImageException(AiImageException.Reason.PERSON_NOT_FOUND, "no person"));
        AppException e = assertThrows(AppException.class, () -> controller.generate(valid()));
        assertEquals("사진에서 사람을 찾지 못했습니다. 전신이 나온 다른 사진을 올려 주세요.", e.getMessage());
    }

    @Test
    void aiFailedReason_retryable() throws Exception {
        when(ai.synthesize(PERSON, TEMPLATE_BG, CLOTH_URL))
                .thenThrow(new AiImageException(AiImageException.Reason.FAILED, "500"));
        RetryableException e = assertThrows(RetryableException.class, () -> controller.generate(valid()));
        assertEquals("이미지를 만들지 못했습니다. 다시 시도해 주세요.", e.getMessage());
    }

    @Test
    void clientThrowsRuntime_retryable() throws Exception {
        when(ai.synthesize(PERSON, TEMPLATE_BG, CLOTH_URL)).thenThrow(new IllegalStateException("boom"));
        assertThrows(RetryableException.class, () -> controller.generate(valid()));
    }

    @Test
    void nullResult_retryable() throws Exception {
        when(ai.synthesize(PERSON, TEMPLATE_BG, CLOTH_URL)).thenReturn(null);
        assertThrows(RetryableException.class, () -> controller.generate(valid()));
    }

    @Test
    void clientHangs_timesOutWithRetryableException() throws Exception {
        TryOnController fast = new TryOnController(session, ai, tryOnService, productService, Duration.ofMillis(100));
        when(ai.synthesize(PERSON, TEMPLATE_BG, CLOTH_URL)).thenAnswer(inv -> {
            Thread.sleep(5000);
            return RESULT;
        });
        assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> assertThrows(RetryableException.class, () -> fast.generate(valid())));
    }

    @Test
    void afterTimeout_nextCallWorks() throws Exception {
        TryOnController fast = new TryOnController(session, ai, tryOnService, productService, Duration.ofMillis(100));
        when(ai.synthesize(PERSON, TEMPLATE_BG, CLOTH_URL))
                .thenAnswer(inv -> {
                    Thread.sleep(5000);
                    return RESULT;
                })
                .thenReturn(RESULT);
        assertThrows(RetryableException.class, () -> fast.generate(valid()));
        assertArrayEquals(RESULT, fast.generate(valid()));
    }

    @Test
    void guest_throwsLoginRequired() {
        session.logout();
        assertThrows(LoginRequiredException.class, () -> controller.generate(valid()));
        verifyNoInteractions(ai);
    }
}
