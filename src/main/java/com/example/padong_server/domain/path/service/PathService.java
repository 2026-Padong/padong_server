package com.example.padong_server.domain.path.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.path.dto.internal.PathSummary;
import com.example.padong_server.domain.path.dto.request.CarPathRequest;
import com.example.padong_server.domain.path.dto.request.PathAllRequest;
import com.example.padong_server.domain.path.dto.request.PedestrianPathRequest;
import com.example.padong_server.domain.path.dto.request.TransitPathRequest;
import com.example.padong_server.domain.path.dto.response.CarPathResponse;
import com.example.padong_server.domain.path.dto.response.PathAllResponse;
import com.example.padong_server.domain.path.dto.response.PedestrianPathResponse;
import com.example.padong_server.domain.path.dto.response.TransitPathResponse;
import com.example.padong_server.domain.path.entity.PathMode;
import com.example.padong_server.domain.path.entity.PathProvider;
import com.example.padong_server.domain.path.repository.PathRecordRepository;
import com.example.padong_server.global.client.google.GoogleRoutesClient;
import com.example.padong_server.global.client.odsay.OdsayClient;
import com.example.padong_server.global.client.sk.SkCarRouteClient;
import com.example.padong_server.global.client.sk.SkPedestrianRouteClient;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class PathService {

    private static final Duration EXPIRATION = Duration.ofDays(1);

    private final AdminDongRepository adminDongRepository;
    private final OdsayClient odsayClient;
    private final SkPedestrianRouteClient skPedestrianRouteClient;
    private final SkCarRouteClient skCarRouteClient;
    private final GoogleRoutesClient googleRoutesClient;
    private final PathRecordRepository pathRecordRepository;

    @Transactional
    public TransitPathResponse searchTransit(TransitPathRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        PathSummary summary = transitSummary(dongs, request.getOpt(), request.getSearchPathType());
        return TransitPathResponse.of(dongs.departure(), dongs.arrival(), summary);
    }

    @Transactional
    public PedestrianPathResponse searchPedestrian(PedestrianPathRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        PathSummary summary = pedestrianSummary(dongs);
        return PedestrianPathResponse.of(dongs.departure(), dongs.arrival(), summary);
    }

    @Transactional
    public CarPathResponse searchCar(CarPathRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        PathSummary summary = carSummary(dongs);
        return CarPathResponse.of(dongs.departure(), dongs.arrival(), summary);
    }

    @Transactional
    public PathAllResponse searchAll(PathAllRequest request) {
        DongPair dongs = lookup(request.getDepartureDongCode(), request.getArrivalDongCode());
        PathSummary transit = transitSummary(dongs, null, null);
        PathSummary pedestrian = pedestrianSummary(dongs);
        PathSummary car = carSummary(dongs);
        return PathAllResponse.of(dongs.departure(), dongs.arrival(), transit, pedestrian, car);
    }

    private PathSummary transitSummary(DongPair dongs, Integer opt, Integer searchPathType) {
        return summaryFor(
                PathMode.TRANSIT,
                dongs,
                () -> withGoogleFallback(
                        PathProvider.ODSAY,
                        () -> TransitPathResponse.parseSummary(
                                odsayClient.searchPubTransPath(
                                        dongs.departure().getLongitude(),
                                        dongs.departure().getLatitude(),
                                        dongs.arrival().getLongitude(),
                                        dongs.arrival().getLatitude(),
                                        opt,
                                        searchPathType)),
                        GoogleRoutesClient.TravelMode.TRANSIT,
                        dongs));
    }

    private PathSummary pedestrianSummary(DongPair dongs) {
        return summaryFor(
                PathMode.PEDESTRIAN,
                dongs,
                () -> withGoogleFallback(
                        PathProvider.SK_PEDESTRIAN,
                        () -> PedestrianPathResponse.parseSummary(
                                skPedestrianRouteClient.route(
                                        dongs.departure().getAdminDongName(),
                                        dongs.departure().getLongitude(),
                                        dongs.departure().getLatitude(),
                                        dongs.arrival().getAdminDongName(),
                                        dongs.arrival().getLongitude(),
                                        dongs.arrival().getLatitude())),
                        GoogleRoutesClient.TravelMode.WALK,
                        dongs));
    }

    private PathSummary carSummary(DongPair dongs) {
        return summaryFor(
                PathMode.CAR,
                dongs,
                () -> withGoogleFallback(
                        PathProvider.SK_CAR,
                        () -> CarPathResponse.parseSummary(
                                skCarRouteClient.route(
                                        dongs.departure().getAdminDongName(),
                                        dongs.departure().getLongitude(),
                                        dongs.departure().getLatitude(),
                                        dongs.arrival().getAdminDongName(),
                                        dongs.arrival().getLongitude(),
                                        dongs.arrival().getLatitude())),
                        GoogleRoutesClient.TravelMode.DRIVE,
                        dongs));
    }

    private PathSummary withGoogleFallback(
            PathProvider primaryProvider,
            Supplier<PathSummary> primary,
            GoogleRoutesClient.TravelMode googleMode,
            DongPair dongs) {
        try {
            return primary.get();
        } catch (CustomException exception) {
            if (exception.getErrorCode().getPathProvider() == primaryProvider) {
                return callGoogleFallback(googleMode, dongs);
            }
            throw exception;
        } catch (RuntimeException exception) {
            // 외부 클라이언트 / 파서의 비-CustomException (RestClientException, NPE 등) 을 도메인 예외로 정규화.
            // cause 는 유지되어 상위 핸들러에서 스택 확인 가능.
            throw new CustomException(ErrorCode.PATH_EXTERNAL_FAILED, exception);
        }
    }

    private PathSummary callGoogleFallback(
            GoogleRoutesClient.TravelMode googleMode, DongPair dongs) {
        try {
            return googleRoutesClient.route(
                    googleMode,
                    dongs.departure().getLatitude(),
                    dongs.departure().getLongitude(),
                    dongs.arrival().getLatitude(),
                    dongs.arrival().getLongitude());
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.PATH_EXTERNAL_FAILED, exception);
        }
    }

    private PathSummary summaryFor(PathMode mode, DongPair dongs, Supplier<PathSummary> fetcher) {
        return findFresh(mode, dongs.depCode(), dongs.arrCode())
                .orElseGet(() -> {
                    PathSummary fresh = fetcher.get();
                    // 캐시 upsert 는 best-effort. readOnly 트랜잭션 내 호출 등으로 실패해도
                    // API 응답 자체는 외부에서 받아온 값으로 정상 반환.
                    try {
                        upsert(mode, dongs.depCode(), dongs.arrCode(), fresh);
                    } catch (RuntimeException e) {
                        log.warn(
                                "path_record 캐시 저장 실패 — best-effort, 응답은 계속. mode={}, dep={}, arr={}, ex={}",
                                mode,
                                dongs.depCode(),
                                dongs.arrCode(),
                                e.getClass().getSimpleName(),
                                e);
                    }
                    return fresh;
                });
    }

    private Optional<PathSummary> findFresh(
            PathMode mode, String departureDongCode, String arrivalDongCode) {
        LocalDateTime threshold = LocalDateTime.now().minus(EXPIRATION);
        return pathRecordRepository
                .findByModeAndDepartureDongCodeAndArrivalDongCode(
                        mode, departureDongCode, arrivalDongCode)
                .filter(record -> record.getUpdatedAt().isAfter(threshold))
                .map(record -> new PathSummary(
                        record.getTotalTime(), record.getTotalDistance(), record.getSource()));
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
                summary.source().name(),
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
