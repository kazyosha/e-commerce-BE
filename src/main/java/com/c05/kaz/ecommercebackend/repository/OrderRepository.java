package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.Order;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByInvoiceNo(String invoiceNo);

    // Đơn đã hoàn tất & đã thanh toán
    List<Order> findByCustomer_IdAndStatusAndPaidTrueOrderByCreatedAtDesc(
            Long customerId,
            OrderStatus status
    );

    // Đơn theo 1 trạng thái
    List<Order> findByCustomer_IdAndStatusOrderByCreatedAtDesc(
            Long customerId,
            OrderStatus status
    );

    // Đơn theo nhiều trạng thái (IN)
    List<Order> findByCustomer_IdAndStatusInOrderByCreatedAtDesc(
            Long customerId,
            Collection<OrderStatus> statuses
    );

    // Lấy 1 đơn theo id + customer
    Optional<Order> findByIdAndCustomer_Id(Long orderId, Long customerId);

    // Supplier
    Optional<Order> findByIdAndSupplier_Id(Long orderId, Long supplierId);

    List<Order> findBySupplier_IdOrderByCreatedAtDesc(Long supplierId);

    List<Order> findBySupplier_IdAndStatusOrderByCreatedAtDesc(Long supplierId, OrderStatus status);
}
