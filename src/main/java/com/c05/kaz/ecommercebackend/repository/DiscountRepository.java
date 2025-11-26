package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.Discount;
import com.c05.kaz.ecommercebackend.entity.Product;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCodeAndSupplier(String code, SupplierShop supplier);

    List<Discount> findBySupplier(SupplierShop supplier);

    // ⭐ FIELD NÀY TRONG Discount LÀ applicableProducts — KHÔNG PHẢI products
    List<Discount> findByApplicableProductsContains(Product product);

    Optional<Discount> findByCode(String discountCode);
}
