package com.c05.kaz.ecommercebackend.entity;

import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // mỗi đơn thuộc 1 shop (multi-vendor)
    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private SupplierShop supplier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status; // PENDING, REJECTED, CANCELED, COMPLETED

    // thông tin nhận hàng (copy từ profile, cho phép chỉnh)
    @Column(nullable = false, length = 150)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String receiverPhone;

    @Column(nullable = false, length = 255)
    private String receiverAddress;

    private String invoiceNo;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private Long originalTotal;   // tổng trước giảm
    private Long discountAmount;  // tổng giảm
    private Long finalTotal;      // tổng cuối

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;  // mã đã áp, nếu có

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

