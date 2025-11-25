package com.c05.kaz.ecommercebackend.dto.chat;

import lombok.Data;

@Data
public class ChatRoomDTO {
    private Long roomId;
    private Long customerId;
    private Long supplierId;
    private String lastMessage;
    private Long lastSenderId;
    private Integer customerUnread;
    private Integer supplierUnread;
    private String updatedAt;
}

