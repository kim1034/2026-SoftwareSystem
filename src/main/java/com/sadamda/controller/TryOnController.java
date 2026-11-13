package com.sadamda.controller;

import com.sadamda.external.AiImageClient;
import com.sadamda.external.DressUpImageClient;
import com.sadamda.external.RemoteImages;
import com.sadamda.model.product.Category;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.model.tryon.TryOnImage;
import com.sadamda.model.tryon.TryOnService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** UC-09 AI 착용 이미지 생성. 담당: 김대원 */
public class TryOnController {
    /** AI 서비스로 보내는 상품 사진의 긴 변 크기(px). */
    private static final int GARMENT_IMAGE_SIDE = 768;

    private final Session session;
    private final AiImageClient aiImageClient;
    private final DressUpImageClient dressUpClient;
    private final TryOnService tryOnService;
    private final ProductService productService;
    /** 그림체·스펙별 캐릭터 기준 이미지. 옷을 바꿔도 같은 얼굴·자세를 유지하게 한다. */
    private final Map<String, byte[]> baseCharacters = new ConcurrentHashMap<>();

    public TryOnController(Session session, AiImageClient aiImageClient, DressUpImageClient dressUpClient,
                           TryOnService tryOnService, ProductService productService) {
        this.session = session;
        this.aiImageClient = aiImageClient;
        this.dressUpClient = dressUpClient;
        this.tryOnService = tryOnService;
        this.productService = productService;
    }

    /**
     * UC-09 3~8단계.
     * TODO: 파일 형식·용량 검증(3a), 타임아웃·재시도·장애 격리(7b, NFR-05·09), 업로드 사진 삭제(NFR-08)
     */
    public byte[] generate(byte[] personImage, byte[] backgroundImage, String clothingImageUrl)
            throws AiImageClient.AiImageException {
        return aiImageClient.synthesize(personImage, backgroundImage, clothingImageUrl);
    }

    /**
     * 옷 입혀보기: 고른 그림체·신체 스펙의 여성 캐릭터에 옷 한 벌을 입힌 이미지를 만든다.
     * 오래 걸리므로 화면 스레드가 아닌 곳에서 부른다. 실패하면 AiImageException (UC-09 7b → [다시 시도]).
     * AI 서비스에 어떤 문제가 생겨도 예외는 이 메서드 밖으로 AiImageException 으로만 나간다 (NFR-09 장애 격리).
     */
    public byte[] dressUp(CharacterStyle style, CharacterSpec spec, Outfit outfit)
            throws AiImageClient.AiImageException {
        Outfit snapshot = outfit.copy();
        String key = style.name() + "|" + spec;
        try {
            Map<DressUpSlot, byte[]> garmentImages = new EnumMap<>(DressUpSlot.class);
            for (DressUpSlot slot : DressUpSlot.values()) {
                Product product = snapshot.get(slot);
                if (product != null) {
                    byte[] jpeg = RemoteImages.toJpeg(RemoteImages.fetch(product.mainImageUrl()), GARMENT_IMAGE_SIDE);
                    if (jpeg != null) { // 사진을 못 구하면 상품 설명만으로 그린다
                        garmentImages.put(slot, jpeg);
                    }
                }
            }
            byte[] image = dressUpClient.dressUp(new DressUpImageClient.Request(
                    style, spec, snapshot, garmentImages, baseCharacters.get(key)));
            if (image == null || image.length == 0) {
                throw new AiImageClient.AiImageException("그림을 받지 못했습니다. 다시 시도해 주세요.");
            }
            baseCharacters.putIfAbsent(key, image);
            return image;
        } catch (AiImageClient.AiImageException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiImageClient.AiImageException("그림을 만드는 중에 문제가 생겼습니다. 다시 시도해 주세요.");
        }
    }

    /** 캐릭터를 새로 뽑고 싶을 때: 그 그림체·스펙의 기준 이미지를 지운다. */
    public void resetCharacter(CharacterStyle style, CharacterSpec spec) {
        baseCharacters.remove(style.name() + "|" + spec);
    }

    /** 옷 입혀보기 자리에 입힐 수 있는 상품 목록. */
    public List<Product> productsFor(DressUpSlot slot) {
        return productService.findAllByCategory(slot.category());
    }

    /** 옷 입혀보기 고르기 창의 검색. */
    public List<Product> productsFor(Category category) {
        return productService.findAllByCategory(category);
    }

    /** 화면에 보여줄 합성 서비스 이름. */
    public String dressUpServiceName() {
        return dressUpClient.name();
    }

    /** 합성 응답을 기다리는 최대 시간(초). */
    public int dressUpTimeoutSeconds() {
        return dressUpClient.timeoutSeconds();
    }

    /** 캐릭터 신체 스펙의 시작값. 로그인한 회원이면 등록한 체형 정보를 쓴다. */
    public CharacterSpec initialSpec() {
        return session.isLoggedIn() ? CharacterSpec.from(session.currentMember().getBodyProfile())
                : CharacterSpec.defaults();
    }

    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    /** UC-09 9단계 [보관함에 저장]. 로그인하지 않았으면 예외. productId 는 대표 상품이며 없으면 null. */
    public TryOnImage saveToLocker(Long productId, byte[] image) {
        return tryOnService.save(memberId(), productId, image);
    }

    /** 내 보관함 목록 (FR-16). */
    public List<TryOnImage> myLocker() {
        return tryOnService.findByMember(memberId());
    }

    /** 내 보관함에서 이미지 삭제 (NFR-08). */
    public void deleteFromLocker(Long imageId) {
        tryOnService.delete(memberId(), imageId);
    }

    private Long memberId() {
        if (!session.isLoggedIn() || session.currentMember().getId() == null) {
            throw new IllegalStateException("로그인한 뒤에 보관함을 쓸 수 있습니다");
        }
        return session.currentMember().getId();
    }
}
