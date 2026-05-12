package com.example.padong_server.domain.rentPrice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.RentType;
import com.example.padong_server.global.client.s3.S3CsvReaderService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RentPriceDataUtilTest {

    private final RentPriceDataUtil rentPriceDataUtil =
            new RentPriceDataUtil(mock(S3CsvReaderService.class));

    @Test
    @DisplayName("Reads sale rows after the metadata header and skips cancelled or invalid rows")
    void readsSaleRowsAfterFindingHeader() {
        RentPriceDataUtil.RentPriceCsvFile file = new RentPriceDataUtil.RentPriceCsvFile(
                "test",
                "test-sale.csv",
                "APARTMENT",
                RentPriceDataUtil.SourceType.SALE,
                "전용면적(㎡)");

        RentPriceRawData rawData =
                rentPriceDataUtil.readRows(
                        file,
                        linesWithMetadata(
                                "NO,시군구,전용면적(㎡),거래금액(만원),해제사유발생일,법정동코드",
                                "1,서울,84.5,\"123,456\",-,1111010100",
                                "2,서울,59.9,\"100,000\",20250101,1111010100",
                                "3,서울,59.9,-,-,1111010100"));

        assertThat(rawData.sourceRowCount()).isEqualTo(3);
        assertThat(rawData.skippedRowCount()).isEqualTo(2);
        assertThat(rawData.saleRows())
                .hasSize(1)
                .first()
                .satisfies(
                        row -> {
                            assertThat(row.legalDongCode()).isEqualTo("1111010100");
                            assertThat(row.buildingType()).isEqualTo("APARTMENT");
                            assertThat(row.salePrice()).isEqualTo(123_456L);
                            assertThat(row.area()).isEqualByComparingTo(new BigDecimal("84.5"));
                        });
    }

    @Test
    @DisplayName("Splits rent rows into jeonse and monthly-rent records")
    void readsRentRowsByRentType() {
        RentPriceDataUtil.RentPriceCsvFile file = new RentPriceDataUtil.RentPriceCsvFile(
                "test",
                "test-rent.csv",
                "OFFICETEL",
                RentPriceDataUtil.SourceType.RENT,
                "전용면적(㎡)");

        RentPriceRawData rawData =
                rentPriceDataUtil.readRows(
                        file,
                        linesWithMetadata(
                                "NO,시군구,전월세구분,전용면적(㎡),보증금(만원),월세금(만원),법정동코드",
                                "1,서울,전세,40.0,\"30,000\",-,1111010100",
                                "2,서울,월세,25.0,\"1,000\",80,1111010100",
                                "3,서울,반전세,25.0,\"1,000\",80,1111010100"));

        assertThat(rawData.sourceRowCount()).isEqualTo(3);
        assertThat(rawData.skippedRowCount()).isEqualTo(1);
        assertThat(rawData.rentRows()).hasSize(2);
        assertThat(rawData.rentRows().get(0))
                .satisfies(
                        row -> {
                            assertThat(row.rentType()).isEqualTo(RentType.JEONSE);
                            assertThat(row.deposit()).isEqualTo(30_000L);
                            assertThat(row.monthlyRent()).isNull();
                            assertThat(row.area()).isEqualByComparingTo(new BigDecimal("40.0"));
                        });
        assertThat(rawData.rentRows().get(1))
                .satisfies(
                        row -> {
                            assertThat(row.rentType()).isEqualTo(RentType.MONTHLY_RENT);
                            assertThat(row.deposit()).isEqualTo(1_000L);
                            assertThat(row.monthlyRent()).isEqualTo(80L);
                        });
    }

    @Test
    @DisplayName("CSV parser keeps commas inside quoted fields")
    void parsesQuotedComma() {
        List<String> columns = RentPriceDataUtil.parseCsvLine("1,\"12,345\",\"a\"\"b\",끝");

        assertThat(columns).containsExactly("1", "12,345", "a\"b", "끝");
    }

    private List<String> linesWithMetadata(String header, String... dataRows) {
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < 15; index++) {
            lines.add("metadata line " + index);
        }
        lines.add(header);
        lines.addAll(List.of(dataRows));
        return lines;
    }
}
