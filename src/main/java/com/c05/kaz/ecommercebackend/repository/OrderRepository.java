package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import com.c05.kaz.ecommercebackend.entity.Order;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(CustomerProfile customer);
    List<Order> findBySupplier(SupplierShop supplier);
    List<Order> findBySupplierAndStatus(SupplierShop supplier, OrderStatus status);
}
