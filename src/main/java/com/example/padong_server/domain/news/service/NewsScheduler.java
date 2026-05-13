package com.example.padong_server.domain.news.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NewsScheduler {

    private final NewsService newsService;

    // 뉴스 스케줄러 비활성화 — 필요 시 @Scheduled(fixedDelay = 21600000) 다시 추가.
    public void refreshNews() {
        log.info("Starting manual news refresh");
        newsService.refreshNewsForAllAdminDongs();
        log.info("Finished manual news refresh");
    }
}
