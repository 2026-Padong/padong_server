package com.example.padong_server.domain.store.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "store_statistics",
        indexes = {
                @Index(name = "idx_store_statistics_admin_dong", columnList = "adminDongCode"),
                @Index(name = "idx_store_statistics_service_category", columnList = "serviceCategoryCode")
        }
)
public class StoreStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String baseYearQuarterCode;
    private String adminDongCode;
    private String adminDongName;
    private String serviceCategoryCode;
    private String serviceCategoryName;
    private Integer storeCount;
    private Integer similarStoreCount;
    private Integer franchiseStoreCount;
}
