package com.example.padong_server.domain.dongne.repository;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminDongRepository extends JpaRepository<AdminDong, Long> {

    Optional<AdminDong> findByAdminDongCode(String adminDongCode);

    Optional<AdminDong> findFirstByAdminDongCodeStartingWith(String adminDongCodePrefix);

    List<AdminDong> findAllByAdminDongCodeIn(Collection<String> adminDongCodes);

    Optional<AdminDong> findByCityNameAndDistrictNameAndAdminDongName(
            String cityName,
            String districtName,
            String adminDongName
    );
}
