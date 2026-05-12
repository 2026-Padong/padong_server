package com.example.padong_server.domain.news.repository;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.news.entity.NewsArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsByAdminDongAndOriginallink(AdminDong adminDong, String originallink);

    List<NewsArticle> findTop20ByAdminDongOrderByFetchedAtDescIdDesc(AdminDong adminDong);

    /**
     * 전체 뉴스에서 무작위 size 개 추출. MySQL ORDER BY RAND() — 행 수 수만 건 이하면 부담 적음.
     */
    @Query(
            value = "SELECT * FROM news_article ORDER BY RAND() LIMIT :size",
            nativeQuery = true)
    List<NewsArticle> findRandom(@Param("size") int size);
}
