package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.DiscountUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountUsageLogRepository extends JpaRepository<DiscountUsageLog, Long> {

    long countByDiscountIdAndUserId(Long discountId, Long userId);
}

