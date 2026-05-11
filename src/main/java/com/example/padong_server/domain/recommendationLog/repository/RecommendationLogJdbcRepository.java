package com.example.padong_server.domain.recommendationLog.repository;

import com.example.padong_server.domain.recommendationLog.dto.RecommendationLogRow;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationResultLogItem;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationSurveyAnswers;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationType;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RecommendationLogJdbcRepository {

    private static final String TABLE_NAME = "user_recommendation_logs";

    private final ObjectProvider<JdbcTemplate> aiJdbcTemplateProvider;

    public RecommendationLogJdbcRepository(
            @Qualifier("aiJdbcTemplate") ObjectProvider<JdbcTemplate> aiJdbcTemplateProvider) {
        this.aiJdbcTemplateProvider = aiJdbcTemplateProvider;
    }

    public int[] batchInsertImpressions(
            Long userId,
            List<RecommendationResultLogItem> recommendations,
            RecommendationSurveyAnswers answers,
            RecommendationType recommendationType
    ) {
        String sql = """
                INSERT INTO user_recommendation_logs (
                    user_id,
                    admin_dong_code,
                    rank_position,
                    impression,
                    clicked_count,
                    liked_count,
                    dwell_time_sec,
                    recommendation_type,
                    q1,
                    q2,
                    q3,
                    q4,
                    q5,
                    q6,
                    q7,
                    q8,
                    q9,
                    q10
                ) VALUES (?, ?, ?, TRUE, 0, 0, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try {
            return jdbcTemplate()
                    .batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            RecommendationResultLogItem recommendation = recommendations.get(i);
                            ps.setLong(1, userId);
                            ps.setString(2, recommendation.adminDongCode());
                            ps.setInt(3, i + 1);
                            ps.setString(4, recommendationType.name());
                            ps.setObject(5, answers.q1());
                            ps.setObject(6, answers.q2());
                            ps.setObject(7, answers.q3());
                            ps.setObject(8, answers.q4());
                            ps.setObject(9, answers.q5());
                            ps.setObject(10, answers.q6());
                            ps.setObject(11, answers.q7());
                            ps.setObject(12, answers.q8());
                            ps.setObject(13, answers.q9());
                            ps.setObject(14, answers.q10());
                        }

                        @Override
                        public int getBatchSize() {
                            return recommendations.size();
                        }
                    });
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.AI_LOG_DB_ERROR, exception);
        }
    }

    public Optional<RecommendationLogRow> findLatestToday(Long userId, String adminDongCode) {
        String sql = """
                SELECT
                    id,
                    user_id,
                    admin_dong_code,
                    rank_position,
                    impression,
                    clicked_count,
                    liked_count,
                    dwell_time_sec,
                    recommendation_type,
                    q1,
                    q2,
                    q3,
                    q4,
                    q5,
                    q6,
                    q7,
                    q8,
                    q9,
                    q10,
                    created_at,
                    updated_at
                FROM user_recommendation_logs
                WHERE user_id = ?
                  AND admin_dong_code = ?
                  AND created_at >= CURRENT_DATE
                  AND created_at < CURRENT_DATE + INTERVAL 1 DAY
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """;

        try {
            List<RecommendationLogRow> rows =
                    jdbcTemplate().query(sql, rowMapper(), userId, adminDongCode);
            return rows.stream().findFirst();
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.AI_LOG_DB_ERROR, exception);
        }
    }

    public int incrementClickedCount(Long userId, String adminDongCode) {
        String sql = updateLatestTodaySql("clicked_count = COALESCE(clicked_count, 0) + 1");
        return update(sql, userId, adminDongCode);
    }

    public int updateLiked(Long userId, String adminDongCode, boolean liked) {
        String sql = updateLatestTodaySql("liked_count = ?");
        return update(sql, liked ? 1 : 0, userId, adminDongCode);
    }

    public int updateDwellTimeSec(Long userId, String adminDongCode, int dwellTimeSec) {
        String sql = updateLatestTodaySql("dwell_time_sec = GREATEST(COALESCE(dwell_time_sec, 0), ?)");
        return update(sql, dwellTimeSec, userId, adminDongCode);
    }

    private int update(String sql, Object... args) {
        try {
            return jdbcTemplate().update(sql, args);
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.AI_LOG_DB_ERROR, exception);
        }
    }

    private String updateLatestTodaySql(String setExpression) {
        return """
                UPDATE %s
                SET %s
                WHERE id = (
                    SELECT id
                    FROM (
                        SELECT id
                        FROM %s
                        WHERE user_id = ?
                          AND admin_dong_code = ?
                          AND created_at >= CURRENT_DATE
                          AND created_at < CURRENT_DATE + INTERVAL 1 DAY
                        ORDER BY created_at DESC, id DESC
                        LIMIT 1
                    ) latest_log
                )
                """.formatted(TABLE_NAME, setExpression, TABLE_NAME);
    }

    private JdbcTemplate jdbcTemplate() {
        JdbcTemplate jdbcTemplate = aiJdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            throw new CustomException(ErrorCode.AI_LOG_DB_ERROR, "AI 로그 DB 설정이 없습니다.");
        }
        return jdbcTemplate;
    }

    private RowMapper<RecommendationLogRow> rowMapper() {
        return (rs, rowNum) -> new RecommendationLogRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("admin_dong_code"),
                rs.getInt("rank_position"),
                rs.getBoolean("impression"),
                rs.getInt("clicked_count"),
                rs.getInt("liked_count"),
                rs.getInt("dwell_time_sec"),
                recommendationType(rs),
                getInteger(rs, "q1"),
                getInteger(rs, "q2"),
                getInteger(rs, "q3"),
                getInteger(rs, "q4"),
                getInteger(rs, "q5"),
                getInteger(rs, "q6"),
                getInteger(rs, "q7"),
                getInteger(rs, "q8"),
                getInteger(rs, "q9"),
                getInteger(rs, "q10"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private RecommendationType recommendationType(ResultSet rs) throws SQLException {
        String value = rs.getString("recommendation_type");
        if (value == null || value.isBlank()) {
            return null;
        }
        return RecommendationType.valueOf(value);
    }

    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
