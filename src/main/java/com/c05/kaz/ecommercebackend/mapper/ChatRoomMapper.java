package com.c05.kaz.ecommercebackend.mapper;

import com.c05.kaz.ecommercebackend.dto.chat.ChatRoomDTO;
import com.c05.kaz.ecommercebackend.entity.ChatRoom;

public class ChatRoomMapper {

    public static ChatRoomDTO toDTO(ChatRoom room) {
        ChatRoomDTO dto = new ChatRoomDTO();
        dto.setRoomId(room.getId());
        dto.setCustomerId(room.getCustomer().getId());
        dto.setSupplierId(room.getSupplier().getId());
        dto.setLastMessage(room.getLastMessage());
        dto.setLastSenderId(room.getLastSenderId());
        dto.setCustomerUnread(room.getCustomerUnread());
        dto.setSupplierUnread(room.getSupplierUnread());
        dto.setUpdatedAt(room.getUpdatedAt().toString());
        return dto;
    }
}