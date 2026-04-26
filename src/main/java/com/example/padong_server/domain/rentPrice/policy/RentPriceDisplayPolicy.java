package com.example.padong_server.domain.rentPrice.policy;

import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RentPriceDisplayPolicy {

    public PriceDecision decideSale(RentPrice stat) {
        return decideSingleValue(
                stat.getMedianSalePrice(),
                stat.getAvgSalePrice(),
                stat.getAvgSalePricePerSquareMeter()
        );
    }

    public PriceDecision decideJeonse(RentPrice stat) {
        return decideSingleValue(
                stat.getMedianJeonseDeposit(),
                stat.getAvgJeonseDeposit(),
                stat.getAvgJeonseDepositPerSquareMeter()
        );
    }

    public MonthlyRentDecision decideMonthlyRent(RentPrice stat) {
        if (stat.getMedianMonthlyDeposit() != null && stat.getMedianMonthlyRent() != null) {
            return new MonthlyRentDecision(
                    stat.getMedianMonthlyDeposit(),
                    stat.getMedianMonthlyRent(),
                    MetricSource.MEDIAN
            );
        }
        if (stat.getAvgMonthlyDeposit() != null && stat.getAvgMonthlyRent() != null) {
            return new MonthlyRentDecision(
                    stat.getAvgMonthlyDeposit(),
                    stat.getAvgMonthlyRent(),
                    MetricSource.AVERAGE
            );
        }
        return new MonthlyRentDecision(
                null,
                null,
                MetricSource.NONE
        );
    }

    private PriceDecision decideSingleValue(
            Long median,
            Long average,
            BigDecimal averagePerSquareMeter
    ) {
        if (median != null) {
            return new PriceDecision(median, null, MetricSource.MEDIAN);
        }
        if (average != null) {
            return new PriceDecision(average, null, MetricSource.AVERAGE);
        }
        if (averagePerSquareMeter != null) {
            return new PriceDecision(null, averagePerSquareMeter, MetricSource.AVERAGE_PER_SQUARE_METER);
        }
        return new PriceDecision(null, null, MetricSource.NONE);
    }

    public enum MetricSource {
        MEDIAN,
        AVERAGE,
        AVERAGE_PER_SQUARE_METER,
        NONE
    }

    public record PriceDecision(
            Long amount,
            BigDecimal amountPerSquareMeter,
            MetricSource source
    ) {
        public boolean hasAmount() {
            return amount != null;
        }
    }

    public record MonthlyRentDecision(
            Long deposit,
            Long monthlyRent,
            MetricSource source
    ) {
        public boolean hasValues() {
            return deposit != null && monthlyRent != null;
        }
    }
}
