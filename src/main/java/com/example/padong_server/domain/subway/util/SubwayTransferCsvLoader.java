package com.example.padong_server.domain.subway.util;

import com.example.padong_server.domain.subway.entity.SubwayTransferInfo;
import com.example.padong_server.domain.subway.repository.SubwayTransferInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubwayTransferCsvLoader {

    private static final String DEFAULT_PATH = "data/subway/subway_transfer_info.csv";

    private final SubwayTransferInfoRepository subwayTransferInfoRepository;

    @Transactional
    public void loadCsv() {
        loadCsv(DEFAULT_PATH);
    }

    @Transactional
    public void loadCsv(String path) {
        List<SubwayTransferInfo> transferInfos = parseCsv(path);
        subwayTransferInfoRepository.saveAll(transferInfos);
    }

    private List<SubwayTransferInfo> parseCsv(String path) {
        List<SubwayTransferInfo> transferInfos = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream(path),
                        Charset.forName("cp949")
                )
        )) {
            String line;
            boolean isFirst = true;

            while ((line = br.readLine()) != null) {
                if (isFirst) {
                    isFirst = false;
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length < 6) {
                    continue;
                }

                SubwayTransferInfo transferInfo = SubwayTransferInfo.builder()
                        .line(tokens[1].trim())
                        .stationName(tokens[2].trim())
                        .transferLine(tokens[3].trim())
                        .transferDistance(Integer.parseInt(tokens[4].trim()))
                        .transferTime(tokens[5].trim())
                        .build();

                transferInfos.add(transferInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return transferInfos;
    }
}
