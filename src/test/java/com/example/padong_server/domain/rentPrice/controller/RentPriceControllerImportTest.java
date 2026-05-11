package com.example.padong_server.domain.rentPrice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.rentPrice.dto.response.RentPriceImportResponse;
import com.example.padong_server.domain.rentPrice.service.RentPriceDataImportService;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentPriceControllerImportTest {

    @Mock
    private RentPriceService rentPriceService;

    @Mock
    private RentPriceDataImportService rentPriceDataImportService;

    @InjectMocks
    private RentPriceController rentPriceController;

    @Test
    @DisplayName("적재 API는 조회 서비스가 아니라 import service를 호출한다")
    void delegatesImportApiToImportService() {
        RentPriceImportResponse expected = new RentPriceImportResponse(1, 4L, 2L, 1L, 1L, 1L);
        when(rentPriceDataImportService.importData()).thenReturn(expected);

        RentPriceImportResponse result = rentPriceController.importRentPriceData().getBody();

        assertThat(result).isEqualTo(expected);
        verify(rentPriceDataImportService).importData();
    }
}
