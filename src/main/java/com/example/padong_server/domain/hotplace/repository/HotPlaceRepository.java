package com.example.padong_server.domain.hotplace.repository;

import com.example.padong_server.domain.hotplace.entity.HotPlace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotPlaceRepository extends JpaRepository<HotPlace, Long> {
}
