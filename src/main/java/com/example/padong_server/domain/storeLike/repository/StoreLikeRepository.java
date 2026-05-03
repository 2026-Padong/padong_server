package com.example.padong_server.domain.storeLike.repository;

import com.example.padong_server.domain.storeLike.entity.StoreLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreLikeRepository extends JpaRepository<StoreLike, Long> {

    Optional<StoreLike> findByStoreRegistrationIdAndUserId(Long storeId, Long userId);

    boolean existsByStoreRegistrationIdAndUserId(Long storeId, Long userId);

    long countByStoreRegistrationId(Long storeId);
}
