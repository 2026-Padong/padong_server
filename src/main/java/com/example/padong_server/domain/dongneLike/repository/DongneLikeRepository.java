package com.example.padong_server.domain.dongneLike.repository;

import com.example.padong_server.domain.dongneLike.entity.DongneLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DongneLikeRepository extends JpaRepository<DongneLike, Long> {

    Optional<DongneLike> findByAdminDongIdAndUserId(Long adminDongId, Long userId);

    boolean existsByAdminDongIdAndUserId(Long adminDongId, Long userId);

    long countByAdminDongId(Long adminDongId);
}
