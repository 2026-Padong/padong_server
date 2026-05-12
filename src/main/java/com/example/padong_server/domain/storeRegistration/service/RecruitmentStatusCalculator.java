package com.example.padong_server.domain.storeRegistration.service;

import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import com.example.padong_server.domain.storeRegistration.entity.RecruitmentStatus;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentStatusCalculator {

    public RecruitmentStatus calculate(Store store, OrderFlow activeFlow, LocalDateTime now) {
        if (!isWithinBusinessHours(store, now)) {
            return RecruitmentStatus.OUT_OF_HOURS;
        }
        if (activeFlow == null) {
            return RecruitmentStatus.NO_FLOW;
        }
        OrderFlowStatus s = activeFlow.effectiveStatus(now);
        return switch (s) {
            case PENDING -> activeFlow.isClosingSoon()
                    ? RecruitmentStatus.CLOSING_SOON
                    : RecruitmentStatus.RECRUITING;
            case WAITING_APPROVAL, APPROVED, READY -> RecruitmentStatus.IN_PROGRESS;
            default -> RecruitmentStatus.NO_FLOW;
        };
    }

    /** weekdayMask (bit0=MON..bit6=SUN) + openTime/closeTime 기준 영업 시간 내 여부. */
    private boolean isWithinBusinessHours(Store store, LocalDateTime now) {
        if (store == null) return false;
        Integer mask = store.getWeekdayMask();
        if (mask == null || mask == 0) return false;
        int dowBit = 1 << (now.getDayOfWeek().getValue() - 1); // MON=bit0
        if ((mask & dowBit) == 0) return false;

        LocalTime open = store.getOpenTime();
        LocalTime close = store.getCloseTime();
        if (open == null || close == null) return true;
        LocalTime t = now.toLocalTime();
        if (close.isAfter(open)) {
            return !t.isBefore(open) && t.isBefore(close);
        }
        // 자정 넘어가는 영업 (e.g. 22:00 ~ 02:00)
        return !t.isBefore(open) || t.isBefore(close);
    }

    // DayOfWeek 의미 한 줄: MON=1, SUN=7 — bit0=MON 이므로 (value-1) shift.
    @SuppressWarnings("unused")
    private static int dayBit(DayOfWeek d) {
        return 1 << (d.getValue() - 1);
    }
}
