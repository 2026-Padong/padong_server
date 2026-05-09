package com.example.padong_server.domain.path.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
	name = "path_record",
	uniqueConstraints =
	@UniqueConstraint(
		name = "uk_path_record",
		columnNames = {"mode", "departure_dong_code", "arrival_dong_code"}),
	indexes = @Index(name = "idx_path_record_updated", columnList = "updated_at"))
public class PathRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PathMode mode;

	@Column(name = "departure_dong_code", nullable = false, length = 10)
	private String departureDongCode;

	@Column(name = "arrival_dong_code", nullable = false, length = 10)
	private String arrivalDongCode;

	@Column(name = "total_time", nullable = false)
	private int totalTime;

	@Column(name = "total_distance", nullable = false)
	private int totalDistance;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
