package com.example.padong_server.domain.transitPath.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.transitPath.dto.request.PedestrianPathRequest;
import com.example.padong_server.domain.transitPath.dto.response.PedestrianPathResponse;
import com.example.padong_server.global.client.sk.SkPedestrianRouteClient;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PedestrianPathService {

    private final AdminDongRepository adminDongRepository;
    private final SkPedestrianRouteClient skPedestrianRouteClient;

    public PedestrianPathResponse search(PedestrianPathRequest request) {
        String departureCode = request.getDepartureDongCode().trim();
        String arrivalCode = request.getArrivalDongCode().trim();
        Preconditions.validate(
                !departureCode.equals(arrivalCode), ErrorCode.TRANSIT_PATH_SAME_DONG);

        AdminDong departureDong = adminDongRepository.getByAdminDongCode(departureCode);
        AdminDong arrivalDong = adminDongRepository.getByAdminDongCode(arrivalCode);

        Map<String, Object> raw = skPedestrianRouteClient.route(
                departureDong.getAdminDongName(),
                departureDong.getLongitude(),
                departureDong.getLatitude(),
                arrivalDong.getAdminDongName(),
                arrivalDong.getLongitude(),
                arrivalDong.getLatitude());

        return PedestrianPathResponse.from(departureDong, arrivalDong, raw);
    }
}
