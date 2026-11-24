package com.sadamda.model.tryon;

import com.sadamda.common.AppException;
import com.sadamda.common.OwnerKeys;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 내 코디북 (UC-19): 옷 입혀보기에서 만든 코디(그림체·신체 스펙·입힌 상품·그림)를 이름을 붙여 모아 두고
 * 다시 열어 고칠 수 있게 한다. 담당: 정우진
 *
 * 회원·주문은 아직 메모리에만 있지만, 코디북은 다시 켜도 남아야 의미가 있어서 보관함(TryOnService)처럼 이 PC 에 둔다.
 * - 폴더: 사용자 홈/.sadamda/codibook/{회원 열쇠}/  (회원 열쇠 = 이메일을 SHA-256 으로 바꾼 앞 16자리)
 * - 코디 1개 = {id}.properties (이름·그림체·스펙·상품 id) + {id}.png (그림, 있을 때만)
 * 회원 번호는 프로그램을 켤 때마다 다시 매겨지므로 폴더 이름에는 이메일을 쓴다. 이메일을 그대로 쓰지 않는 것은
 * 파일 이름에 쓸 수 없는 글자를 피하고, 폴더만 보고 누구의 것인지 알 수 없게 하려는 것이다.
 */
public class CodiBookService {
    /** 코디 이름 최대 글자 수. 목록에서 한 줄로 보이게 한다. */
    public static final int MAX_TITLE = 30;
    /** 회원 한 명이 모아 둘 수 있는 코디 수. */
    public static final int MAX_CODIS = 100;

    private static final String SLOT_KEY = "slot.";

    private final Path baseDir;
    private final Map<String, List<SavedCodi>> cache = new HashMap<>();

    public CodiBookService() {
        this(Path.of(System.getProperty("user.home"), ".sadamda", "codibook"));
    }

    public CodiBookService(Path baseDir) {
        this.baseDir = baseDir;
    }

    /** UC-19 1~4단계 새 코디로 저장. image 는 [입혀보기] 전이면 null. */
    public synchronized SavedCodi save(String owner, String title, CharacterStyle style, CharacterSpec spec,
                                       Map<DressUpSlot, Long> productIds, byte[] image) {
        String cleanTitle = validate(owner, title, productIds);
        List<SavedCodi> codis = load(owner);
        if (codis.size() >= MAX_CODIS) {
            throw new AppException("코디북에는 " + MAX_CODIS + "개까지 모을 수 있어요. 안 쓰는 코디를 지운 뒤 저장해 주세요.");
        }
        long id = codis.stream().mapToLong(SavedCodi::id).max().orElse(0) + 1;
        SavedCodi saved = write(owner, id, cleanTitle, style, spec, productIds, image);
        codis.add(saved);
        return saved;
    }

    /** UC-19 4a 다시 연 코디를 고친 내용으로 덮어쓴다. 이름은 그대로 둔다. image 가 null 이면 예전 그림을 지운다. */
    public synchronized SavedCodi overwrite(String owner, long id, CharacterStyle style, CharacterSpec spec,
                                            Map<DressUpSlot, Long> productIds, byte[] image) {
        SavedCodi old = find(owner, id);
        validate(owner, old.title(), productIds);
        SavedCodi saved = write(owner, id, old.title(), style, spec, productIds, image);
        replace(owner, saved);
        return saved;
    }

    /** UC-19 5a 이름 바꾸기. */
    public synchronized SavedCodi rename(String owner, long id, String title) {
        SavedCodi old = find(owner, id);
        String cleanTitle = validate(owner, title, old.productIds());
        SavedCodi renamed = new SavedCodi(id, cleanTitle, old.style(), old.spec(), old.productIds(),
                old.imagePath(), old.savedAt());
        writeProperties(owner, renamed);
        replace(owner, renamed);
        return renamed;
    }

    /** UC-19 5b 지우기. 그림 파일도 함께 지우며 되돌릴 수 없다. */
    public synchronized void delete(String owner, long id) {
        SavedCodi target = find(owner, id);
        Path dir = ownerDir(owner);
        try {
            Files.deleteIfExists(dir.resolve(id + ".properties"));
            Files.deleteIfExists(dir.resolve(id + ".png"));
        } catch (IOException e) {
            throw new AppException("코디를 지우지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
        load(owner).remove(target);
    }

    /** UC-19 1단계 내 코디북: 최근에 저장한 것부터. */
    public synchronized List<SavedCodi> findAll(String owner) {
        requireOwner(owner);
        return load(owner).stream()
                .sorted(Comparator.comparing(SavedCodi::savedAt).thenComparing(SavedCodi::id).reversed())
                .collect(Collectors.toList());
    }

    public synchronized SavedCodi find(String owner, long id) {
        requireOwner(owner);
        return load(owner).stream()
                .filter(codi -> codi.id() == id)
                .findFirst()
                .orElseThrow(() -> new AppException("코디를 찾을 수 없어요. 이미 지운 코디일 수 있어요."));
    }

    /** 저장한 그림. 없거나 열 수 없으면 null. */
    public byte[] readImage(SavedCodi codi) {
        if (codi == null || codi.imagePath() == null) {
            return null;
        }
        try {
            return Files.readAllBytes(Path.of(codi.imagePath()));
        } catch (IOException e) {
            return null;
        }
    }

    /** 회원 이메일 → 폴더 이름. 대소문자·앞뒤 공백이 달라도 같은 회원으로 본다. */
    public static String ownerKey(String email) {
        return OwnerKeys.of(email);
    }

    private String validate(String owner, String title, Map<DressUpSlot, Long> productIds) {
        requireOwner(owner);
        String clean = title == null ? "" : title.strip();
        if (clean.isEmpty()) {
            throw new AppException("코디 이름을 입력해 주세요.");
        }
        if (clean.length() > MAX_TITLE) {
            throw new AppException("코디 이름은 " + MAX_TITLE + "자까지 쓸 수 있어요.");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new AppException("입힌 옷이 없어 저장할 수 없어요. 옷을 하나 이상 골라 주세요.");
        }
        return clean;
    }

    private static void requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new AppException("로그인한 뒤에 코디북을 쓸 수 있어요.");
        }
    }

