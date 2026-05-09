package com.example.padong_server.domain.path.repository;

import com.example.padong_server.domain.path.entity.PathMode;
import com.example.padong_server.domain.path.entity.PathRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PathRecordRepository extends JpaRepository<PathRecord, Long> {

    Optional<PathRecord> findByModeAndDepartureDongCodeAndArrivalDongCode(
            PathMode mode, String departureDongCode, String arrivalDongCode);

    @Modifying
    @Query(
            value =
                    "INSERT INTO path_record "
                            + "(mode, departure_dong_code, arrival_dong_code, total_time,"
                            + " total_distance, created_at, updated_at) "
                            + "VALUES (:mode, :departureDongCode, :arrivalDongCode, :totalTime,"
                            + " :totalDistance, :now, :now) "
                            + "ON DUPLICATE KEY UPDATE "
                            + "total_time = VALUES(total_time), "
                            + "total_distance = VALUES(total_distance), "
                            + "updated_at = VALUES(updated_at)",
            nativeQuery = true)
    void upsert(
            @Param("mode") String mode,
            @Param("departureDongCode") String departureDongCode,
            @Param("arrivalDongCode") String arrivalDongCode,
            @Param("totalTime") int totalTime,
            @Param("totalDistance") int totalDistance,
            @Param("now") LocalDateTime now);
}
