package com.example.padong_server.domain.storeLike.repository;

import com.example.padong_server.domain.storeLike.entity.StoreLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreLikeRepository extends JpaRepository<StoreLike, Long> {

    Optional<StoreLike> findByStoreIdAndUserId(Long storeId, Long userId);

    boolean existsByStoreIdAndUserId(Long storeId, Long userId);

    long countByStoreId(Long storeId);

    void deleteByUserId(Long userId);

    /**
     * 커서 기반 — 마지막으로 본 likeId 보다 작은 것 fetch. 검색어 q 는 가게명만 부분 일치
     * (case-insensitive, null/blank 시 무시). 프론트 placeholder "가게 이름을 검색해보세요" 와 일치.
     * Pageable 로 size+1 전달해 hasNext 판별.
     */
    @Query(
            """
            SELECT sl FROM StoreLike sl JOIN FETCH sl.store s
            WHERE sl.userId = :userId
              AND (:cursor IS NULL OR sl.id < :cursor)
              AND (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY sl.id DESC
            """)
    List<StoreLike> findMyLikesCursor(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            @Param("q") String q,
            Pageable pageable);
}
