package com.example.padong_server.global.client.tour;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TourApiQuotaService {

    public static final int DAILY_LIMIT = 1000;

    private final TourApiDailyUsageRepository usageRepository;

    @Transactional(readOnly = true)
    public int getRemainingCalls(LocalDate usageDate) {
        return Math.max(0, DAILY_LIMIT - getUsedCalls(usageDate));
    }

    @Transactional(readOnly = true)
    public int getUsedCalls(LocalDate usageDate) {
        return usageRepository.findById(usageDate)
                .map(TourApiDailyUsage::getRequestCount)
                .orElse(0);
    }

    @Transactional
    public int consumeCalls(LocalDate usageDate, int count) {
        TourApiDailyUsage usage = usageRepository.findById(usageDate)
                .orElseGet(() -> new TourApiDailyUsage(usageDate, 0));
        usage.addRequests(count);
        usageRepository.save(usage);
        return usage.getRequestCount();
    }
}
