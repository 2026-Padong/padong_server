package com.example.padong_server.domain.news.controller;

import com.example.padong_server.domain.news.dto.NewsResponse;
import com.example.padong_server.domain.news.service.NewsService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@Tag(name = "뉴스", description = "뉴스 정보 조회 API")
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/search")
    @Operation(summary = "행정동 뉴스 20건 조회", description = "adminDongId 기준.")
    public ResponseEntity<ResponseDTO<NewsResponse>> getNews(
            @Parameter(description = "행정동 PK", example = "12") @RequestParam Long adminDongId) {
        return ResponseEntity.ok(newsService.getNewsByAdminDongId(adminDongId));
    }

    @GetMapping("/random")
    @Operation(
            summary = "랜덤 뉴스 조회",
            description = "저장된 전체 뉴스 중 무작위 size 개 반환 (기본 3, 최대 20). 동네 무관.")
    public ResponseEntity<ResponseDTO<NewsResponse>> getRandomNews(
            @Parameter(description = "반환할 뉴스 개수 (기본 3, 최대 20)", example = "3")
            @RequestParam(required = false, defaultValue = "3") int size) {
        return ResponseEntity.ok(newsService.getRandomNews(size));
    }
}
