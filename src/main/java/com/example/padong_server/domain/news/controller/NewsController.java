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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "뉴스 정보 요청 API")
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/search/youth-house")
    @Operation(summary = "청년주택 뉴스 조회")
    public ResponseEntity<ResponseDTO<NewsResponse>> getYouthHouseNews() {
        return ResponseEntity.ok(newsService.getNews("청년 주택"));
    }

    @GetMapping("/search/house-price")
    @Operation(summary = "주택가격 뉴스 조회")
    public ResponseEntity<ResponseDTO<NewsResponse>> getHousePriceNews() {
        return ResponseEntity.ok(newsService.getNews("주택 가격"));
    }
}
