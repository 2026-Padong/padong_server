package com.example.padong_server.domain.dongne.repository;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminDongRepository extends JpaRepository<AdminDong, Long> {

    Optional<AdminDong> findByAdminDongCode(String adminDongCode);
}
