package com.example.padong_server.domain.news.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 행정동별 뉴스 자동 갱신 — 6시간 주기.
 * application.yml 의 {@code news.scheduler.enabled=true} 일 때만 동작.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "news.scheduler.enabled", havingValue = "true")
public class NewsScheduler {

    private static final long SIX_HOURS_MS = 6L * 60 * 60 * 1000;

    private final NewsService newsService;

    @Scheduled(fixedDelay = SIX_HOURS_MS)
    public void refreshNews() {
        log.info("[news-scheduler] starting news refresh");
        newsService.refreshNewsForAllAdminDongs();
        log.info("[news-scheduler] finished news refresh");
    }
}
