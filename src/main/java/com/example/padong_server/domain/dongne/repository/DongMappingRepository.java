package com.example.padong_server.domain.dongne.repository;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.entity.DongMapping;
import com.example.padong_server.domain.dongne.entity.LegalDong;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DongMappingRepository extends JpaRepository<DongMapping, Long> {

    boolean existsByAdminDongAndLegalDong(AdminDong adminDong, LegalDong legalDong);

    List<DongMapping> findByAdminDong(AdminDong adminDong);

    @Query("select mapping from DongMapping mapping join fetch mapping.adminDong join fetch mapping.legalDong")
    List<DongMapping> findAllWithAdminAndLegal();
}
