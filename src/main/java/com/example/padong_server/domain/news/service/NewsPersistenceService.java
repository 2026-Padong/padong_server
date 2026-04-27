package com.example.padong_server.domain.news.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.news.dto.NewsResponse;
import com.example.padong_server.domain.news.entity.NewsArticle;
import com.example.padong_server.domain.news.repository.NewsArticleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsPersistenceService {

    private final NewsArticleRepository newsArticleRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNewsArticles(AdminDong adminDong, List<NewsResponse.News> items) {
        LocalDateTime fetchedAt = LocalDateTime.now();

        for (NewsResponse.News item : items) {
            if (item.getOriginallink() == null || item.getOriginallink().isBlank()) {
                continue;
            }
            if (newsArticleRepository.existsByAdminDongAndOriginallink(adminDong, item.getOriginallink())) {
                continue;
            }

            NewsArticle article = NewsArticle.of(
                    adminDong,
                    item.getTitle(),
                    item.getDescription(),
                    item.getOriginallink(),
                    item.getThumbnail(),
                    fetchedAt
            );
            newsArticleRepository.save(article);
        }
    }
}
