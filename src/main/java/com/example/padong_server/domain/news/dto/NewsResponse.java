package com.example.padong_server.domain.news.dto;

import com.example.padong_server.domain.news.entity.NewsArticle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsResponse {
  private List<News> items = new ArrayList<>();

  public static NewsResponse fromEntities(List<NewsArticle> articles) {
    NewsResponse response = new NewsResponse();
    response.items = articles.stream()
        .map(News::fromEntity)
        .toList();
    return response;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class News{
    private Long adminDongId;
    private String title;
    private String description;
    private String originallink;
    private String thumbnail;

    public static News fromEntity(NewsArticle article) {
      News news = new News();
      news.adminDongId = article.getAdminDong().getId();
      news.title = article.getTitle();
      news.description = article.getDescription();
      news.originallink = article.getOriginallink();
      news.thumbnail = article.getThumbnail();
      return news;
    }
  }
}
