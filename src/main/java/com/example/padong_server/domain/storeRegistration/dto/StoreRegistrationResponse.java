package com.example.padong_server.domain.storeRegistration.dto;

import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreRegistrationResponse {

    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private LocalTime openTime;
    private LocalTime closeTime;
    private long likeCount;
    private boolean likedByCurrentUser;
}
