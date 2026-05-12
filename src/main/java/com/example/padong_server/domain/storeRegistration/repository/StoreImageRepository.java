package com.example.padong_server.domain.storeRegistration.repository;

import com.example.padong_server.domain.storeRegistration.entity.StoreImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreImageRepository extends JpaRepository<StoreImage, Long> {

    List<StoreImage> findByStoreIdOrderBySortOrderAsc(Long storeId);

    int countByStoreId(Long storeId);
}
