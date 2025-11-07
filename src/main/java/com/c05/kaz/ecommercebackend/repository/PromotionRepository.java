package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.Promotion;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findBySupplier(SupplierShop supplier);
    Optional<Promotion> findByCode(String code);
}
