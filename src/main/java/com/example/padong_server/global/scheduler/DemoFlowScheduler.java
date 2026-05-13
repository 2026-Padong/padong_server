package com.example.padong_server.global.scheduler;

import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시연용 데모 데이터 — 매일 자정 기준으로 가게별 모임을 자동 갱신.
 * <ul>
 *   <li>KST 00:00 — 어제 active flow(PENDING/WAITING_APPROVAL/APPROVED/READY) 모두 COMPLETED 처리</li>
 *   <li>KST 00:01 — 가게별 최신 flow 를 template 삼아 새 PENDING flow INSERT
 *       deadline = 가게 close_time - 30분 (자정 넘는 가게는 다음 날 close)</li>
 * </ul>
 *
 * <p>application.yml 의 {@code demo.scheduler.enabled=true} 일 때만 동작.
 * 운영 시연용 데이터 reset 용도라 실제 운영 시작 시 끄는 것이 자연스러움.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "demo.scheduler.enabled", havingValue = "true")
public class DemoFlowScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final List<OrderFlowStatus> ACTIVE_STATUSES = List.of(
            OrderFlowStatus.PENDING,
            OrderFlowStatus.WAITING_APPROVAL,
            OrderFlowStatus.APPROVED,
            OrderFlowStatus.READY);

    private final OrderFlowRepository orderFlowRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void completeYesterdayActiveFlows() {
        List<OrderFlow> active = orderFlowRepository.findAll().stream()
                .filter(f -> ACTIVE_STATUSES.contains(f.getStatus()))
                .toList();
        active.forEach(OrderFlow::completePickup);
        log.info("[demo-scheduler] completed {} active flows", active.size());
    }

    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void createTodayFlows() {
        Map<Long, OrderFlow> latestPerStore = new HashMap<>();
        for (OrderFlow f : orderFlowRepository.findAll()) {
            if (f.getStore() == null) continue;
            Long storeId = f.getStore().getId();
            OrderFlow cur = latestPerStore.get(storeId);
            if (cur == null || f.getId() > cur.getId()) {
                latestPerStore.put(storeId, f);
            }
        }

        LocalDate today = LocalDate.now(KST);
        int created = 0;
        for (OrderFlow template : latestPerStore.values()) {
            LocalTime startTime = template.getRecruitmentStart() != null
                    ? template.getRecruitmentStart().toLocalTime()
                    : LocalTime.of(0, 0);

            LocalDateTime start = LocalDateTime.of(today, startTime);
            LocalDateTime deadline = computeDeadline(template, today);

            OrderFlow createdFlow = OrderFlow.builder()
                    .store(template.getStore())
                    .menu(template.getMenu())
                    .status(OrderFlowStatus.PENDING)
                    .recruitmentStart(start)
                    .recruitmentDeadline(deadline)
                    .minOrderPerPerson(template.getMinOrderPerPerson())
                    .paymentMethod(template.getPaymentMethod())
                    .maxParticipants(template.getMaxParticipants())
                    .currentParticipants(1)
                    .build();
            orderFlowRepository.save(createdFlow);
            created++;
        }
        log.info("[demo-scheduler] created {} new flows for {}", created, today);
    }

    /** 마감 시각 — 가게 close 30분 전. 자정 넘는 가게는 내일 close - 30분. */
    private LocalDateTime computeDeadline(OrderFlow template, LocalDate today) {
        if (template.getStore() == null
                || template.getStore().getCloseTime() == null
                || template.getStore().getOpenTime() == null) {
            return LocalDateTime.of(today, LocalTime.of(22, 0));
        }
        LocalTime close = template.getStore().getCloseTime();
        LocalTime open = template.getStore().getOpenTime();
        LocalTime deadlineTime = close.minusMinutes(30);
        boolean overnight = close.isBefore(open);
        return overnight
                ? LocalDateTime.of(today.plusDays(1), deadlineTime)
                : LocalDateTime.of(today, deadlineTime);
    }
}
