// src/main/java/com/c05/kaz/ecommercebackend/entity/CartItem.java
package com.c05.kaz.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // Giá tại thời điểm thêm vào giỏ (optional)
    @Column(nullable = false)
    private Long priceAtAdd;
}
