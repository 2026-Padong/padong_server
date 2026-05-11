package com.example.padong_server.domain.picture.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PictureItemResponse {

    private final String contentId;
    private final String title;
    private final String roadAddress;
    private final String firstImageUrl;
    private final String adminDongCode;
    private final String adminDongName;
}
