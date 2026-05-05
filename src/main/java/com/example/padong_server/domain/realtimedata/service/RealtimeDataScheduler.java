package com.example.padong_server.domain.realtimedata.service;

import com.example.padong_server.domain.hotplace.entity.HotPlace;
import com.example.padong_server.domain.hotplace.repository.HotPlaceRepository;
import com.example.padong_server.domain.realtimedata.entity.RealtimeData;
import com.example.padong_server.global.client.seoul.SeoulRealtimeClient;
import com.example.padong_server.global.client.seoul.SeoulRealtimeData;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeDataScheduler {

    private final HotPlaceRepository hotPlaceRepository;
    private final SeoulRealtimeClient seoulRealtimeClient;
    private final RealtimeDataService realtimeDataService;

    @Scheduled(cron = "0 */5 * * * *")
    public void collectRealtimeData() {
        List<HotPlace> hotPlaces = hotPlaceRepository.findAll();
        if (hotPlaces.isEmpty()) {
            log.info("Skipping realtime data collection because no hot places are registered.");
            return;
        }

        LocalDateTime measuredAt = LocalDateTime.now();
        Map<String, HotPlace> distinctHotPlaces = new LinkedHashMap<>();
        for (HotPlace hotPlace : hotPlaces) {
            distinctHotPlaces.putIfAbsent(hotPlace.getAreaNm(), hotPlace);
        }

        List<RealtimeData> realtimeDataList = distinctHotPlaces.values().stream()
                .map(hotPlace -> fetchRealtimeData(hotPlace, measuredAt))
                .filter(java.util.Objects::nonNull)
                .toList();

        if (realtimeDataList.isEmpty()) {
            log.warn("Realtime data collection finished with no successful records.");
            return;
        }

        realtimeDataService.saveAll(realtimeDataList);
        log.info("Saved {} realtime data records at {}", realtimeDataList.size(), measuredAt);
    }

    private RealtimeData fetchRealtimeData(HotPlace hotPlace, LocalDateTime measuredAt) {
        try {
            SeoulRealtimeData realtimeData = seoulRealtimeClient.getRealtimeDataByAreaNm(hotPlace.getAreaNm());
            return realtimeDataService.toEntity(hotPlace, realtimeData, measuredAt);
        } catch (Exception exception) {
            log.warn("Failed to collect realtime data for areaNm={}", hotPlace.getAreaNm(), exception);
            return null;
        }
    }
}
