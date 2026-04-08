package com.example.padongbe.domain.dongne.repository;

import com.example.padongbe.domain.dongne.entity.AdminDong;
import com.example.padongbe.domain.dongne.entity.DongMapping;
import com.example.padongbe.domain.dongne.entity.LegalDong;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DongMappingRepository extends JpaRepository<DongMapping, Long>{
    boolean existsByAdminDongAndLegalDong(AdminDong admin, LegalDong legal);

    List<DongMapping> findByAdminDong(AdminDong admin);
}
