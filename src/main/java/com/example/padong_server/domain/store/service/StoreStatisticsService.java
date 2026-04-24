package com.example.padong_server.domain.store.service;

import com.example.padong_server.domain.store.entity.StoreStatistics;
import com.example.padong_server.domain.store.repository.StoreStatisticsRepository;
import com.example.padong_server.domain.store.util.StoreStatisticsDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreStatisticsService {

    private final StoreStatisticsRepository storeStatisticsRepository;
    private final StoreStatisticsDataUtil storeStatisticsDataUtil;

    public int uploadStoreStatisticsData() {
        List<StoreStatistics> storeStatistics = storeStatisticsDataUtil.readStoreStatisticsFromCsv();
        storeStatisticsRepository.saveAll(storeStatistics);
        return storeStatistics.size();
    }
}
