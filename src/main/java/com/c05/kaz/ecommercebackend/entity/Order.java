package com.c05.kaz.ecommercebackend.entity;

import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import com.c05.kaz.ecommercebackend.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    private SupplierShop supplier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    // ====== Payment info ======
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private boolean paid;

    @Column(length = 100)
    private String paymentTransactionId;

    @Column(length = 100)
    private String bankCode;

    // ====== Receiver info ======
    @Column(nullable = false, length = 150)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String receiverPhone;

    @Column(nullable = false, length = 255)
    private String receiverAddress;

    private String invoiceNo;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private Long originalTotal;
    private Long discountAmount;
    private Long finalTotal;

    @Column(name = "shipping_fee")
    private Long shippingFee;

    // ⭐ Thay cho promotion cũ
    @Column(name = "discount_code", length = 50)
    private String discountCode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Double totalPrice;
}
