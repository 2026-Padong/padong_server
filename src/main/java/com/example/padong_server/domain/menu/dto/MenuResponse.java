package com.example.padong_server.domain.menu.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuResponse {

    private Long id;
    private Long storeId;
    private String menuInfo;
    private Integer originalPrice;
    private Integer discountPrice;
    private String pickupAvailableTime;
    private String recruitmentDeadline;
    private String paymentMethod;
}
