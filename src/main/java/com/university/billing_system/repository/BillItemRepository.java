package com.university.billing_system.repository;

import com.university.billing_system.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {

    @Query("SELECT COUNT(i) > 0 FROM BillItem i WHERE i.product.productId = :productId")
    boolean existsByProductId(@Param("productId") Long productId);
}