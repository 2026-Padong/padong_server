package com.example.padong_server.domain.news.controller;

import com.example.padong_server.domain.news.dto.NewsResponse;
import com.example.padong_server.domain.news.service.NewsService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "동네 이름으로 저장된 뉴스 20건 조회")
    public ResponseEntity<ResponseDTO<NewsResponse>> getNews(@RequestParam String dongne) {
        return ResponseEntity.ok(newsService.getNews(dongne));
    }
}
