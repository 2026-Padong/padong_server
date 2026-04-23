package com.example.padong_server.domain.rentPrice.repository;

import com.example.padongbe.domain.dongne.entity.LegalDong;
import com.example.padongbe.domain.rentPrice.entity.RentPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RentPriceRepository extends JpaRepository<RentPrice, Long> {
    Optional<RentPrice> findByLegalDongAndBuildingType(LegalDong legalDong, String buildingType);

    @Query("SELECT MIN(r.avgMonthlyRent) FROM RentPrice r")
    Double findMinAvgMonthlyRent();

    @Query("SELECT MAX(r.avgMonthlyRent) FROM RentPrice r")
    Double findMaxAvgMonthlyRent();

    @Query("SELECT MIN(r.avgMonthlyDeposit) FROM RentPrice r")
    Double findMinAvgMonthlyDeposit();

    @Query("SELECT MAX(r.avgMonthlyDeposit) FROM RentPrice r")
    Double findMaxAvgMonthlyDeposit();

    @Query("SELECT MIN(r.avgJeonseDeposit) FROM RentPrice r")
    Double findMinAvgJeonseDeposit();

    @Query("SELECT MAX(r.avgJeonseDeposit) FROM RentPrice r")
    Double findMaxAvgJeonseDeposit();


}
