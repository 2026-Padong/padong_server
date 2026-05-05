package com.example.padong_server.global.client.tour;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tour_api_daily_usage")
public class TourApiDailyUsage {

    @Id
    private LocalDate usageDate;

    @Column(nullable = false)
    private int requestCount;

    public TourApiDailyUsage(LocalDate usageDate, int requestCount) {
        this.usageDate = usageDate;
        this.requestCount = requestCount;
    }

    public int addRequests(int count) {
        this.requestCount += count;
        return this.requestCount;
    }
}
