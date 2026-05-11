package com.example.padong_server.domain.storeRegistration.repository;

import com.example.padong_server.domain.storeRegistration.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRegistrationRepository extends JpaRepository<Store, Long> {
}
