package com.example.padong_server.domain.oauth.repository;

import com.example.padong_server.domain.oauth.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKakaoId(Long kakaoId);

    boolean existsByKakaoId(Long kakaoId);
}
