package com.example.padong_server.domain.path.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.path.dto.request.CarPathRequest;
import com.example.padong_server.domain.path.dto.request.PedestrianPathRequest;
import com.example.padong_server.domain.path.dto.request.TransitPathRequest;
import com.example.padong_server.domain.path.dto.response.CarPathResponse;
import com.example.padong_server.domain.path.dto.response.PedestrianPathResponse;
import com.example.padong_server.domain.path.dto.response.TransitPathResponse;
import com.example.padong_server.global.client.odsay.OdsayClient;
import com.example.padong_server.global.client.sk.SkCarRouteClient;
import com.example.padong_server.global.client.sk.SkPedestrianRouteClient;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PathService {

    private final AdminDongRepository adminDongRepository;
    private final OdsayClient odsayClient;
    private final SkPedestrianRouteClient skPedestrianRouteClient;
    private final SkCarRouteClient skCarRouteClient;

    public TransitPathResponse searchTransit(TransitPathRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        Map<String, Object> raw = odsayClient.searchPubTransPath(
                dongs.departure().getLongitude(),
                dongs.departure().getLatitude(),
                dongs.arrival().getLongitude(),
                dongs.arrival().getLatitude(),
                request.getOpt(),
                request.getSearchPathType());
        return TransitPathResponse.from(dongs.departure(), dongs.arrival(), raw);
    }

    public PedestrianPathResponse searchPedestrian(PedestrianPathRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        Map<String, Object> raw = skPedestrianRouteClient.route(
                dongs.departure().getAdminDongName(),
                dongs.departure().getLongitude(),
                dongs.departure().getLatitude(),
                dongs.arrival().getAdminDongName(),
                dongs.arrival().getLongitude(),
                dongs.arrival().getLatitude());
        return PedestrianPathResponse.from(dongs.departure(), dongs.arrival(), raw);
    }

    public CarPathResponse searchCar(CarPathRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        Map<String, Object> raw = skCarRouteClient.route(
                dongs.departure().getAdminDongName(),
                dongs.departure().getLongitude(),
                dongs.departure().getLatitude(),
                dongs.arrival().getAdminDongName(),
                dongs.arrival().getLongitude(),
                dongs.arrival().getLatitude());
        return CarPathResponse.from(dongs.departure(), dongs.arrival(), raw);
    }

    private DongPair lookup(String departureDongCode, String arrivalDongCode) {
        String departureCode = departureDongCode.trim();
        String arrivalCode = arrivalDongCode.trim();
        Preconditions.validate(
                !departureCode.equals(arrivalCode), ErrorCode.TRANSIT_PATH_SAME_DONG);
        AdminDong departureDong = adminDongRepository.getByAdminDongCode(departureCode);
        AdminDong arrivalDong = adminDongRepository.getByAdminDongCode(arrivalCode);
        return new DongPair(departureDong, arrivalDong);
    }

    private record DongPair(AdminDong departure, AdminDong arrival) {}
}
