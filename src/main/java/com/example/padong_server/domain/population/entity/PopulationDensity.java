package com.example.padong_server.domain.population.entity;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "population_density",
        indexes = {
                @Index(name = "idx_population_density_admin_dong", columnList = "admin_dong_id")
        }
)
public class PopulationDensity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_dong_id", nullable = false, unique = true)
    private AdminDong adminDong;

    @Column(nullable = false)
    private String cityName;

    @Column(nullable = false)
    private String districtName;

    @Column(nullable = false)
    private String adminDongName;

    @Column(nullable = false)
    private double totalPopulation;

    @Column(nullable = false)
    private double areaSize;

    @Column(nullable = false)
    private double density;

    @Column(nullable = false)
    private double soccerFieldPopulation;
}
