package com.example.padong_server.domain.score.service;

import org.springframework.stereotype.Component;

@Component
public class ScoreCalculator {

    public double calculateScore(
            double totalMobility,
            double avgTime,
            double density,
            double safety
    ) {
        // legacy
        return 0.0;
    }
}
