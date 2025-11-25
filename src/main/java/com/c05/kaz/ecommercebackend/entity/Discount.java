package com.c05.kaz.ecommercebackend.entity;

import com.c05.kaz.ecommercebackend.enums.DiscountStatus;
import com.c05.kaz.ecommercebackend.enums.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "discounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private SupplierShop supplier;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountType type;      // PERCENT hoặc AMOUNT

    private Double value;           // 30%, 50000đ

    private Long minOrderValue;

    private Long maxDiscountAmount; // chỉ áp dụng cho % (optional)

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private DiscountStatus status;

    private Integer totalUsage;     // số lượt

    private Integer usedCount;

    private Integer limitPerUser;

    @ManyToMany
    @JoinTable(
            name = "discount_products",
            joinColumns = @JoinColumn(name = "discount_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> applicableProducts;
}

