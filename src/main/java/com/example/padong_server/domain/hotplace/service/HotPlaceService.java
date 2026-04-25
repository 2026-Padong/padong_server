package com.example.padong_server.domain.hotplace.service;

import com.example.padong_server.domain.hotplace.entity.HotPlace;
import com.example.padong_server.domain.hotplace.repository.HotPlaceRepository;
import com.example.padong_server.domain.hotplace.util.HotPlaceDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotPlaceService {

    private final HotPlaceRepository hotPlaceRepository;
    private final HotPlaceDataUtil hotPlaceDataUtil;

    public int uploadHotPlaceData() {
        List<HotPlace> hotPlaces = hotPlaceDataUtil.readHotPlacesFromCsv();
        hotPlaceRepository.saveAll(hotPlaces);
        return hotPlaces.size();
    }
}
