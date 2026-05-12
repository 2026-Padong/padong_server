package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreRegistrationResponse {

    private Long id;
    private String name;
    private StoreCategory category;
    private String categoryLabel;
    private String address;
    private String phoneNumber;
    private String description;
    private LocalTime openTime;
    private LocalTime closeTime;
    private int weekdayMask;
    private Double latitude;
    private Double longitude;
    private String thumbnailUrl;
    private long likeCount;
    private boolean likedByCurrentUser;
}
