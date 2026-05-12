package com.example.padong_server.domain.dongneLike.repository;

import com.example.padong_server.domain.dongneLike.entity.DongneLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DongneLikeRepository extends JpaRepository<DongneLike, Long> {

    Optional<DongneLike> findByAdminDongIdAndUserId(Long adminDongId, Long userId);

    boolean existsByAdminDongIdAndUserId(Long adminDongId, Long userId);

    long countByAdminDongId(Long adminDongId);

    void deleteByUserId(Long userId);

    /**
     * 커서 기반. q 매칭 룰은 /dongs/search (AdminDongRepository#searchByName) 와 동일:
     * 행정동명 · 자치구명 · 전체 주소 (city+district+admin) 부분 일치.
     * 정렬은 prefix 우선 (행정동명 prefix > 자치구명 prefix > contains), 동순위는 likeId DESC.
     */
    @Query(
            """
            SELECT dl FROM DongneLike dl JOIN FETCH dl.adminDong d
            WHERE dl.userId = :userId
              AND (:cursor IS NULL OR dl.id < :cursor)
              AND (:q IS NULL
                   OR LOWER(d.adminDongName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(d.districtName)  LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(d.cityName, ' ', d.districtName, ' ', d.adminDongName))
                      LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY
              CASE
                WHEN LOWER(d.adminDongName) LIKE LOWER(CONCAT(:q, '%')) THEN 0
                WHEN LOWER(d.districtName)  LIKE LOWER(CONCAT(:q, '%')) THEN 1
                ELSE 2
              END,
              dl.id DESC
            """)
    List<DongneLike> findMyLikesCursor(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            @Param("q") String q,
            Pageable pageable);
}
