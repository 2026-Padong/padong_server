package com.example.padong_server.domain.dongne.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LocationResponse {
  private String address;
  private String type;
  private double lat;
  private double lng;
}
