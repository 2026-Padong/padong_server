package com.example.padong_server.domain.hotplace.controller;

import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
import com.example.padong_server.domain.hotplace.service.HotplaceRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "District Realtime", description = "\uC790\uCE58\uAD6C \uAE30\uC900 \uC2E4\uC2DC\uAC04 \uB3C4\uC2DC\uB370\uC774\uD130 \uC870\uD68C API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/realtime/districts")
public class DistrictRealtimeController {

    private final HotplaceRealtimeService hotplaceRealtimeService;

    @Operation(
            summary = "\uC790\uCE58\uAD6C \uC2E4\uC2DC\uAC04 \uB370\uC774\uD130 \uC870\uD68C",
            description = "query parameter guName\uC5D0 \uD574\uB2F9\uD558\uB294 \uC790\uCE58\uAD6C\uC758 \uD56B\uD50C\uB808\uC774\uC2A4 \uBAA9\uB85D\uC744 \uC870\uD68C\uD558\uACE0, \uB300\uD45C \uD56B\uD50C\uB808\uC774\uC2A4\uC758 \uB0A0\uC528 \uC694\uC57D\uACFC \uC804\uCCB4 \uD56B\uD50C\uB808\uC774\uC2A4 \uD63C\uC7A1\uB3C4 \uBAA9\uB85D\uC744 \uBC18\uD658\uD569\uB2C8\uB2E4."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "\uC2E4\uC2DC\uAC04 \uB370\uC774\uD130 \uC870\uD68C \uC131\uACF5",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DistrictRealtimeResponse.class),
                            examples = @ExampleObject(
                                    name = "\uC6A9\uC0B0\uAD6C \uC870\uD68C \uC608\uC2DC",
                                    value = """
                                            {
                                              "guName": "\uC6A9\uC0B0\uAD6C",
                                              "selectedAreaNm": "\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00",
                                              "summary": {
                                                "weatherStatus": "\uB9D1\uC74C",
                                                "temperature": "21.3",
                                                "sensibleTemperature": "22.0",
                                                "humidity": "55%",
                                                "pm10Status": "\uC88B\uC74C",
                                                "pm10": "18.0",
                                                "precipitationProbability": "10%"
                                              },
                                              "hotplaces": [
                                                {
                                                  "areaNm": "\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00",
                                                  "thumbnail": "https://example.com/museum.jpg",
                                                  "roadAddr": "\uC11C\uC6B8 \uC6A9\uC0B0\uAD6C \uC11C\uBE59\uACE0\uB85C 137",
                                                  "areaPpltnMin": "12000",
                                                  "areaPpltnMax": "18000",
                                                  "congestionLevel": "\uC5EC\uC720",
                                                  "dominantAgeGroup": "20\uB300",
                                                  "dominantAgeRate": "31.2%",
                                                  "roadTrafficIdx": "\uC6D0\uD65C",
                                                  "roadTrafficSpd": "42.5"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "\uD574\uB2F9 \uC790\uCE58\uAD6C\uC758 \uD56B\uD50C\uB808\uC774\uC2A4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C"),
            @ApiResponse(responseCode = "500", description = "\uC11C\uC6B8\uC2DC \uC2E4\uC2DC\uAC04 API \uD638\uCD9C \uB610\uB294 \uB0B4\uBD80 \uCC98\uB9AC \uC2E4\uD328")
    })
    @GetMapping
    public ResponseEntity<DistrictRealtimeResponse> getDistrictRealtime(
            @Parameter(description = "\uC870\uD68C\uD560 \uC790\uCE58\uAD6C \uC774\uB984", example = "\uC6A9\uC0B0\uAD6C")
            @RequestParam String guName
    ) {
        return ResponseEntity.ok(hotplaceRealtimeService.getDistrictRealtime(guName));
    }
}
