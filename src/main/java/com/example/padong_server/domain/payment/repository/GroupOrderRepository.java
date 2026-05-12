package com.example.padong_server.domain.payment.repository;

import com.example.padong_server.domain.payment.entity.GroupOrder;
import com.example.padong_server.domain.payment.entity.GroupOrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupOrderRepository extends JpaRepository<GroupOrder, Long> {

    /** 한 가게의 가장 최근 active(=OPEN) 공구 1개. */
    Optional<GroupOrder> findTopByStoreIdAndStatusOrderByIdDesc(Long storeId, GroupOrderStatus status);

    /** 한 가게의 가장 최근 공구 1개 (status 무관). */
    Optional<GroupOrder> findTopByStoreIdOrderByIdDesc(Long storeId);

    /** 여러 가게의 active 공구 일괄 조회 (목록 화면 N+1 회피용). */
    List<GroupOrder> findByStoreIdInAndStatus(Collection<Long> storeIds, GroupOrderStatus status);


    @Modifying(flushAutomatically = true)
    @Query("""
            update GroupOrder g
            set g.currentParticipants = g.currentParticipants + 1,
                g.currentAmount = g.currentAmount + :amount
            where g.id = :groupOrderId
              and g.currentParticipants < g.maxParticipants
              and g.status = :status
            """)
    int increaseParticipantsIfAvailable(
            @Param("groupOrderId") Long groupOrderId,
            @Param("amount") int amount,
            @Param("status") GroupOrderStatus status
    );

    default int increaseParticipantsIfAvailable(Long groupOrderId, int amount) {
        return increaseParticipantsIfAvailable(groupOrderId, amount, GroupOrderStatus.OPEN);
    }

    @Modifying(flushAutomatically = true)
    @Query("""
            update GroupOrder g
            set g.currentParticipants =
                    case when g.currentParticipants > 0 then g.currentParticipants - 1 else 0 end,
                g.currentAmount =
                    case when g.currentAmount >= :amount then g.currentAmount - :amount else 0 end
            where g.id = :groupOrderId
            """)
    int decreaseParticipants(@Param("groupOrderId") Long groupOrderId, @Param("amount") int amount);
}
