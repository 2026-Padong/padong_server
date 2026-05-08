package com.example.padong_server.domain.transitPath.dto.request;

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
@Schema(description = "행정동 A → 행정동 B 보행자 경로 요청")
public class PedestrianPathRequest {

    @Schema(description = "출발 행정동 코드", example = "1162069500")
    @NotBlank
    private String departureDongCode;

    @Schema(description = "도착 행정동 코드", example = "1168064000")
    @NotBlank
    private String arrivalDongCode;
}
