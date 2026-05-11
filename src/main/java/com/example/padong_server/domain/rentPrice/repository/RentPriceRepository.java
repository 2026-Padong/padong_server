package com.example.padong_server.domain.rentPrice.repository;

import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentPriceRepository extends JpaRepository<RentPrice, Long> {

    List<RentPrice> findAllByAdminDongAdminDongCode(String adminDongCode);

    List<RentPrice> findAllByAdminDongAdminDongCodeIn(Collection<String> adminDongCodes);
}
