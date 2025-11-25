package com.c05.kaz.ecommercebackend.dto.chat;

import lombok.Data;

@Data
public class ChatMessageDTO {
    private Long id;
    private Long roomId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String imageUrl;
    private String status;
    private String createdAt;
}
