package com.example.padong_server.domain.dongne.repository;

import com.example.padong_server.domain.dongne.entity.LegalDong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LegalDongRepository extends JpaRepository<LegalDong, Long> {
    Optional<LegalDong> findByLegalDongCode(String legalDongCode);

    Optional<LegalDong> findByCityNameAndDistrictNameAndLegalDongName(
            String cityName,
            String districtName,
            String legalDongName
    );
}
