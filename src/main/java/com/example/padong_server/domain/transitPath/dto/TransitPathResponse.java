package com.example.padong_server.domain.transitPath.dto;

import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(description = "행정동 A → 행정동 B 대중교통 길찾기 응답")
public class TransitPathResponse {

    @Schema(description = "출발 행정동")
    private AdminDongDto departureDong;

    @Schema(description = "도착 행정동")
    private AdminDongDto arrivalDong;

    @Schema(description = "검색 종류 (0: 도시내, 1: 도시간)", example = "0")
    private int searchType;

    @Schema(description = "응답 경로 개수", example = "1")
    private int pathCount;

    @Schema(description = "추천 경로 목록")
    private List<TransitPath> paths;

    public static TransitPathResponse from(
            AdminDong departureDong, AdminDong arrivalDong, Map<String, Object> body) {
        Map<String, Object> result = OdsayJson.map(body.get("result"));
        Preconditions.validate(!result.isEmpty(), ErrorCode.ODSAY_NO_RESULT);

        List<TransitPath> paths = OdsayJson.mapList(result.get("path")).stream()
                .map(TransitPath::from)
                .toList();
        Preconditions.validate(!paths.isEmpty(), ErrorCode.ODSAY_NO_RESULT);

        return TransitPathResponse.builder()
                .departureDong(AdminDongDto.from(departureDong))
                .arrivalDong(AdminDongDto.from(arrivalDong))
                .searchType(OdsayJson.intValue(result.get("searchType"), 0))
                .pathCount(paths.size())
                .paths(paths)
                .build();
    }
}
