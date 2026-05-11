package com.example.padong_server.domain.storeRegistration.repository;

import com.example.padong_server.domain.storeRegistration.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRegistrationRepository extends JpaRepository<Store, Long> {

    Page<Store> findByOwnerIdOrderByIdDesc(Long ownerId, Pageable pageable);
}
