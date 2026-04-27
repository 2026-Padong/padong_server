package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HotplaceRealtimeItem {

    private final String category;
    private final String areaNm;
    private final String congestionLevel;
    private final String congestionMessage;
}
