package com.example.padong_server.domain.activityMobility.support;

import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.dongne.entity.AdminDong;

import java.util.HashSet;
import java.util.Set;

/** 여러 도착동에 공통으로 등장한 출발동의 이동량을 누적하는 계산용 객체. */
public final class CommonDepartureAccumulator {

    private final AdminDong departureDong;
    private final Set<String> arrivalDongCodes = new HashSet<>();
    private double totalMobility;

    public CommonDepartureAccumulator(AdminDong departureDong) {
        this.departureDong = departureDong;
    }

    public void add(Mobility mobility) {
        arrivalDongCodes.add(mobility.getArrivalDong().getAdminDongCode());
        totalMobility += mobility.getTotalMobility();
    }

    public AdminDong departureDong() {
        return departureDong;
    }

    public int arrivalDongCount() {
        return arrivalDongCodes.size();
    }

    public double totalMobility() {
        return totalMobility;
    }
}
