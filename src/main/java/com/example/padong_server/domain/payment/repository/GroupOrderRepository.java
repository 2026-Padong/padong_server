package com.example.padong_server.domain.payment.repository;

import com.example.padong_server.domain.payment.entity.GroupOrder;
import com.example.padong_server.domain.payment.entity.GroupOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupOrderRepository extends JpaRepository<GroupOrder, Long> {

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
