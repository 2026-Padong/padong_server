package com.example.padong_server.domain.dongne.repository;

import com.example.padong_server.domain.dongne.entity.AdminDongBoundary;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminDongBoundaryRepository extends JpaRepository<AdminDongBoundary, Long> {

    Optional<AdminDongBoundary> findByAdminDong_AdminDongCode(String adminDongCode);

    List<AdminDongBoundary> findAllByAdminDong_AdminDongCodeIn(Collection<String> adminDongCodes);
}
