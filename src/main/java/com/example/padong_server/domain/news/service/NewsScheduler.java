package com.example.padong_server.domain.news.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NewsScheduler {

    private final NewsService newsService;

    @Scheduled(fixedDelay = 21600000)
    public void refreshNews() {
        log.info("Starting scheduled news refresh");
        newsService.refreshNewsForAllAdminDongs();
        log.info("Finished scheduled news refresh");
    }
}
