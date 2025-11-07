package com.c05.kaz.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_customer_product",
                columnNames = {"customer_id", "product_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1 khách / 1 sản phẩm / 1 shop: 1 review
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private SupplierShop supplier;

    private Integer rating; // sao

    @Column(length = 2000)
    private String comment;

    // phản hồi từ shop
    @Column(length = 2000)
    private String supplierReply;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

