package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgeDetail {

    private final String rate10;
    private final String rate20;
    private final String rate30;
    private final String rate40;
    private final String rate50;
    private final String rate60;
    private final String rate70;
}
