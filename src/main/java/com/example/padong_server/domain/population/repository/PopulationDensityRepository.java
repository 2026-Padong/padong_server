package com.example.padong_server.domain.population.repository;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.population.entity.PopulationDensity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopulationDensityRepository extends JpaRepository<PopulationDensity, Long> {

    Optional<PopulationDensity> findByAdminDong(AdminDong adminDong);

    List<PopulationDensity> findAllByAdminDongIdIn(Collection<Long> adminDongIds);
}
