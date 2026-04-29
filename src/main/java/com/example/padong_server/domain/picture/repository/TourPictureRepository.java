package com.example.padong_server.domain.picture.repository;

import com.example.padong_server.domain.picture.entity.TourPicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourPictureRepository extends JpaRepository<TourPicture, Long> {

    List<TourPicture> findByAdminDongCodeOrderByTitleAscContentIdAsc(String adminDongCode);
}
