package com.example.padong_server.domain.dongne.dto;

import com.example.padongbe.domain.rentPrice.dto.response.RentPriceDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DetailResponse {

  private String adminDongCode;
  private double score;
  private double totalMobility;
  private double avgTime;
  private double density;
  private double trafficAccidents;
  private double fires;
  private double crimes;
  private double publicSafety;
  private RentPriceDto[] rentPrice;
  private LocationResponse[] location;

}
