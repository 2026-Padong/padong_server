package com.example.padong_server.domain.transitPath.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class OdsayJson {

    private OdsayJson() {}

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>(list.size());
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> firstMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                if (element instanceof Map<?, ?> map) {
                    return (Map<String, Object>) map;
                }
            }
        }
        return Map.of();
    }

    static int intValue(Object value, int defaultValue) {
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    static Integer nullableNonNegativeInt(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        int intValue = number.intValue();
        return intValue < 0 ? null : intValue;
    }

    static double doubleValue(Object value, double defaultValue) {
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    static String stringValue(Object value) {
        return Objects.toString(value, "");
    }
}
