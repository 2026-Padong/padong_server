package com.example.padong_server.domain.storeRegistration.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreRegistrationResponse {

    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private String operatingHours;
}
