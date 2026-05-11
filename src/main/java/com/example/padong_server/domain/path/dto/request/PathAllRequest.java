package com.example.padong_server.domain.path.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "행정동 A → 행정동 B 통합 길찾기 요청 (대중교통/보행자/자동차 한번에)")
public class PathAllRequest {

    @Schema(description = "출발 행정동 코드", example = "1162069500")
    @NotBlank
    private String departureDongCode;

    @Schema(description = "도착 행정동 코드", example = "1168064000")
    @NotBlank
    private String arrivalDongCode;
}
