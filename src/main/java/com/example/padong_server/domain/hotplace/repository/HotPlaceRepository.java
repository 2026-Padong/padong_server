package com.example.padong_server.domain.hotplace.repository;

import com.example.padong_server.domain.hotplace.entity.HotPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HotPlaceRepository extends JpaRepository<HotPlace, Long> {

    List<HotPlace> findByGuName(String guName);

    Optional<HotPlace> findByAreaNm(String areaNm);
}
