package com.example.padong_server.domain.store.repository;

import com.example.padong_server.domain.store.entity.StoreStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreStatisticsRepository extends JpaRepository<StoreStatistics, Long> {
}
