package com.c05.kaz.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // mỗi room giữa 1 customer & 1 shop
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private SupplierShop supplier;

    private LocalDateTime createdAt;
}

