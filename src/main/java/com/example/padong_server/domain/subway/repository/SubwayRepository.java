package com.example.padong_server.domain.subway.repository;

import com.example.padong_server.domain.subway.entity.Subway;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubwayRepository extends JpaRepository<Subway, Long> {
}
