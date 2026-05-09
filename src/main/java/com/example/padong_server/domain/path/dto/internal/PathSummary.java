package com.example.padong_server.domain.path.dto.internal;

import com.example.padong_server.domain.path.entity.PathSource;

public record PathSummary(int totalTime, int totalDistance, PathSource source) {}
