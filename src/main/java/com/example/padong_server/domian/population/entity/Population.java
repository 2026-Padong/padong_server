package com.example.padong_server.domian.population.entity;

import com.example.padongbe.domain.dongne.entity.AdminDong;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Population {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdminDong adminDong;
    private double totalPopulation;
    private double density;
}
