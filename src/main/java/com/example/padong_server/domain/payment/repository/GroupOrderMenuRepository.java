package com.example.padong_server.domain.payment.repository;

import com.example.padong_server.domain.payment.entity.GroupOrderMenu;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupOrderMenuRepository extends JpaRepository<GroupOrderMenu, Long> {

    List<GroupOrderMenu> findAllByGroupOrderIdAndMenuIdIn(Long groupOrderId, Collection<Long> menuIds);

    Optional<GroupOrderMenu> findByGroupOrderIdAndMenuId(Long groupOrderId, Long menuId);
}
