package com.example.padong_server.domain.menu.service;

import com.example.padong_server.domain.menu.dto.MenuCreateRequest;
import com.example.padong_server.domain.menu.dto.MenuResponse;
import com.example.padong_server.domain.menu.dto.MenuUpdateRequest;
import com.example.padong_server.domain.menu.entity.Menu;
import com.example.padong_server.domain.menu.repository.MenuRepository;
import com.example.padong_server.domain.storeRegistration.entity.StoreRegistration;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final StoreRegistrationRepository storeRegistrationRepository;

    @Transactional
    public MenuResponse createMenu(MenuCreateRequest request) {
        validate(
                request.menuInfo(),
                request.originalPrice(),
                request.discountPrice(),
                request.pickupAvailableTime(),
                request.maxParticipants(),
                request.recruitmentDeadline(),
                request.paymentMethod(),
                request.currentParticipants()
        );

        StoreRegistration storeRegistration = findStore(request.storeId());
        Menu menu = Menu.builder()
                .storeRegistration(storeRegistration)
                .menuInfo(request.menuInfo().trim())
                .originalPrice(request.originalPrice())
                .discountPrice(request.discountPrice())
                .pickupAvailableTime(request.pickupAvailableTime().trim())
                .maxParticipants(request.maxParticipants())
                .recruitmentDeadline(request.recruitmentDeadline().trim())
                .paymentMethod(request.paymentMethod().trim())
                .currentParticipants(request.currentParticipants())
                .build();

        return toResponse(menuRepository.save(menu));
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> getMenus(Long storeId) {
        findStore(storeId);
        return menuRepository.findAllByStoreRegistrationId(storeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MenuResponse updateMenu(Long menuId, MenuUpdateRequest request) {
        validate(
                request.menuInfo(),
                request.originalPrice(),
                request.discountPrice(),
                request.pickupAvailableTime(),
                request.maxParticipants(),
                request.recruitmentDeadline(),
                request.paymentMethod(),
                request.currentParticipants()
        );

        Menu menu = findMenu(menuId);
        menu.update(
                request.menuInfo().trim(),
                request.originalPrice(),
                request.discountPrice(),
                request.pickupAvailableTime().trim(),
                request.maxParticipants(),
                request.recruitmentDeadline().trim(),
                request.paymentMethod().trim(),
                request.currentParticipants()
        );

        return toResponse(menu);
    }

    @Transactional
    public void deleteMenu(Long menuId) {
        menuRepository.delete(findMenu(menuId));
    }

    private StoreRegistration findStore(Long storeId) {
        if (storeId == null) {
            throw new CustomException(ErrorCode.INVALID_MENU_REQUEST);
        }

        return storeRegistrationRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    private Menu findMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
    }

    private void validate(
            String menuInfo,
            Integer originalPrice,
            Integer discountPrice,
            String pickupAvailableTime,
            Integer maxParticipants,
            String recruitmentDeadline,
            String paymentMethod,
            Integer currentParticipants
    ) {
        if (!StringUtils.hasText(menuInfo)
                || originalPrice == null
                || discountPrice == null
                || !StringUtils.hasText(pickupAvailableTime)
                || maxParticipants == null
                || !StringUtils.hasText(recruitmentDeadline)
                || !StringUtils.hasText(paymentMethod)
                || currentParticipants == null) {
            throw new CustomException(ErrorCode.INVALID_MENU_REQUEST);
        }

        if (originalPrice < 0
                || discountPrice < 0
                || discountPrice > originalPrice
                || maxParticipants < 1
                || currentParticipants < 0
                || currentParticipants > maxParticipants) {
            throw new CustomException(ErrorCode.INVALID_MENU_REQUEST);
        }
    }

    private MenuResponse toResponse(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .storeId(menu.getStoreRegistration().getId())
                .menuInfo(menu.getMenuInfo())
                .originalPrice(menu.getOriginalPrice())
                .discountPrice(menu.getDiscountPrice())
                .pickupAvailableTime(menu.getPickupAvailableTime())
                .maxParticipants(menu.getMaxParticipants())
                .recruitmentDeadline(menu.getRecruitmentDeadline())
                .paymentMethod(menu.getPaymentMethod())
                .currentParticipants(menu.getCurrentParticipants())
                .build();
    }
}
