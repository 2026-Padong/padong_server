package com.example.padong_server.domain.dongne.boundary;

import com.example.padong_server.domain.dongne.entity.AdminDongBoundary;
import com.example.padong_server.domain.dongne.repository.AdminDongBoundaryRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 행정동 경계 조회 — DB-backed. {@link AdminDongBoundaryRepository} 에서 직접 fetch 후
 * 저장된 JSON 문자열을 {@link JsonNode} 로 deserialize 해 응답에 그대로 노출.
 */
@Service
@RequiredArgsConstructor
public class AdminDongBoundaryService {

    private final AdminDongBoundaryRepository boundaryRepository;
    private final ObjectMapper objectMapper;

    /** 단건 — 매핑 안 된 코드면 404. */
    @Transactional(readOnly = true)
    public JsonNode getFeature(String adminDongCode) {
        AdminDongBoundary row =
                boundaryRepository
                        .findByAdminDong_AdminDongCode(adminDongCode)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.ADMIN_DONG_BOUNDARY_NOT_FOUND,
                                                "행정동 경계 데이터가 없습니다. adminDongCode="
                                                        + adminDongCode));
        return readJson(row.getGeometryJson());
    }

    /** 단건 — 없으면 null. Service 가 자체 호출 시 try/catch 대신 null check. */
    @Transactional(readOnly = true)
    public JsonNode findFeature(String adminDongCode) {
        return boundaryRepository
                .findByAdminDong_AdminDongCode(adminDongCode)
                .map(r -> readJson(r.getGeometryJson()))
                .orElse(null);
    }

    /** 다건 — FeatureCollection 묶음. 없는 코드는 조용히 제외. */
    @Transactional(readOnly = true)
    public JsonNode getFeatureCollection(Collection<String> codes) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "FeatureCollection");
        ArrayNode features = objectMapper.createArrayNode();
        for (AdminDongBoundary row :
                boundaryRepository.findAllByAdminDong_AdminDongCodeIn(codes)) {
            features.add(readJson(row.getGeometryJson()));
        }
        root.set("features", features);
        return root;
    }

    /** code 리스트로 한 번에 fetch — {@code DongSuggestionItem} 같이 N건 batch 빌드용. */
    @Transactional(readOnly = true)
    public Map<String, JsonNode> findFeaturesByCodes(Collection<String> codes) {
        Map<String, JsonNode> result = new HashMap<>();
        if (codes == null || codes.isEmpty()) {
            return result;
        }
        List<AdminDongBoundary> rows =
                boundaryRepository.findAllByAdminDong_AdminDongCodeIn(codes);
        for (AdminDongBoundary row : rows) {
            result.put(row.getAdminDong().getAdminDongCode(), readJson(row.getGeometryJson()));
        }
        return result;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException e) {
            throw new CustomException(
                    ErrorCode.ADMIN_DONG_BOUNDARY_NOT_FOUND,
                    "행정동 경계 데이터 파싱 실패",
                    e);
        }
    }
}
