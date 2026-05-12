package com.example.padong_server.domain.storeRegistration.repository;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.storeLike.entity.StoreLike;
import com.example.padong_server.domain.storeRegistration.dto.StoreSearchCriteria;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class StoreSpecifications {

    private StoreSpecifications() {}

    /**
     * 검색·필터 조합. status 는 DB 가 아닌 도메인 계산이라 service 에서 후처리.
     */
    public static Specification<Store> from(StoreSearchCriteria criteria, Long currentUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.adminDongCode() != null && !criteria.adminDongCode().isBlank()) {
                var join = root.join("adminDong", JoinType.INNER);
                predicates.add(cb.equal(join.get("adminDongCode"), criteria.adminDongCode()));
            }

            if (criteria.hasText()) {
                String like = "%" + criteria.trimmedQ().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), like),
                                cb.like(cb.lower(root.get("category")), like),
                                cb.like(cb.lower(root.get("description")), like)));
            }

            if (criteria.category() != null && !criteria.category().isEmpty()) {
                predicates.add(root.get("category").in(criteria.category()));
            }

            if (Boolean.TRUE.equals(criteria.likedOnly()) && currentUserId != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                var likeRoot = sub.from(StoreLike.class);
                sub.select(likeRoot.get("store").get("id"))
                        .where(cb.equal(likeRoot.get("userId"), currentUserId));
                predicates.add(root.get("id").in(sub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
