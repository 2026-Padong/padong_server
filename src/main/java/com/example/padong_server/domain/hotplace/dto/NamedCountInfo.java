package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NamedCountInfo {

    private final Integer count;
    private final List<String> names;
}
