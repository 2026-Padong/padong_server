package com.example.padong_server.domain.menu.service;

import com.example.padong_server.domain.menu.dto.MenuCreateRequest;
import com.example.padong_server.domain.menu.dto.MenuResponse;
import com.example.padong_server.domain.menu.dto.MenuUpdateRequest;
import com.example.padong_server.domain.menu.entity.Menu;
import com.example.padong_server.domain.menu.repository.MenuRepository;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowRepository;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuService {

    private static final List<OrderFlowStatus> ACTIVE_ORDER_FLOW_STATUSES =
            List.copyOf(EnumSet.of(
                    OrderFlowStatus.PENDING,
                    OrderFlowStatus.WAITING_APPROVAL,
                    OrderFlowStatus.APPROVED,
                    OrderFlowStatus.READY));

    private final MenuRepository menuRepository;
    private final StoreRegistrationRepository storeRegistrationRepository;
    private final OrderFlowRepository orderFlowRepository;

    @Transactional
    public MenuResponse createMenu(MenuCreateRequest request) {
        Store store = findStore(request.storeId());
        Menu menu = Menu.builder()
                .store(store)
                .name(request.name().trim())
                .price(request.price())
                .build();
        return MenuResponse.from(menuRepository.save(menu));
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> getMenus(Long storeId) {
        findStore(storeId);
        return menuRepository.findAllByStoreId(storeId).stream()
                .map(MenuResponse::from)
                .toList();
    }

    @Transactional
    public MenuResponse updateMenu(Long menuId, MenuUpdateRequest request) {
        Menu menu = findMenu(menuId);
        menu.update(request.name().trim(), request.price());
        return MenuResponse.from(menu);
    }

    @Transactional
    public void deleteMenu(Long menuId) {
        menuRepository.delete(findMenu(menuId));
    }

    @Transactional
    public MenuResponse toggleSoldOut(Long menuId, boolean soldOut) {
        Menu menu = findMenu(menuId);
        // soldOut=true 로 바꾸려는데 진행 중 모임에 묶여있으면 차단.
        // (소비자 결제가 이미 진행 중인 메뉴를 갑자기 품절 처리하지 못하게.)
        if (soldOut
                && !menu.isSoldOut()
                && orderFlowRepository.existsActiveOrderFlowContainingMenu(
                        menuId, ACTIVE_ORDER_FLOW_STATUSES)) {
            throw new CustomException(ErrorCode.MENU_IN_ACTIVE_ORDER_FLOW);
        }
        menu.changeSoldOut(soldOut);
        return MenuResponse.from(menu);
    }

    private Store findStore(Long storeId) {
        if (storeId == null) {
            throw new CustomException(ErrorCode.INVALID_MENU_REQUEST);
        }
        return storeRegistrationRepository
                .findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    private Menu findMenu(Long menuId) {
        return menuRepository
                .findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
    }
}
