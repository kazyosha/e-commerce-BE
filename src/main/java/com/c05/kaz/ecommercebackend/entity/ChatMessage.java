package com.c05.kaz.ecommercebackend.entity;

import com.c05.kaz.ecommercebackend.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ================================
    // Quan hệ với ChatRoom
    // ================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    // Người gửi (có thể là CustomerProfile hoặc SupplierShop →
    // chỉ lưu user_id để đơn giản hóa)
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    // Người nhận
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;


    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = MessageStatus.SENT;
        }
    }
}
