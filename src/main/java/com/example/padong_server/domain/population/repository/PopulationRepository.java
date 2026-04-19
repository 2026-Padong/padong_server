package com.example.padong_server.domain.population.repository;

import com.example.padongbe.domain.dongne.entity.AdminDong;
import com.example.padongbe.domain.population.entity.Population;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PopulationRepository extends JpaRepository<Population, Long> {

    Optional<Population> findByAdminDong(AdminDong adminDong);

    @Query("SELECT MIN(p.density) FROM Population p")
    Double findMinDensity();

    @Query("SELECT MAX(p.density) FROM Population p")
    Double findMaxDensity();
}
