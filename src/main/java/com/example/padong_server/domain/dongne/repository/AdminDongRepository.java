package com.example.padong_server.domain.dongne.repository;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdminDongRepository extends JpaRepository<AdminDong, Long> {

    String ADMIN_DONG_CODE_REQUIRED_MESSAGE = "행정동 코드는 비어 있을 수 없습니다.";
    String ADMIN_DONG_NOT_FOUND_MESSAGE_FORMAT = "존재하지 않는 행정동 코드입니다: %s";

    Optional<AdminDong> findByAdminDongCode(String adminDongCode);

    default AdminDong getByAdminDongCode(String adminDongCode) {
        Preconditions.validate(
                adminDongCode != null && !adminDongCode.trim().isEmpty(),
                ErrorCode.VALIDATION_ERROR);
        String sanitizedAdminDongCode = adminDongCode.trim();
        return findByAdminDongCode(sanitizedAdminDongCode)
                .orElseThrow(
                        () ->
                                new CustomException(
                                        ErrorCode.VALIDATION_ERROR,
                                        ADMIN_DONG_NOT_FOUND_MESSAGE_FORMAT.formatted(
                                                sanitizedAdminDongCode)));
    }

    List<AdminDong> findAllByAdminDongCodeIn(Collection<String> adminDongCodes);

    Optional<AdminDong> findByCityNameAndDistrictNameAndAdminDongName(
            String cityName, String districtName, String adminDongName);

    Optional<AdminDong> findFirstByAdminDongNameContainingOrderByIdAsc(String adminDongName);
}
