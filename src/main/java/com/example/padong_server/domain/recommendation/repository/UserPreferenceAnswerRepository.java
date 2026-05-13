package com.example.padong_server.domain.recommendation.repository;

import com.example.padong_server.domain.recommendation.entity.UserPreferenceAnswer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceAnswerRepository extends JpaRepository<UserPreferenceAnswer, Long> {

    Optional<UserPreferenceAnswer> findByUserId(Long userId);
}
