package com.example.padong_server.domain.storeRegistration.repository;

import com.example.padong_server.domain.storeRegistration.entity.Store;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRegistrationRepository
        extends JpaRepository<Store, Long>, JpaSpecificationExecutor<Store> {

    Page<Store> findByOwnerIdOrderByIdDesc(Long ownerId, Pageable pageable);

    /** 전체 가게에서 무작위 size 개. ORDER BY RAND() — 수천 행 규모면 부담 적음. */
    @Query(
            value = "SELECT * FROM store_registrations WHERE deleted_at IS NULL ORDER BY RAND() LIMIT :size",
            nativeQuery = true)
    List<Store> findRandom(@Param("size") int size);
}
