package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import com.c05.kaz.ecommercebackend.entity.Order;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Object> findByInvoiceNo(String invoiceNo);

    // Đơn đã hoàn tất & đã thanh toán
    List<Order> findByCustomer_IdAndStatusAndPaidTrueOrderByCreatedAtDesc(
            Long customerId,
            OrderStatus status
    );

    // Đơn đang giao
    List<Order> findByCustomer_IdAndStatusOrderByCreatedAtDesc(
            Long customerId,
            OrderStatus status
    );

    // Lấy 1 đơn theo id + customer (chống xem ké đơn của người khác)
    Optional<Order> findByIdAndCustomer_Id(Long orderId, Long customerId);
}
