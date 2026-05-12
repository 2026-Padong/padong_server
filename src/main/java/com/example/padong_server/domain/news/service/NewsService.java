package com.example.padong_server.domain.news.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.news.dto.NewsResponse;
import com.example.padong_server.domain.news.entity.NewsArticle;
import com.example.padong_server.domain.news.repository.NewsArticleRepository;
import com.example.padong_server.global.ResponseDTO;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
    private final NaverNewsProperties naverNewsProperties;

    /** adminDongId 기준 뉴스 조회. 없으면 외부 API 로 새로 가져와 저장 후 반환. */
    public ResponseDTO<NewsResponse> getNewsByAdminDongId(Long adminDongId) {
        if (adminDongId == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "adminDongId 는 필수입니다.");
        }
        AdminDong adminDong = adminDongRepository
                .findById(adminDongId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.VALIDATION_ERROR,
                        "존재하지 않는 행정동 PK 입니다: " + adminDongId));
        ensureNewsExists(adminDong);
        List<NewsArticle> articles =
                newsArticleRepository.findTop20ByAdminDongOrderByFetchedAtDescIdDesc(adminDong);
        return ResponseDTO.res(HttpStatus.OK, "news 조회 성공", NewsResponse.fromEntities(articles));
    }

    /** legacy 호환 — dongne 이름 기반 (스케줄러/refresh 흐름이 사용). */
    public ResponseDTO<NewsResponse> getNews(String dongne) {
        AdminDong adminDong = findAdminDongByNameContaining(dongne);
        ensureNewsExists(adminDong);
        List<NewsArticle> articles =
                newsArticleRepository.findTop20ByAdminDongOrderByFetchedAtDescIdDesc(adminDong);
        return ResponseDTO.res(HttpStatus.OK, "news 조회 성공", NewsResponse.fromEntities(articles));
    }

    public ResponseDTO<NewsResponse> getGeneralNews(String query) {
        NewsResponse response = requestNews(query);
        return ResponseDTO.res(HttpStatus.OK, "news 조회 성공", response);
    }

    /** 전체 저장된 뉴스에서 무작위 size 개 (기본 3, 최대 20). */
    public ResponseDTO<NewsResponse> getRandomNews(int size) {
        int capped = Math.min(Math.max(size, 1), 20);
        List<NewsArticle> articles = newsArticleRepository.findRandom(capped);
        return ResponseDTO.res(HttpStatus.OK, "랜덤 뉴스 조회 성공", NewsResponse.fromEntities(articles));
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
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "동 이름은 비어 있을 수 없습니다.");
        }

        return adminDongRepository.findFirstByAdminDongNameContainingOrderByIdAsc(dongne.trim())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.VALIDATION_ERROR, "일치하는 행정동이 없습니다: " + dongne));
    }

    private void saveLatestNews(AdminDong adminDong) {
        NewsResponse response = requestNews(adminDong.getAdminDongName());
        newsPersistenceService.saveNewsArticles(adminDong, response.getItems());
    }

    private NewsResponse requestNews(String query) {
        NewsResponse response = buildWebClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/news.json")
                        .queryParam("query", query)
                        .queryParam("display", NewsService.NEWS_DISPLAY_SIZE)
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
                .defaultHeader("X-Naver-Client-Id", naverNewsProperties.clientId())
                .defaultHeader("X-Naver-Client-Secret", naverNewsProperties.clientSecret())
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
