package com.example.padong_server.domain.realtimedata.service;

import com.example.padong_server.domain.realtimedata.entity.RealtimeData;
import com.example.padong_server.domain.realtimedata.repository.RealtimeDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
