package com.example.padong_server.domain.rentPrice.repository;

import com.example.padong_server.domain.dongne.entity.LegalDong;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentPriceRepository extends JpaRepository<RentPrice, Long> {

    Optional<RentPrice> findByLegalDongAndBuildingType(
            LegalDong legalDong,
            String buildingType
    );

    List<RentPrice> findAllByLegalDong(LegalDong legalDong);
}
