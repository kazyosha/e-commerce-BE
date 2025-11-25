package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.chat.request.SendMessageRequest;
import com.c05.kaz.ecommercebackend.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{roomId}")
    public void handleMessage(
            @DestinationVariable Long roomId,
            @Payload SendMessageRequest req
    ) {
        // Lưu DB + gửi WebSocket ra FE
        chatService.sendMessage(roomId, req);
    }
}
