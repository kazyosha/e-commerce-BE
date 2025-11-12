package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SupplierStatus;
import com.c05.kaz.ecommercebackend.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<SupplierShop, Long> {

    Optional<SupplierShop> findByUser_Id(Long userId);

    List<SupplierShop> findByUser_UserTypeAndUser_Status(
            UserType userType,
            AccountStatus status
    );

    boolean existsByUser_Id(Long userId);

    List<SupplierShop> findByStatus(SupplierStatus status);
}
