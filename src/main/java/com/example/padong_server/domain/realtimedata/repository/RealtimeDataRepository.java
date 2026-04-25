package com.example.padong_server.domain.realtimedata.repository;

import com.example.padong_server.domain.realtimedata.entity.RealtimeData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RealtimeDataRepository extends JpaRepository<RealtimeData, Long> {
}
