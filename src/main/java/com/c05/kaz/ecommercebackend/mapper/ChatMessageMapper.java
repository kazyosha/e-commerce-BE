package com.c05.kaz.ecommercebackend.mapper;

import com.c05.kaz.ecommercebackend.dto.chat.ChatMessageDTO;
import com.c05.kaz.ecommercebackend.entity.ChatMessage;

public class ChatMessageMapper {

    public static ChatMessageDTO toDTO(ChatMessage msg) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(msg.getId());
        dto.setRoomId(msg.getRoom().getId());
        dto.setSenderId(msg.getSenderId());
        dto.setReceiverId(msg.getReceiverId());
        dto.setContent(msg.getContent());
        dto.setImageUrl(msg.getImageUrl());
        dto.setStatus(msg.getStatus().name());
        dto.setCreatedAt(msg.getCreatedAt().toString());
        return dto;
    }
}