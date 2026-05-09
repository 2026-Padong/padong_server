package com.example.padong_server.domain.path.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.path.dto.internal.PathSummary;
import com.example.padong_server.domain.path.dto.request.CarPathRequest;
import com.example.padong_server.domain.path.dto.request.PedestrianPathRequest;
import com.example.padong_server.domain.path.dto.request.TransitPathRequest;
import com.example.padong_server.domain.path.dto.response.CarPathResponse;
import com.example.padong_server.domain.path.dto.response.PedestrianPathResponse;
import com.example.padong_server.domain.path.dto.response.TransitPathResponse;
import com.example.padong_server.domain.path.entity.PathMode;
import com.example.padong_server.domain.path.repository.PathRecordRepository;
import com.example.padong_server.global.client.odsay.OdsayClient;
import com.example.padong_server.global.client.sk.SkCarRouteClient;
import com.example.padong_server.global.client.sk.SkPedestrianRouteClient;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PathService {

    private static final Duration EXPIRATION = Duration.ofDays(1);

    private final AdminDongRepository adminDongRepository;
    private final OdsayClient odsayClient;
    private final SkPedestrianRouteClient skPedestrianRouteClient;
    private final SkCarRouteClient skCarRouteClient;
    private final PathRecordRepository pathRecordRepository;

    @Transactional
    public TransitPathResponse searchTransit(TransitPathRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        PathSummary summary =
                findFresh(PathMode.TRANSIT, dongs.depCode(), dongs.arrCode())
                        .orElseGet(() -> {
                            PathSummary fresh =
                                    TransitPathResponse.parseSummary(
                                            odsayClient.searchPubTransPath(
                                                    dongs.departure().getLongitude(),
                                                    dongs.departure().getLatitude(),
                                                    dongs.arrival().getLongitude(),
                                                    dongs.arrival().getLatitude(),
                                                    request.getOpt(),
                                                    request.getSearchPathType()));
                            upsert(PathMode.TRANSIT, dongs.depCode(), dongs.arrCode(), fresh);
                            return fresh;
                        });
        return TransitPathResponse.of(dongs.departure(), dongs.arrival(), summary);
    }

    @Transactional
    public PedestrianPathResponse searchPedestrian(PedestrianPathRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        PathSummary summary =
                findFresh(PathMode.PEDESTRIAN, dongs.depCode(), dongs.arrCode())
                        .orElseGet(() -> {
                            PathSummary fresh =
                                    PedestrianPathResponse.parseSummary(
                                            skPedestrianRouteClient.route(
                                                    dongs.departure().getAdminDongName(),
                                                    dongs.departure().getLongitude(),
                                                    dongs.departure().getLatitude(),
                                                    dongs.arrival().getAdminDongName(),
                                                    dongs.arrival().getLongitude(),
                                                    dongs.arrival().getLatitude()));
                            upsert(PathMode.PEDESTRIAN, dongs.depCode(), dongs.arrCode(), fresh);
                            return fresh;
                        });
        return PedestrianPathResponse.of(dongs.departure(), dongs.arrival(), summary);
    }

    @Transactional
    public CarPathResponse searchCar(CarPathRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        PathSummary summary =
                findFresh(PathMode.CAR, dongs.depCode(), dongs.arrCode())
                        .orElseGet(() -> {
                            PathSummary fresh =
                                    CarPathResponse.parseSummary(
                                            skCarRouteClient.route(
                                                    dongs.departure().getAdminDongName(),
                                                    dongs.departure().getLongitude(),
                                                    dongs.departure().getLatitude(),
                                                    dongs.arrival().getAdminDongName(),
                                                    dongs.arrival().getLongitude(),
                                                    dongs.arrival().getLatitude()));
                            upsert(PathMode.CAR, dongs.depCode(), dongs.arrCode(), fresh);
                            return fresh;
                        });
        return CarPathResponse.of(dongs.departure(), dongs.arrival(), summary);
    }

    private Optional<PathSummary> findFresh(
            PathMode mode, String departureDongCode, String arrivalDongCode) {
        LocalDateTime threshold = LocalDateTime.now().minus(EXPIRATION);
        return pathRecordRepository
                .findByModeAndDepartureDongCodeAndArrivalDongCode(
                        mode, departureDongCode, arrivalDongCode)
                .filter(record -> record.getUpdatedAt().isAfter(threshold))
                .map(record -> new PathSummary(record.getTotalTime(), record.getTotalDistance()));
    }

    private void upsert(
            PathMode mode,
            String departureDongCode,
            String arrivalDongCode,
            PathSummary summary) {
        pathRecordRepository.upsert(
                mode.name(),
                departureDongCode,
                arrivalDongCode,
                summary.totalTime(),
                summary.totalDistance(),
                LocalDateTime.now());
    }

    private DongPair lookup(String departureDongCode, String arrivalDongCode) {
        String departureCode = departureDongCode.trim();
        String arrivalCode = arrivalDongCode.trim();
        Preconditions.validate(
                !departureCode.equals(arrivalCode), ErrorCode.TRANSIT_PATH_SAME_DONG);
        AdminDong departureDong = adminDongRepository.getByAdminDongCode(departureCode);
        AdminDong arrivalDong = adminDongRepository.getByAdminDongCode(arrivalCode);
        return new DongPair(departureDong, arrivalDong, departureCode, arrivalCode);
    }

    private record DongPair(
            AdminDong departure, AdminDong arrival, String depCode, String arrCode) {}
}
