package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주거 실거래가 적재 결과")
public record RentPriceImportResponse(
        @Schema(description = "저장된 행정동 + 건물유형 기준 rent_price 통계 건수", example = "1692")
        int savedStatCount,

        @Schema(description = "원본 CSV 전체 row 수", example = "1497726")
        long sourceRowCount,

        @Schema(description = "유효 매매 row 수. 취소/결측 매매는 제외.", example = "231263")
        long saleRowCount,

        @Schema(description = "유효 전세 row 수", example = "495772")
        long jeonseRowCount,

        @Schema(description = "유효 월세 row 수", example = "756246")
        long monthlyRentRowCount,

        @Schema(description = "취소 매매, 결측 금액, 비정상 전월세 구분 등으로 제외한 row 수", example = "14445")
        long skippedRowCount
) {
}
