package com.university.billing_system.repository;

import com.university.billing_system.entity.Bill;
import com.university.billing_system.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {

    @Query("""
           SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b
           WHERE b.status = :status
             AND b.finalizedAt >= :start AND b.finalizedAt < :end
           """)
    BigDecimal sumSalesBetween(@Param("status") BillStatus status,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    List<Bill> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start,
                                                                   LocalDateTime end);
}