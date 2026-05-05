package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HotplaceTransport {

    private final SubwayTransportInfo subway;
    private final NamedCountInfo bus;
    private final NamedCountInfo bike;
}