    private SavedCodi write(String owner, long id, String title, CharacterStyle style, CharacterSpec spec,
                            Map<DressUpSlot, Long> productIds, byte[] image) {
        Path dir = ownerDir(owner);
        Path imageFile = dir.resolve(id + ".png");
        try {
            Files.createDirectories(dir);
            if (image != null && image.length > 0) {
                Path temp = Files.createTempFile(dir, "codi_", ".tmp");
                Files.write(temp, image);
                Files.move(temp, imageFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(imageFile);
            }
        } catch (IOException e) {
            throw new AppException("코디를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
        boolean hasImage = image != null && image.length > 0;
        SavedCodi saved = new SavedCodi(id, title, style == null ? CharacterStyle.REAL : style,
                spec == null ? CharacterSpec.defaults() : spec, productIds,
                hasImage ? imageFile.toString() : null, LocalDateTime.now());
        writeProperties(owner, saved);
        return saved;
    }

    private void writeProperties(String owner, SavedCodi codi) {
        Properties props = new Properties();
        props.setProperty("title", codi.title());
        props.setProperty("style", codi.style().name());
        props.setProperty("height", String.valueOf(codi.spec().heightCm()));
        props.setProperty("weight", String.valueOf(codi.spec().weightKg()));
        props.setProperty("bodyType", codi.spec().bodyType());
        props.setProperty("ageGroup", codi.spec().ageGroup());
        props.setProperty("savedAt", codi.savedAt().toString());
        codi.productIds().forEach((slot, productId) -> props.setProperty(SLOT_KEY + slot.name(), productId.toString()));
        Path dir = ownerDir(owner);
        try {
            Files.createDirectories(dir);
            Path temp = Files.createTempFile(dir, "codi_", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                props.store(writer, "sadamda codibook");
            }
            Files.move(temp, dir.resolve(codi.id() + ".properties"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AppException("코디를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    private void replace(String owner, SavedCodi codi) {
        List<SavedCodi> codis = load(owner);
        codis.removeIf(each -> each.id() == codi.id());
        codis.add(codi);
    }

    private Path ownerDir(String owner) {
        return baseDir.resolve(owner);
    }

    /** 처음 부를 때 폴더에서 읽어 둔다. 읽을 수 없는 파일은 건너뛴다. */
    private List<SavedCodi> load(String owner) {
        return cache.computeIfAbsent(owner, key -> {
            List<SavedCodi> result = new ArrayList<>();
            Path dir = ownerDir(key);
            if (!Files.isDirectory(dir)) {
                return result;
            }
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".properties"))
                        .collect(Collectors.toList())) {
                    SavedCodi codi = read(file);
                    if (codi != null) {
                        result.add(codi);
                    }
                }
            } catch (IOException ignored) {
                // 폴더를 읽지 못하면 빈 코디북으로 시작한다
            }
            return result;
        });
    }

    private static SavedCodi read(Path file) {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
            long id = Long.parseLong(file.getFileName().toString().replace(".properties", ""));
            Map<DressUpSlot, Long> productIds = new EnumMap<>(DressUpSlot.class);
            for (DressUpSlot slot : DressUpSlot.values()) {
                String value = props.getProperty(SLOT_KEY + slot.name());
                if (value != null) {
                    productIds.put(slot, Long.parseLong(value.trim()));
                }
            }
            CharacterStyle style;
            try {
                style = CharacterStyle.valueOf(props.getProperty("style", "REAL"));
            } catch (IllegalArgumentException e) {
                style = CharacterStyle.REAL;
            }
            CharacterSpec spec = new CharacterSpec(
                    Integer.parseInt(props.getProperty("height", "160")),
                    Integer.parseInt(props.getProperty("weight", "56")),
                    props.getProperty("bodyType"), props.getProperty("ageGroup"));
            Path image = file.resolveSibling(id + ".png");
            return new SavedCodi(id, props.getProperty("title", "이름 없는 코디"), style, spec, productIds,
                    Files.isRegularFile(image) ? image.toString() : null,
                    LocalDateTime.parse(props.getProperty("savedAt")));
        } catch (IOException | RuntimeException e) {
            return null; // 형식이 다른 파일은 코디가 아니다
        }
    }
}
