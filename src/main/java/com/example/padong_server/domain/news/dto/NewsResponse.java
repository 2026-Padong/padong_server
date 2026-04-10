package com.example.padong_server.domain.news.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsResponse {
  List<News> items;

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class News{
    private String title;
    private String description;
    private String originallink;
    private String thumbnail;
  }
}
