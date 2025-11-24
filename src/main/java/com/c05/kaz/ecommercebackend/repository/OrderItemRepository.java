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
    SELECT 
        p.name,
        oi.quantity,
        oi.unitPrice,
        (oi.quantity * oi.unitPrice) AS totalSale,
        ((oi.quantity * oi.unitPrice) * 5 / 100) AS platformFee,
        (p.importPrice * oi.quantity) AS cost,
        ((oi.quantity * oi.unitPrice) - o.shippingFee - ((oi.quantity * oi.unitPrice) * 5 / 100)) AS revenue,
        ((oi.quantity * oi.unitPrice) - o.shippingFee 
            - ((oi.quantity * oi.unitPrice) * 5 / 100) 
            - (p.importPrice * oi.quantity)) AS profit,
        o.updatedAt
    FROM OrderItem oi
    JOIN oi.order o
    JOIN oi.product p
    WHERE p.supplier.id = :supplierId
    AND o.status = 'COMPLETED'
    AND o.updatedAt BETWEEN :start AND :end
""")
    List<Object[]> findFinanceReport(Long supplierId,
                                     LocalDateTime start,
                                     LocalDateTime end);
}
