package com.sadamda.model.attendance;

import com.sadamda.common.AppException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 옷장 출석 (UC-21): 하루 한 번 출석하면 그날의 코디를 받고, 한 주에 5칸을 채우면 무료배송 쿠폰을 1장 준다. 담당: 정우진
 *
 * 연속 출석(하루 놓치면 처음부터)이 아니라 "이번 주 7칸 중 몇 칸"으로 센다. 하루 빠져도 그 주가 끝나지 않아
 * 중장년 사용자가 부담 없이 이어 갈 수 있게 했다. 쿠폰은 주마다 한 번만 준다.
 *
 * 회원·주문은 아직 메모리에만 있지만 출석은 다시 켜도 남아야 해서 이 PC 에 둔다.
 * - 파일: 사용자 홈/.sadamda/attendance/{회원 열쇠}.properties (회원 열쇠는 OwnerKeys)
 * - days = 출석한 날짜들, coupons = 남은 쿠폰 수, rewardedWeeks = 쿠폰을 준 주의 월요일들
 */
public class AttendanceService {
    /** 한 주에 이만큼 출석하면 무료배송 쿠폰 1장. */
    public static final int COUPON_DAYS = 5;

    private final Path baseDir;
    private final Map<String, Record> cache = new HashMap<>();

    public AttendanceService() {
        this(Path.of(System.getProperty("user.home"), ".sadamda", "attendance"));
    }

    public AttendanceService(Path baseDir) {
        this.baseDir = baseDir;
    }

    /** 이번 주 상태 (출석은 하지 않는다). */
    public synchronized AttendanceWeek week(String owner, LocalDate today) {
        requireOwner(owner);
        return toWeek(load(owner), today, false);
    }

    /**
     * UC-21 2~4단계 오늘 출석. 이미 했으면 아무것도 바꾸지 않고 상태만 돌려준다 (2a).
     * 이번 주 COUPON_DAYS 칸을 처음 채우면 쿠폰 1장을 준다 (4단계).
     */
    public synchronized AttendanceWeek checkIn(String owner, LocalDate today) {
        requireOwner(owner);
        Record record = load(owner);
        if (record.days.contains(today)) {
            return toWeek(record, today, false);
        }
        record.days.add(today);
        LocalDate monday = mondayOf(today);
        boolean earned = false;
        if (countInWeek(record, monday) >= COUPON_DAYS && !record.rewardedWeeks.contains(monday)) {
            record.rewardedWeeks.add(monday);
            record.coupons++;
            earned = true;
        }
        try {
            save(owner, record);
        } catch (AppException e) {
            // 파일에 못 남겼으면 화면에도 출석하지 않은 것으로 되돌린다
            record.days.remove(today);
            if (earned) {
                record.rewardedWeeks.remove(monday);
                record.coupons--;
            }
            throw e;
        }
        return toWeek(record, today, earned);
    }

    /** 가지고 있는 무료배송 쿠폰 수. */
    public synchronized int coupons(String owner) {
        requireOwner(owner);
        return load(owner).coupons;
    }

    /** 무료배송 쿠폰 1장을 쓴다 (UC-11 결제 뒤). 없으면 AppException. */
    public synchronized void useCoupon(String owner) {
        requireOwner(owner);
        Record record = load(owner);
        if (record.coupons <= 0) {
            throw new AppException("쓸 수 있는 무료배송 쿠폰이 없어요.");
        }
        record.coupons--;
        try {
            save(owner, record);
        } catch (AppException e) {
            record.coupons++;
            throw e;
        }
    }

    public static LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static AttendanceWeek toWeek(Record record, LocalDate today, boolean earned) {
        LocalDate monday = mondayOf(today);
        Set<LocalDate> stamped = record.days.stream()
                .filter(day -> !day.isBefore(monday) && day.isBefore(monday.plusDays(7)))
                .collect(Collectors.toCollection(TreeSet::new));
        return new AttendanceWeek(monday, today, stamped, record.coupons, earned, record.days.size());
    }

    private static long countInWeek(Record record, LocalDate monday) {
        return record.days.stream()
                .filter(day -> !day.isBefore(monday) && day.isBefore(monday.plusDays(7)))
                .count();
    }

    private static void requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new AppException("로그인한 뒤에 출석할 수 있어요.");
        }
    }

    private Record load(String owner) {
        return cache.computeIfAbsent(owner, key -> {
            Record record = new Record();
            Path file = baseDir.resolve(key + ".properties");
            if (!Files.isRegularFile(file)) {
                return record;
            }
            Properties props = new Properties();
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
                record.days.addAll(dates(props.getProperty("days", "")));
                record.rewardedWeeks.addAll(dates(props.getProperty("rewardedWeeks", "")));
                record.coupons = Math.max(0, Integer.parseInt(props.getProperty("coupons", "0").trim()));
            } catch (IOException | RuntimeException ignored) {
                // 읽을 수 없으면 새로 시작한다
            }
            return record;
        });
    }

    private void save(String owner, Record record) {
        Properties props = new Properties();
        props.setProperty("days", join(record.days));
        props.setProperty("rewardedWeeks", join(record.rewardedWeeks));
        props.setProperty("coupons", String.valueOf(record.coupons));
        try {
            Files.createDirectories(baseDir);
            Path temp = Files.createTempFile(baseDir, "attendance_", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                props.store(writer, "sadamda attendance");
            }
            Files.move(temp, baseDir.resolve(owner + ".properties"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AppException("출석을 저장하지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    private static Set<LocalDate> dates(String text) {
        Set<LocalDate> result = new TreeSet<>();
        Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(value -> result.add(LocalDate.parse(value)));
        return result;
    }

    private static String join(Set<LocalDate> dates) {
        return dates.stream().map(LocalDate::toString).collect(Collectors.joining(","));
    }

    private static final class Record {
        final Set<LocalDate> days = new TreeSet<>();
        final Set<LocalDate> rewardedWeeks = new TreeSet<>();
        int coupons;
    }
}
