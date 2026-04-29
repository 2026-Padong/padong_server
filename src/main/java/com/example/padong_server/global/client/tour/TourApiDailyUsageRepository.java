package com.example.padong_server.global.client.tour;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface TourApiDailyUsageRepository extends JpaRepository<TourApiDailyUsage, LocalDate> {
}
