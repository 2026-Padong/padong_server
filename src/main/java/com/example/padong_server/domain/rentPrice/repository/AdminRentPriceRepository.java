package com.example.padong_server.domain.rentPrice.repository;

import com.example.padong_server.domain.rentPrice.entity.AdminRentPrice;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRentPriceRepository extends JpaRepository<AdminRentPrice, Long> {

    List<AdminRentPrice> findAllByAdminDongAdminDongCode(String adminDongCode);

    List<AdminRentPrice> findAllByAdminDongAdminDongCodeIn(Collection<String> adminDongCodes);
}
