package com.example.padong_server.domain.picture.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminDongPictureResponse {

    private final String adminDongCode;
    private final String adminDongName;
    private final int pictureCount;
    private final List<PictureItemResponse> pictures;
}
