package com.example.padong_server.domain.news.repository;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.news.entity.NewsArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsByAdminDongAndOriginallink(AdminDong adminDong, String originallink);

    List<NewsArticle> findTop20ByAdminDongOrderByFetchedAtDescIdDesc(AdminDong adminDong);
}
