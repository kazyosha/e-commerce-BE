package com.c05.kaz.ecommercebackend.entity;

import com.c05.kaz.ecommercebackend.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // người nhận: cả khách + nhà cung cấp + admin...
    @ManyToOne(optional = false)
    @JoinColumn(name = "receiver_id")
    private UserAccount receiver;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(length = 255)
    private String title;

    @Column(length = 2000)
    private String content;

    private boolean readFlag;

    // tham chiếu mềm
    private Long relatedOrderId;
    private Long relatedProductId;
    private Long relatedPromotionId;

    private LocalDateTime createdAt;
}

