package com.example.padongbe.domain.dongne.dto;

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
