package com.sadamda.model.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 옷장 출석(UC-21)의 이번 주 상태. 한 주는 월요일부터 일요일까지 7칸이다.
 *
 * @param monday          이번 주 월요일
 * @param today           오늘
 * @param stamped         이번 주에 출석한 날
 * @param coupons         가지고 있는 무료배송 쿠폰 수
 * @param couponJustEarned 방금 출석으로 이번 주 쿠폰을 받았는지
 * @param totalDays       지금까지 출석한 날 수
 */
public record AttendanceWeek(LocalDate monday, LocalDate today, Set<LocalDate> stamped, int coupons,
                             boolean couponJustEarned, int totalDays) {
    public AttendanceWeek {
        stamped = Set.copyOf(new TreeSet<>(stamped));
    }

    /** 월요일부터 일요일까지 7일. */
    public List<LocalDate> days() {
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(monday.plusDays(i));
        }
        return days;
    }

    public boolean isStamped(LocalDate day) {
        return stamped.contains(day);
    }

    public boolean checkedToday() {
        return stamped.contains(today);
    }

    public int stampedCount() {
        return stamped.size();
    }

    /** 이번 주 쿠폰을 받았는지 (도장 수가 기준을 넘었는지). */
    public boolean couponDoneThisWeek() {
        return stamped.size() >= AttendanceService.COUPON_DAYS;
    }

    /** 이번 주 쿠폰까지 남은 칸 수. 이미 받았으면 0. */
    public int daysLeftForCoupon() {
        return Math.max(0, AttendanceService.COUPON_DAYS - stamped.size());
    }
}
