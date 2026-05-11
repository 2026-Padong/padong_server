package com.example.padong_server.domain.activityMobility.repository;

import com.example.padong_server.domain.activityMobility.entity.SafetyIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SafetyIndexRepository extends JpaRepository<SafetyIndex, Long> {

    Optional<SafetyIndex> findByCityNameAndDistrictName(String cityName, String districtName);

    Optional<SafetyIndex> findByDistrictName(String districtName);
}
