package com.example.padong_server.domain.transitPath.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.transitPath.dto.TransitPathRequest;
import com.example.padong_server.domain.transitPath.dto.TransitPathResponse;
import com.example.padong_server.global.client.odsay.OdsayClient;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransitPathService {

    private final AdminDongRepository adminDongRepository;
    private final OdsayClient odsayClient;

    public TransitPathResponse search(TransitPathRequest request) {
        String departureCode = request.getDepartureDongCode().trim();
        String arrivalCode = request.getArrivalDongCode().trim();
        Preconditions.validate(
                !departureCode.equals(arrivalCode), ErrorCode.TRANSIT_PATH_SAME_DONG);

        AdminDong departureDong = adminDongRepository.getByAdminDongCode(departureCode);
        AdminDong arrivalDong = adminDongRepository.getByAdminDongCode(arrivalCode);

        Map<String, Object> raw = odsayClient.searchPubTransPath(
                departureDong.getLongitude(),
                departureDong.getLatitude(),
                arrivalDong.getLongitude(),
                arrivalDong.getLatitude(),
                request.getOpt(),
                request.getSearchPathType());

        return TransitPathResponse.from(departureDong, arrivalDong, raw);
    }
}
