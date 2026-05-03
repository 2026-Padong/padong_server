package com.example.padong_server.domain.menu.repository;

import com.example.padong_server.domain.menu.entity.Menu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findAllByStoreRegistrationId(Long storeId);
}
