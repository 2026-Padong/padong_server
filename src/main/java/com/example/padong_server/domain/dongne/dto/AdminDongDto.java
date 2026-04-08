package com.example.padong_server.domain.dongne.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDongDto {
    private String adminDongCode;
    private String address;
}
