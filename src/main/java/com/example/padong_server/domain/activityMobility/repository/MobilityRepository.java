package com.example.padong_server.domain.activityMobility.repository;

import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface MobilityRepository extends JpaRepository<Mobility,Long> {

  Page<Mobility> findByArrivalDong(AdminDong adminDong, Pageable pageable);

  List<Mobility> findByArrivalDong(AdminDong adminDong);

  @Query("SELECT MIN(m.totalMobility) FROM Mobility m")
  Double findMinMobility();

  @Query("SELECT MAX(m.totalMobility) FROM Mobility m")
  Double findMaxMobility();

  @Query("SELECT MIN(m.avgTime) FROM Mobility m")
  Double findMinAvgTime();

  @Query("SELECT MAX(m.avgTime) FROM Mobility m")
  Double findMaxAvgTime();


  Optional<Mobility> findByArrivalDongAndDepartureDong(AdminDong arrivalDong, AdminDong departureDong);
}
