package com.example.padongbe.domain.activityMobility.dto;

import com.example.padongbe.domain.dongne.dto.AdminDongDto;
import com.example.padongbe.domain.dongne.entity.AdminDong;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MobilityResponse {
  private AdminDongDto departureDong;
  private double score;
  private double totalMobility;
  private double avgTime;
  private double density;
  private double safety;
  private double avgJeonseDeposit;
  private double avgMonthlyDeposit;
  private double avgMonthlyRent;
}
