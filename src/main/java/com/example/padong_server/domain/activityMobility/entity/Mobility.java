package com.example.padong_server.domain.activityMobility.entity;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mobility {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String month;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "departure_dong_id")
  private AdminDong departureDong;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "arrival_dong_id")
  private AdminDong arrivalDong;

  private double totalMobility;
  private double avgTime;
}
