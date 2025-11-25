package com.c05.kaz.ecommercebackend.dto.chat.request;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Long senderId;
    private Long receiverId;
    private String content;
    private String imageUrl;
}
