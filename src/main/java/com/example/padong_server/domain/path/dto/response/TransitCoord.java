package com.example.padong_server.domain.path.dto.response;

import com.example.padong_server.domain.path.dto.internal.OdsayJson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "좌표(경도/위도)")
public class TransitCoord {

    @Schema(description = "경도", example = "126.902682")
    private double x;

    @Schema(description = "위도", example = "37.534863")
    private double y;

    static TransitCoord from(Object x, Object y) {
        return TransitCoord.builder()
                .x(OdsayJson.doubleValue(x, 0.0))
                .y(OdsayJson.doubleValue(y, 0.0))
                .build();
    }
}
