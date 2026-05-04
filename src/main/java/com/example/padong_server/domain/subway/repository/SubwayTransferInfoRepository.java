package com.example.padong_server.domain.subway.repository;

import com.example.padong_server.domain.subway.entity.SubwayTransferInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SubwayTransferInfoRepository extends JpaRepository<SubwayTransferInfo, Long> {

    List<SubwayTransferInfo> findByStationNameIn(Collection<String> stationNames);
}
