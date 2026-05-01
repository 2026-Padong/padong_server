package com.example.padong_server.domain.activityMobility.dto;

import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommonDepartureMobilityResponse {

    private AdminDongDto departureDong;
    private double totalMobility;
}
