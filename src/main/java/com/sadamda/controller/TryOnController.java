package com.sadamda.controller;

import com.sadamda.external.AiImageClient;
import com.sadamda.external.DressUpClients;
import com.sadamda.external.DressUpImageClient;
import com.sadamda.external.DressUpSettings;
import com.sadamda.external.RemoteImages;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.tryon.CharacterFigure;
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.model.tryon.TryOnImage;
import com.sadamda.model.tryon.TryOnService;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** UC-09 AI 착용 이미지 생성. 담당: 김대원 */
public class TryOnController {
    /** AI 서비스로 보내는 상품 사진의 긴 변 크기(px). */
    private static final int GARMENT_IMAGE_SIDE = 768;

    private final Session session;
    private final AiImageClient aiImageClient;
    private final TryOnService tryOnService;
    private final ProductService productService;
    private volatile DressUpImageClient dressUpClient;

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

    /** 옷을 입히기 전의 캐릭터: 그림체의 기본 캐릭터에 신체 스펙(키·몸무게·체형)을 반영한 이미지. */
    public BufferedImage characterImage(CharacterStyle style, CharacterSpec spec) {
        return CharacterFigure.shaped(style, spec);
    }

    /**
     * 옷 입혀보기: 신체 스펙이 반영된 기본 캐릭터에 고른 옷을 입힌 이미지를 AI 로 만든다.
     * 오래 걸리므로 화면 스레드가 아닌 곳에서 부른다. 실패하면 AiImageException (UC-09 7b → [다시 시도]).
     * AI 서비스에 어떤 문제가 생겨도 예외는 이 메서드 밖으로 AiImageException 으로만 나간다 (NFR-09 장애 격리).
     */
    public byte[] dressUp(CharacterStyle style, CharacterSpec spec, Outfit outfit)
            throws AiImageClient.AiImageException {
        Outfit snapshot = outfit.copy();
        DressUpImageClient client = dressUpClient;
        try {
            if (snapshot.isEmpty()) {
                throw new AiImageClient.AiImageException("입힐 옷을 하나 이상 골라 주세요.");
            }
            if (!client.connected()) {
                return client.dressUp(null); // 연결 안내 예외를 그대로 내보낸다
            }
            byte[] base = RemoteImages.toPng(CharacterFigure.shaped(style, spec));
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
            byte[] image = client.dressUp(new DressUpImageClient.Request(style, spec, snapshot, garmentImages, base));
            if (image == null || image.length == 0) {
                throw new AiImageClient.AiImageException("그림을 받지 못했습니다. 다시 시도해 주세요.");
            }
            return fitToSpec(image, spec, snapshot.get(DressUpSlot.HAT) != null);
        } catch (AiImageClient.AiImageException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiImageClient.AiImageException("그림을 만드는 중에 문제가 생겼습니다. 다시 시도해 주세요.");
        }
    }

    /** AI 결과에서 캐릭터의 화면 속 키를 스펙대로 바로잡는다. 그림을 열 수 없으면 받은 그대로 돌려준다. */
    private static byte[] fitToSpec(byte[] image, CharacterSpec spec, boolean wearingHat) {
        BufferedImage decoded = RemoteImages.decode(image);
        if (decoded == null) {
            return image;
        }
        BufferedImage fitted = CharacterFigure.fitHeight(decoded, spec, wearingHat);
        return fitted == decoded ? image : RemoteImages.toPng(fitted);
    }

    /** 옷 입혀보기 자리에 입힐 수 있는 상품 목록. */
    public List<Product> productsFor(DressUpSlot slot) {
        return productService.findAllByCategory(slot.category());
    }

    /** 화면에 보여줄 합성 서비스 이름. */
    public String dressUpServiceName() {
        return dressUpClient.name();
    }

    /** AI 합성 서비스에 연결되어 있는지. 아니면 화면이 [AI 연결 설정]을 안내한다. */
    public boolean dressUpConnected() {
        return dressUpClient.connected();
    }

    /** 합성 응답을 기다리는 최대 시간(초). */
    public int dressUpTimeoutSeconds() {
        return dressUpClient.timeoutSeconds();
    }

    /** [AI 연결 설정]에 보여줄 현재 저장값. */
    public DressUpSettings dressUpSettings() {
        return DressUpSettings.load();
    }

    /** 환경 변수로 키가 주어져 화면 설정보다 우선하는 상태인지. */
    public boolean dressUpKeyFromEnvironment() {
        return DressUpClients.keyFromEnvironment();
    }

    /** [AI 연결 설정] 저장: 이 PC 의 사용자 설정에만 저장하고 합성기를 다시 고른다. */
    public void configureDressUp(String apiKey, String baseUrl, String model) {
        DressUpSettings settings = new DressUpSettings(apiKey, baseUrl, model);
        settings.save();
        dressUpClient = DressUpClients.create(settings);
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
