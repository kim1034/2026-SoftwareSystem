package com.sadamda.controller;

import com.sadamda.common.AppException;
import com.sadamda.common.OwnerKeys;
import com.sadamda.model.attendance.AttendanceService;
import com.sadamda.model.attendance.AttendanceWeek;
import com.sadamda.model.member.BodyProfile;
import com.sadamda.model.member.Member;
import com.sadamda.model.recommend.OutfitHelper;
import com.sadamda.model.recommend.OutfitSuggestion;

import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * UC-21 옷장 출석. 모든 메서드 로그인 필요 (LoginRequiredException). 담당: 정우진
 * 홈 화면의 "옷장 출석" 칸에서 부른다. 무료배송 쿠폰을 쓰는 곳은 OrderController.checkout.
 */
public class AttendanceController {
    private final Session session;
    private final AttendanceService attendanceService;
    private final OutfitHelper outfitHelper;
    private final Supplier<LocalDate> today;

    public AttendanceController(Session session, AttendanceService attendanceService, OutfitHelper outfitHelper) {
        this(session, attendanceService, outfitHelper, LocalDate::now);
    }

    /** today 는 시험할 때 날짜를 정해 넣으려고 받는다. */
    public AttendanceController(Session session, AttendanceService attendanceService, OutfitHelper outfitHelper,
                                Supplier<LocalDate> today) {
        this.session = session;
        this.attendanceService = attendanceService;
        this.outfitHelper = outfitHelper;
        this.today = today;
    }

    /** UC-21 1단계 이번 주 7칸 (출석은 하지 않는다). */
    public AttendanceWeek week() {
        return attendanceService.week(owner(), today.get());
    }

    /** UC-21 2~4단계 [오늘 출석하고 코디 받기]. 이미 했으면 상태만 돌려준다 (2a). */
    public AttendanceWeek checkIn() {
        return attendanceService.checkIn(owner(), today.get());
    }

    /**
     * UC-21 3·5단계 출석한 날의 코디. 회원이 등록한 체형·연령대에 맞춘다.
     * 이번 주에 출석하지 않은 날이면 AppException (5a). 고를 상품이 없으면 null.
     */
    public OutfitSuggestion outfitOf(LocalDate day) {
        AttendanceWeek week = week();
        if (!week.isStamped(day)) {
            throw new AppException("출석한 날의 코디만 볼 수 있어요.");
        }
        BodyProfile profile = session.currentMember().getBodyProfile();
        return outfitHelper.dailyOutfit(day, profile == null ? null : profile.bodyType(),
                profile == null ? null : profile.ageGroup());
    }

    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    private String owner() {
        Member member = session.requireLogin();
        return OwnerKeys.of(member.getEmail());
    }
}
