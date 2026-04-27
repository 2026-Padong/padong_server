package com.example.padong_server.domain.news.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.news.dto.NewsResponse;
import com.example.padong_server.domain.news.entity.NewsArticle;
import com.example.padong_server.domain.news.repository.NewsArticleRepository;
import com.example.padong_server.global.ResponseDTO;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    private static final String BASE_URL = "https://openapi.naver.com";
    private static final int NEWS_DISPLAY_SIZE = 20;

    private final AdminDongRepository adminDongRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsPersistenceService newsPersistenceService;
    private final WebClient.Builder webClientBuilder;

    @Value("${naver.news.client-id}")
    private String clientId;

    @Value("${naver.news.client-secret}")
    private String clientSecret;

    public ResponseDTO<NewsResponse> getNews(String dongne) {
        AdminDong adminDong = findAdminDongByNameContaining(dongne);
        ensureNewsExists(adminDong);

        List<NewsArticle> articles = newsArticleRepository.findTop20ByAdminDongOrderByFetchedAtDescIdDesc(adminDong);
        return ResponseDTO.res(HttpStatus.OK, "news 조회 성공", NewsResponse.fromEntities(articles));
    }

    public ResponseDTO<NewsResponse> getGeneralNews(String query) {
        NewsResponse response = requestNews(query, NEWS_DISPLAY_SIZE);
        return ResponseDTO.res(HttpStatus.OK, "news 조회 성공", response);
    }

    public void refreshNewsForAllAdminDongs() {
        List<AdminDong> adminDongs = adminDongRepository.findAll();
        for (AdminDong adminDong : adminDongs) {
            try {
                saveLatestNews(adminDong);
            } catch (RuntimeException exception) {
                log.warn("Failed to refresh news for adminDongId={}", adminDong.getId(), exception);
            }
        }
        log.info("Refreshed news for {} admin dongs", adminDongs.size());
    }

    public void refreshNewsForDongName(String dongne) {
        AdminDong adminDong = findAdminDongByNameContaining(dongne);
        saveLatestNews(adminDong);
    }

    private void ensureNewsExists(AdminDong adminDong) {
        List<NewsArticle> existingArticles = newsArticleRepository.findTop20ByAdminDongOrderByFetchedAtDescIdDesc(adminDong);
        if (existingArticles.isEmpty()) {
            saveLatestNews(adminDong);
        }
    }

    private AdminDong findAdminDongByNameContaining(String dongne) {
        if (dongne == null || dongne.isBlank()) {
            throw new IllegalArgumentException("동 이름은 비어 있을 수 없습니다.");
        }

        return adminDongRepository.findFirstByAdminDongNameContainingOrderByIdAsc(dongne.trim())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 행정동이 없습니다: " + dongne));
    }

    private void saveLatestNews(AdminDong adminDong) {
        NewsResponse response = requestNews(adminDong.getAdminDongName(), NEWS_DISPLAY_SIZE);
        newsPersistenceService.saveNewsArticles(adminDong, response.getItems());
    }

    private NewsResponse requestNews(String query, int display) {
        NewsResponse response = buildWebClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/news.json")
                        .queryParam("query", query)
                        .queryParam("display", display)
                        .queryParam("start", 1)
                        .build())
                .retrieve()
                .bodyToMono(NewsResponse.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("뉴스 응답이 비어 있습니다.");
        }

        if (response.getItems() == null) {
            response.setItems(List.of());
            return response;
        }

        response.getItems().forEach(item -> {
            item.setTitle(cleanHtml(item.getTitle()));
            item.setDescription(cleanHtml(item.getDescription()));
            item.setThumbnail(extractThumbnail(item.getOriginallink()));
        });
        return response;
    }

    private WebClient buildWebClient() {
        return webClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader("X-Naver-Client-Id", clientId)
                .defaultHeader("X-Naver-Client-Secret", clientSecret)
                .build();
    }

    private String extractThumbnail(String originallink) {
        if (originallink == null || originallink.isBlank()) {
            return null;
        }

        try {
            Document doc = Jsoup.connect(originallink)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .get();

            Element metaImage = doc.selectFirst("meta[property=og:image]");
            if (metaImage != null) {
                return metaImage.attr("content");
            }
        } catch (IOException exception) {
            log.debug("Failed to extract thumbnail from {}", originallink, exception);
        }
        return null;
    }

    private String cleanHtml(String html) {
        return html == null ? "" : Jsoup.parse(html).text();
    }
}
