package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<SupplierShop, Long> {
}