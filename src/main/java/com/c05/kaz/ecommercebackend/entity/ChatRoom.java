package com.c05.kaz.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ================================
    // Quan hệ với CustomerProfile
    // ================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    // ================================
    // Quan hệ với SupplierShop
    // ================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierShop supplier;

    // optionally gắn với đơn hàng
    @Column(name = "order_id")
    private Long orderId;

    // lưu để FE load nhanh
    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_sender_id")
    private Long lastSenderId;

    // số tin chưa đọc của customer
    @Column(nullable = false)
    private Integer customerUnread = 0;

    // số tin chưa đọc của supplier
    @Column(nullable = false)
    private Integer supplierUnread = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
