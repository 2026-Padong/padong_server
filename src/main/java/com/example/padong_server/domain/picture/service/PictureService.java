package com.example.padong_server.domain.picture.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.picture.dto.AdminDongPictureResponse;
import com.example.padong_server.domain.picture.dto.PictureImportResponse;
import com.example.padong_server.domain.picture.dto.PictureItemResponse;
import com.example.padong_server.domain.picture.entity.TourPicture;
import com.example.padong_server.domain.picture.repository.TourPictureRepository;
import com.example.padong_server.global.client.sk.SkAddress;
import com.example.padong_server.global.client.sk.SkAddressClient;
import com.example.padong_server.global.client.tour.TourApiClient;
import com.example.padong_server.global.client.tour.TourApiQuotaService;
import com.example.padong_server.global.client.tour.TourContent;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PictureService {

    private static final Pattern PARENTHESIZED_DONG_PATTERN = Pattern.compile("\\(([^()]*)\\)");
    private static final Pattern DONG_TOKEN_PATTERN = Pattern.compile("^[가-힣0-9]+동$");

    private final TourApiClient tourApiClient;
    private final TourApiQuotaService tourApiQuotaService;
    private final SkAddressClient skAddressClient;
    private final DongneService dongneService;
    private final AdminDongRepository adminDongRepository;
    private final TourPictureRepository tourPictureRepository;

    @Transactional
    public PictureImportResponse importPictures() {
        LocalDate today = LocalDate.now();
        int remainingTourApiCalls = tourApiQuotaService.getRemainingCalls(today);
        if (remainingTourApiCalls <= 0) {
            return new PictureImportResponse(0, 0, 0, 0, 0, 0, tourApiQuotaService.getUsedCalls(today), 0);
        }

        TourApiClient.TourContentPageResult pageResult = tourApiClient.getSeoulContents(remainingTourApiCalls);
        tourApiQuotaService.consumeCalls(today, pageResult.requestCount());

        List<TourPicture> picturesToSave = new ArrayList<>();
        Map<String, Optional<ResolvedAdminDong>> resolvedAdminDongCache = new HashMap<>();
        int skippedNoImageCount = 0;
        int skippedUnresolvedCount = 0;
        int resolvedByParenthesisCount = 0;
        int resolvedByAddressApiCount = 0;

        for (TourContent content : pageResult.contents()) {
            if (!StringUtils.hasText(content.firstImage())) {
                skippedNoImageCount++;
                continue;
            }

            ResolvedAdminDong resolvedAdminDong = resolvedAdminDongCache.computeIfAbsent(
                    content.address(),
                    address -> Optional.ofNullable(resolveAdminDong(address))
            ).orElse(null);

            if (resolvedAdminDong == null) {
                skippedUnresolvedCount++;
                continue;
            }

            if (resolvedAdminDong.byAddressApi()) {
                resolvedByAddressApiCount++;
            } else {
                resolvedByParenthesisCount++;
            }

            picturesToSave.add(TourPicture.builder()
                    .contentId(content.contentId())
                    .title(content.title())
                    .roadAddress(content.address())
                    .firstImageUrl(content.firstImage())
                    .adminDongName(resolvedAdminDong.adminDongName())
                    .adminDongCode(resolvedAdminDong.adminDongCode())
                    .build());
        }

        tourPictureRepository.deleteAllInBatch();
        tourPictureRepository.saveAll(picturesToSave);

        int usedCalls = tourApiQuotaService.getUsedCalls(today);
        return new PictureImportResponse(
                pageResult.contents().size(),
                picturesToSave.size(),
                skippedNoImageCount,
                skippedUnresolvedCount,
                resolvedByParenthesisCount,
                resolvedByAddressApiCount,
                usedCalls,
                Math.max(0, TourApiQuotaService.DAILY_LIMIT - usedCalls)
        );
    }

    @Transactional(readOnly = true)
    public AdminDongPictureResponse getPicturesByAdminDongCode(String adminDongCode) {
        AdminDong adminDong = dongneService.findAdminDongByCode(adminDongCode);
        List<TourPicture> pictures = tourPictureRepository.findByAdminDongCodeOrderByTitleAscContentIdAsc(adminDongCode);

        if (pictures.isEmpty()) {
            throw new CustomException(ErrorCode.PICTURE_NOT_FOUND);
        }

        return AdminDongPictureResponse.builder()
                .adminDongCode(adminDongCode)
                .adminDongName(adminDong.getAdminDongName())
                .pictures(pictures.stream().map(this::toResponse).toList())
                .build();
    }

    private ResolvedAdminDong resolveAdminDong(String roadAddress) {
        String dongToken = extractParenthesizedDong(roadAddress);
        if (StringUtils.hasText(dongToken)) {
            AdminDong adminDong = findAdminDongByName(dongToken);
            if (adminDong != null) {
                return new ResolvedAdminDong(adminDong.getAdminDongName(), adminDong.getAdminDongCode(), false);
            }
        }

        SkAddress skAddress;
        try {
            skAddress = skAddressClient.resolveRoadAddress(roadAddress);
        } catch (CustomException exception) {
            return null;
        }

        if (skAddress == null || !StringUtils.hasText(skAddress.jibunAddress())) {
            return null;
        }

        if (StringUtils.hasText(skAddress.adminDongCode())) {
            AdminDong adminDong = adminDongRepository.findByAdminDongCode(skAddress.adminDongCode()).orElse(null);
            if (adminDong != null) {
                return new ResolvedAdminDong(adminDong.getAdminDongName(), adminDong.getAdminDongCode(), true);
            }
        }

        if (StringUtils.hasText(skAddress.adminDongName())) {
            AdminDong adminDong = findAdminDongByName(skAddress.adminDongName());
            if (adminDong != null) {
                return new ResolvedAdminDong(adminDong.getAdminDongName(), adminDong.getAdminDongCode(), true);
            }
        }

        String extractedDong = extractDongToken(skAddress.jibunAddress());
        if (!StringUtils.hasText(extractedDong)) {
            return null;
        }

        AdminDong adminDong = findAdminDongByName(extractedDong);
        if (adminDong == null) {
            return null;
        }

        return new ResolvedAdminDong(adminDong.getAdminDongName(), adminDong.getAdminDongCode(), true);
    }

    private String extractParenthesizedDong(String roadAddress) {
        if (!StringUtils.hasText(roadAddress)) {
            return null;
        }

        Matcher matcher = PARENTHESIZED_DONG_PATTERN.matcher(roadAddress);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1).trim();
    }

    private String extractDongToken(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }

        String[] tokens = address.trim().split("\\s+");
        for (String token : tokens) {
            if (DONG_TOKEN_PATTERN.matcher(token).matches()) {
                return token;
            }
        }

        return null;
    }

    private AdminDong findAdminDongByName(String dongName) {
        Optional<AdminDong> adminDong = adminDongRepository.findFirstByAdminDongNameContainingOrderByIdAsc(dongName);
        return adminDong.orElse(null);
    }

    private PictureItemResponse toResponse(TourPicture picture) {
        return PictureItemResponse.builder()
                .contentId(picture.getContentId())
                .title(picture.getTitle())
                .roadAddress(picture.getRoadAddress())
                .firstImageUrl(picture.getFirstImageUrl())
                .adminDongCode(picture.getAdminDongCode())
                .adminDongName(picture.getAdminDongName())
                .build();
    }

    private record ResolvedAdminDong(
            String adminDongName,
            String adminDongCode,
            boolean byAddressApi
    ) {
    }
}
