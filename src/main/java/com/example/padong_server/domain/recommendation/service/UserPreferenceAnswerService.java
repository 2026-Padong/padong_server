package com.example.padong_server.domain.recommendation.service;

import com.example.padong_server.domain.recommendation.dto.PersonalRecommendationRequest;
import com.example.padong_server.domain.recommendation.entity.UserPreferenceAnswer;
import com.example.padong_server.domain.recommendation.repository.UserPreferenceAnswerRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPreferenceAnswerService {

    private final UserPreferenceAnswerRepository repository;

    @Transactional
    public void upsert(Long userId, PersonalRecommendationRequest request) {
        if (userId == null) return;
        repository
                .findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.updateAnswers(
                                request.getQ1(), request.getQ2(), request.getQ3(),
                                request.getQ4(), request.getQ5(), request.getQ6(),
                                request.getQ7(), request.getQ8(), request.getQ9(),
                                request.getQ10()),
                        () -> repository.save(UserPreferenceAnswer.builder()
                                .userId(userId)
                                .q1(request.getQ1()).q2(request.getQ2()).q3(request.getQ3())
                                .q4(request.getQ4()).q5(request.getQ5()).q6(request.getQ6())
                                .q7(request.getQ7()).q8(request.getQ8()).q9(request.getQ9())
                                .q10(request.getQ10())
                                .build()));
    }

    @Transactional(readOnly = true)
    public Optional<UserPreferenceAnswer> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }
}
