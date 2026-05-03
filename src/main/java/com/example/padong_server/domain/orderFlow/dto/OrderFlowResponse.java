package com.example.padong_server.domain.orderFlow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderFlowResponse {

    private Long id;
    private Long menuId;
    private Long storeId;
    private String menuInfo;
    private String status;
    private boolean canApprove;
    private boolean canReject;
    private boolean canMarkReadyForPickup;
    private boolean canCompletePickup;
}
