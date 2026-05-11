package com.example.padong_server.domain.picture.repository;

import com.example.padong_server.domain.picture.entity.TourPicture;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourPictureRepository extends JpaRepository<TourPicture, Long> {

    List<TourPicture> findByAdminDongCodeOrderByTitleAscContentIdAsc(String adminDongCode);

    Optional<TourPicture> findByContentId(String contentId);
}
