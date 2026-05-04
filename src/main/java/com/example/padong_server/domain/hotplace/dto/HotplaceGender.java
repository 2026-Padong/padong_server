package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HotplaceGender {

    private final String maleRate;
    private final String femaleRate;
}
