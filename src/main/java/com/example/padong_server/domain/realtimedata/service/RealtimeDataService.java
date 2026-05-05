package com.example.padong_server.domain.realtimedata.service;

import com.example.padong_server.domain.hotplace.entity.HotPlace;
import com.example.padong_server.domain.realtimedata.entity.RealtimeData;
import com.example.padong_server.domain.realtimedata.repository.RealtimeDataRepository;
import com.example.padong_server.global.client.seoul.SeoulRealtimeData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeDataService {

    private final RealtimeDataRepository realtimeDataRepository;

    @Transactional
    public RealtimeData save(RealtimeData realtimeData) {
        return realtimeDataRepository.save(realtimeData);
    }

    @Transactional
    public List<RealtimeData> saveAll(List<RealtimeData> realtimeDataList) {
        return realtimeDataRepository.saveAll(realtimeDataList);
    }

    public RealtimeData toEntity(HotPlace hotPlace, SeoulRealtimeData realtimeData, LocalDateTime measuredAt) {
        return RealtimeData.builder()
                .areaCode(realtimeData.getAreaCd())
                .areaName(realtimeData.getAreaNm())
                .hotspotName(hotPlace.getAreaNm())
                .areaCongestLevel(realtimeData.getAreaCongestLvl())
                .areaCongestMessage(realtimeData.getAreaCongestMsg())
                .weatherStatus(realtimeData.getWeatherStatus())
                .temperature(realtimeData.getTemperature())
                .pm10(realtimeData.getPm10())
                .rainChance(realtimeData.getRainChance())
                .measuredAt(measuredAt)
                .build();
    }
}
