package com.example.padong_server.domain.activityMobility.entity;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_mobility_period_arrival_departure",
            columnNames = {"start_month", "end_month", "arrival_dong_id", "departure_dong_id"}
        )
    }
)
public class Mobility {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "start_month", nullable = false, length = 6)
  private String startMonth;

  @Column(name = "end_month", nullable = false, length = 6)
  private String endMonth;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "departure_dong_id", nullable = false)
  private AdminDong departureDong;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "arrival_dong_id", nullable = false)
  private AdminDong arrivalDong;

  @Column(nullable = false)
  private double totalMobility;

  @Column(nullable = false)
  private double avgTime;
}
