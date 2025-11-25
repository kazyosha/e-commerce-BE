package com.c05.kaz.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "discount_usage_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    private Long reducedAmount;

    private LocalDateTime usedAt;
}
