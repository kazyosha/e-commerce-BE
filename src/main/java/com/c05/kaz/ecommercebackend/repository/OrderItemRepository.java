package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.Order;
import com.c05.kaz.ecommercebackend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    @Query("""
    SELECT SUM(
        oi.lineTotal * 1.0 * (o.shopRevenue * 1.0 / o.originalTotal)
    )
    FROM OrderItem oi
    JOIN oi.order o
    WHERE oi.product.id = :productId
      AND o.status = 'COMPLETED'
""")
    Long getProductShopRevenue(Long productId);


    @Query("""
    SELECT 
        p.name,
        oi.quantity,
        oi.unitPrice,

        (oi.lineTotal * 1.0 * (o.shopRevenue * 1.0 / o.originalTotal)),

        ((oi.lineTotal * 1.0 * (o.shopRevenue * 1.0 / o.originalTotal)) * 0.05),

        (p.importPrice * oi.quantity),

        ((oi.lineTotal * 1.0 * (o.shopRevenue * 1.0 / o.originalTotal))
            - ((oi.lineTotal * 1.0 * (o.shopRevenue * 1.0 / o.originalTotal)) * 0.05)),

        (
            ((oi.lineTotal * 1.0 * (o.shopRevenue * 1.0 / o.originalTotal))
                - ((oi.lineTotal * 1.0 * (o.shopRevenue * 1.0 / o.originalTotal)) * 0.05))
            - (p.importPrice * oi.quantity)
        ),

        o.updatedAt
    FROM OrderItem oi
    JOIN oi.order o
    JOIN oi.product p
    WHERE p.supplier.id = :supplierId
      AND o.status = com.c05.kaz.ecommercebackend.enums.OrderStatus.COMPLETED
      AND o.updatedAt BETWEEN :start AND :end
""")
    List<Object[]> findFinanceReport(
            Long supplierId,
            LocalDateTime start,
            LocalDateTime end
    );
}