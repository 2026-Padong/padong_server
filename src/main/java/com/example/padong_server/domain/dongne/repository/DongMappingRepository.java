package com.example.padong_server.domain.dongne.repository;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.entity.DongMapping;
import com.example.padong_server.domain.dongne.entity.LegalDong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DongMappingRepository extends JpaRepository<DongMapping, Long> {

    boolean existsByAdminDongAndLegalDong(AdminDong adminDong, LegalDong legalDong);

    List<DongMapping> findByAdminDong(AdminDong adminDong);
}
