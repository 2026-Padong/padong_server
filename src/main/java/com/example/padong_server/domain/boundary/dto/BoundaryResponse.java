package com.example.padong_server.domain.boundary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BoundaryResponse {
    List<List<Double>> points;
}
