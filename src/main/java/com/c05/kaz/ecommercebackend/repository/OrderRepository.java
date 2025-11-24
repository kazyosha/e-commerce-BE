package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.dto.supplier.StoreRevenueDTO;
import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import com.c05.kaz.ecommercebackend.entity.Order;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer(CustomerProfile customer);

    List<Order> findBySupplier(SupplierShop supplier);

    List<Order> findBySupplierAndStatus(SupplierShop supplier, OrderStatus status);

    @Query("""
    SELECT new com.c05.kaz.ecommercebackend.dto.supplier.StoreRevenueDTO(
        s.id,
        s.shopName,
        COALESCE(SUM(o.finalTotal), 0L),
        COALESCE((SUM(o.finalTotal) * 5L) / 100L, 0L))
    FROM SupplierShop s
    LEFT JOIN Order o
        ON o.supplier.id = s.id
        AND o.status = 'COMPLETED'
    WHERE s.id IN :supplierIds
    GROUP BY s.id, s.shopName
""")
    List<StoreRevenueDTO> calculateRevenue(List<Long> supplierIds);



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
    List<Order> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);


    // Lấy 1 đơn theo id + customer
    Optional<Order> findByIdAndCustomer_Id(Long orderId, Long customerId);

    // Supplier
    Optional<Order> findByIdAndSupplier_Id(Long orderId, Long supplierId);

    List<Order> findBySupplier_IdOrderByCreatedAtDesc(Long supplierId);

    List<Order> findBySupplier_IdAndStatusOrderByCreatedAtDesc(Long supplierId, OrderStatus status);

    @Query("""
        SELECT 
            o.supplier.id AS supplierId,
            o.supplier.shopName AS shopName,
            COUNT(o.id) AS totalOrders,
            SUM(o.finalTotal) AS totalRevenue
        FROM Order o
        WHERE o.supplier.id = :supplierId
          AND o.status = com.c05.kaz.ecommercebackend.enums.OrderStatus.COMPLETED
        GROUP BY o.supplier.id, o.supplier.shopName
    """)
    List<Object[]> getRevenueBySupplier(Long supplierId);


    @Query("""
    SELECT 
        s.id AS supplierId,
        s.shopName AS shopName,
        COUNT(o.id) AS totalOrders,
        COALESCE(SUM(o.finalTotal), 0) AS totalRevenue
    FROM SupplierShop s
    LEFT JOIN Order o
        ON o.supplier.id = s.id
        AND o.status = com.c05.kaz.ecommercebackend.enums.OrderStatus.COMPLETED
    GROUP BY s.id, s.shopName
    ORDER BY s.id ASC
""")
    List<Object[]> getAllSuppliersRevenue();


    @Query("""
    SELECT 
        MONTH(o.createdAt) AS month,
        SUM(o.finalTotal) AS revenue
    FROM Order o
    WHERE o.status = com.c05.kaz.ecommercebackend.enums.OrderStatus.COMPLETED
      AND YEAR(o.createdAt) = :year
    GROUP BY MONTH(o.createdAt)
    ORDER BY month ASC
""")
    List<Object[]> getMonthlyRevenue(int year);

}
