package com.example.padongbe.domain.dongne.repository;

import com.example.padongbe.domain.dongne.entity.AdminDong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminDongRepository extends JpaRepository<AdminDong, Long> {

    Optional<AdminDong> findByAdminDongCode(String adminDongCode);
    Optional<AdminDong> findByAdminTypeCode(String adminTypeCode);
    Optional<AdminDong> findByCityAndDistrictAndAdminAreaName(String city, String district, String dong);

}
