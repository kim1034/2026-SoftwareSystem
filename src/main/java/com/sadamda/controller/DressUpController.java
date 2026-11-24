package com.sadamda.controller;

import com.sadamda.external.DressUpClients;
import com.sadamda.external.DressUpException;
import com.sadamda.external.DressUpImageClient;
import com.sadamda.external.DressUpSettings;
import com.sadamda.external.RemoteImages;
import com.sadamda.model.product.Product;
import com.sadamda.model.product.ProductService;
import com.sadamda.model.tryon.Background;
import com.sadamda.model.tryon.BackgroundCatalog;
import com.sadamda.model.tryon.CharacterFigure;
import com.sadamda.model.tryon.CharacterSpec;
import com.sadamda.model.tryon.CharacterStyle;
import com.sadamda.model.tryon.DressUpSlot;
import com.sadamda.model.tryon.Outfit;
import com.sadamda.model.tryon.UserPhoto;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 옷 입혀보기 (UC-09). 담당: 정우진
 * - 캐릭터: 신체 스펙을 반영한 캐릭터에 고른 상품을 입혀 본다.
 * - 내 사진: 사용자가 올린 전신 사진에 고른 상품을 입히고, 고른 배경(계절·나라별 사진)으로 바꾼다.
 * 두 가지 모두 같은 AI 합성기(DressUpImageClient)를 쓴다. TryOnController(김대원)의 사진 합성(generate)은
 * 가짜 AI 에 상품 하나만 받는 구조라 이 화면은 쓰지 않고, 보관함 저장은 context.tryOn.saveToLocker 를 그대로 쓴다.
 */
public class DressUpController {
    /** AI 서비스로 보내는 상품 사진의 긴 변 크기(px). */
    private static final int GARMENT_IMAGE_SIDE = 768;
    /** AI 서비스로 보내는 배경 사진의 긴 변 크기(px). */
    private static final int BACKGROUND_IMAGE_SIDE = 1280;

    private final Session session;
    private final ProductService productService;
    private final BackgroundCatalog backgrounds;
    private volatile DressUpImageClient dressUpClient;

    public DressUpController(Session session, DressUpImageClient dressUpClient, ProductService productService) {
        this(session, dressUpClient, productService, new BackgroundCatalog());
    }

    public DressUpController(Session session, DressUpImageClient dressUpClient, ProductService productService,
                             BackgroundCatalog backgrounds) {
        this.session = session;
        this.dressUpClient = dressUpClient;
        this.productService = productService;
        this.backgrounds = backgrounds;
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
            throws DressUpException {
        Outfit snapshot = outfit.copy();
        DressUpImageClient client = dressUpClient;
        try {
            if (snapshot.isEmpty()) {
                throw new DressUpException("입힐 옷을 하나 이상 골라 주세요.");
            }
            if (!client.connected()) {
                return client.dressUp(null); // 연결 안내 예외를 그대로 내보낸다
            }
            byte[] base = RemoteImages.toPng(CharacterFigure.shaped(style, spec));
            byte[] image = client.dressUp(new DressUpImageClient.Request(style, spec, snapshot,
                    garmentImages(snapshot), base));
            if (image == null || image.length == 0) {
                throw new DressUpException("그림을 받지 못했습니다. 다시 시도해 주세요.");
            }
            return fitToSpec(image, spec, snapshot.get(DressUpSlot.HAT) != null);
        } catch (DressUpException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DressUpException("그림을 만드는 중에 문제가 생겼습니다. 다시 시도해 주세요.");
        }
    }

    /**
     * 내 사진으로 입어보기: 올린 사진에 고른 옷을 입히고, 배경을 고르면 그 장소에서 찍은 사진처럼 바꾼다.
     * 옷과 배경 중 하나는 골라야 한다. 오래 걸리므로 화면 스레드가 아닌 곳에서 부른다.
     * 사진은 메모리에만 있고 파일로 남기지 않는다 (NFR-08). 어떤 문제가 생겨도 DressUpException 으로만 나간다.
     *
     * @param background 고른 배경. null 이면 사진의 원래 배경 그대로.
     */
    public byte[] dressUpPhoto(UserPhoto photo, Background background, Outfit outfit) throws DressUpException {
        Outfit snapshot = outfit.copy();
        DressUpImageClient client = dressUpClient;
        try {
            if (photo == null) {
                throw new DressUpException("내 사진을 먼저 올려 주세요.");
            }
            if (snapshot.isEmpty() && background == null) {
                throw new DressUpException("입힐 옷이나 배경을 하나 이상 골라 주세요.");
            }
            if (!client.connected()) {
                return client.dressUp(null); // 연결 안내 예외를 그대로 내보낸다
            }
            byte[] backgroundImage = null;
            if (background != null) {
                backgroundImage = RemoteImages.toJpeg(RemoteImages.fetch(background.imageUrl()), BACKGROUND_IMAGE_SIDE);
                if (backgroundImage == null) {
                    throw new DressUpException("배경 사진을 불러오지 못했습니다. 인터넷 연결을 확인하거나 다른 배경을 골라 주세요.");
                }
            }
            byte[] image = client.dressUp(DressUpImageClient.Request.photo(snapshot, garmentImages(snapshot),
                    photo.jpegForAi(), backgroundImage));
            if (image == null || image.length == 0) {
                throw new DressUpException("그림을 받지 못했습니다. 다시 시도해 주세요.");
            }
            return image;
        } catch (DressUpException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DressUpException("그림을 만드는 중에 문제가 생겼습니다. 다시 시도해 주세요.");
        }
    }

    /**
     * 올린 사진을 확인한다 (UC-09 2a 동의, 3a 형식·용량). 문제가 있으면 화면에 보여줄 문장으로 AppException.
     * 확인한 사진은 메모리에만 두고 파일로 저장하지 않는다 (NFR-08).
     */
    public UserPhoto checkPhoto(String fileName, byte[] bytes, boolean photoUseAgreed) {
        return UserPhoto.of(fileName, bytes, photoUseAgreed);
    }

    /** 내 사진으로 입어보기의 배경 목록 (계절 순). */
    public List<Background> backgrounds() {
        return backgrounds.all();
    }

    /** 고른 옷의 상품 사진(JPEG)을 내려받는다. 사진을 못 구한 자리는 빼고, 그 상품은 설명만으로 그린다. */
    private static Map<DressUpSlot, byte[]> garmentImages(Outfit outfit) {
        Map<DressUpSlot, byte[]> garmentImages = new EnumMap<>(DressUpSlot.class);
        for (DressUpSlot slot : DressUpSlot.values()) {
            Product product = outfit.get(slot);
            if (product != null) {
                byte[] jpeg = RemoteImages.toJpeg(RemoteImages.fetch(product.mainImageUrl()), GARMENT_IMAGE_SIDE);
                if (jpeg != null) {
                    garmentImages.put(slot, jpeg);
                }
            }
        }
        return garmentImages;
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

    /** 옷 입혀보기 자리에 입힐 수 있는 판매 중 상품 (인기순). 주얼리는 그 종류(귀걸이·목걸이 등)만. */
    public List<Product> productsFor(DressUpSlot slot) {
        return productService.findAllByCategory(slot.category()).stream().filter(slot::accepts).toList();
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

    /** 환경 변수에 키가 있는지. 화면에 키를 저장하지 않았을 때만 그 키를 쓴다. */
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
}
