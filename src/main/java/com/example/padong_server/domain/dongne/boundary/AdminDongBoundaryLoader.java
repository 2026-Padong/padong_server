package com.example.padong_server.domain.dongne.boundary;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.entity.AdminDongBoundary;
import com.example.padong_server.domain.dongne.repository.AdminDongBoundaryRepository;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * GeoJSON 파일 → admin_dong_boundary 테이블 import 서비스. 다른 데이터 적재 패턴
 * ({@code POST /dongne}, {@code POST /realtime/data}) 와 동일하게 admin endpoint
 * 호출로 1회 적재. 적재 후엔 일반 조회 API 가 DB 만 쓴다.
 *
 * <p>매칭 룰 (자세한 배경은 docs/dong-boundary.md):
 * <ol>
 *   <li>통계청 공식 코드표(stat_region_code_202506.csv) 로 GeoJSON ADM_CD →
 *       권위 있는 (sgg_name, emd_name) 획득
 *   <li>우리 AdminDong 과 (districtName, normalized adminDongName) 으로 join → admin_dong_id 확보
 *   <li>매칭된 행만 AdminDongBoundary insert
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDongBoundaryLoader {

    private static final String GEOJSON_PATH = "data/dongne/admin_dong_boundary_seoul.geojson";
    private static final String STAT_CSV_PATH = "data/dongne/stat_region_code_202506.csv";

    private final ObjectMapper objectMapper;
    private final AdminDongRepository adminDongRepository;
    private final AdminDongBoundaryRepository boundaryRepository;

    /**
     * 행정동 경계 일괄 import. 이미 데이터 있으면 0 반환 (idempotent).
     *
     * @return insert 된 행 수
     */
    @Transactional
    public int importIfEmpty() {
        long existing = boundaryRepository.count();
        if (existing > 0) {
            log.info("AdminDongBoundary 이미 {}건 존재 — import 스킵", existing);
            return 0;
        }
        try {
            Map<String, StatEntry> statByAdmCd = loadStatCodes();
            List<AdminDongBoundary> rows = buildRows(statByAdmCd);
            boundaryRepository.saveAll(rows);
            log.info(
                    "AdminDongBoundary import 완료. inserted={}/{}",
                    rows.size(),
                    statByAdmCd.size());
            return rows.size();
        } catch (IOException e) {
            log.warn("AdminDongBoundary import 실패", e);
            throw new com.example.padong_server.global.exception.CustomException(
                    com.example.padong_server.global.exception.ErrorCode.VALIDATION_ERROR,
                    "행정동 경계 데이터 import 실패: " + e.getMessage());
        }
    }

    private Map<String, StatEntry> loadStatCodes() throws IOException {
        Map<String, StatEntry> byAdmCd = new HashMap<>();
        try (InputStream in = new ClassPathResource(STAT_CSV_PATH).getInputStream();
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            br.readLine(); // header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 6) continue;
                String admCd = cols[0].trim() + cols[2].trim() + cols[4].trim();
                byAdmCd.put(admCd, new StatEntry(cols[3].trim(), cols[5].trim()));
            }
        }
        return byAdmCd;
    }

    private List<AdminDongBoundary> buildRows(Map<String, StatEntry> statByAdmCd) throws IOException {
        // (districtName, normalizedName) → AdminDong 인덱싱
        Map<String, AdminDong> ourByKey = new HashMap<>();
        for (AdminDong d : adminDongRepository.findAll()) {
            ourByKey.put(keyOf(d.getDistrictName(), d.getAdminDongName()), d);
        }

        List<AdminDongBoundary> rows = new ArrayList<>();
        try (InputStream in = new ClassPathResource(GEOJSON_PATH).getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            JsonNode features = root.get("features");
            if (features == null || !features.isArray()) {
                log.warn("GeoJSON 에 features 배열 없음");
                return rows;
            }
            for (JsonNode feature : features) {
                JsonNode props = feature.get("properties");
                if (props == null) continue;
                String admCd = textOf(props, "ADM_CD");
                if (admCd == null) continue;
                StatEntry stat = statByAdmCd.get(admCd);
                if (stat == null) {
                    log.warn("통계청 코드표에 없는 ADM_CD. admCd={}", admCd);
                    continue;
                }
                AdminDong ours = ourByKey.get(keyOf(stat.guName(), stat.dongName()));
                if (ours == null) {
                    log.warn(
                            "AdminDong 매칭 실패. guName={}, dongName={}, admCd={}",
                            stat.guName(),
                            stat.dongName(),
                            admCd);
                    continue;
                }
                String enrichedJson = objectMapper.writeValueAsString(
                        enrichFeature(feature, ours.getAdminDongCode(), stat));
                rows.add(AdminDongBoundary.builder()
                        .adminDong(ours)
                        .geometryJson(enrichedJson)
                        .build());
            }
        }
        return rows;
    }

    /** 응답 시 properties 가 일관되도록 우리 코드/이름/구명을 박아둠. */
    private JsonNode enrichFeature(JsonNode original, String ourCode, StatEntry stat) {
        ObjectNode feature = (ObjectNode) original.deepCopy();
        ObjectNode props = objectMapper.createObjectNode();
        props.put("adminDongCode", ourCode);
        props.put("name", stat.dongName());
        props.put("guName", stat.guName());
        feature.set("properties", props);
        return feature;
    }

    private String keyOf(String guName, String adminDongName) {
        return guName + "/" + normalize(adminDongName);
    }

    /** 표기 차이 흡수: '제제' 중복 (CSV 타이포), '제3동' → '3동', '·'/'.' / 공백 제거. */
    private String normalize(String name) {
        if (name == null) return "";
        return name.replace("제제", "제")
                .replaceAll("제(\\d)", "$1")
                .replace("·", "")
                .replace(".", "")
                .replaceAll("\\s+", "");
    }

    private String textOf(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private record StatEntry(String guName, String dongName) {}
}
