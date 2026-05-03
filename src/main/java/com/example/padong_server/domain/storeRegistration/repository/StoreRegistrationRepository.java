package com.example.padong_server.domain.storeRegistration.repository;

import com.example.padong_server.domain.storeRegistration.entity.StoreRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRegistrationRepository extends JpaRepository<StoreRegistration, Long> {
}
