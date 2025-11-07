package com.c05.kaz.ecommercebackend.entity;

import com.c05.kaz.ecommercebackend.enums.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private SupplierShop supplier;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private Double discountValue;

    private Long minOrderValue;

    private Integer maxUsage; // tổng số lần dùng
    private Integer usedCount;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private boolean active;
}

