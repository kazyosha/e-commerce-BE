package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.chat.ChatMessageDTO;
import com.c05.kaz.ecommercebackend.dto.chat.ChatRoomDTO;
import com.c05.kaz.ecommercebackend.dto.chat.request.MarkReadRequest;
import com.c05.kaz.ecommercebackend.dto.chat.request.OpenRoomRequest;
import com.c05.kaz.ecommercebackend.dto.chat.request.SendMessageRequest;
import com.c05.kaz.ecommercebackend.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    @GetMapping("/rooms")
    public List<ChatRoomDTO> getMyRooms() {
        return chatService.getRoomsOfCurrentUser();
    }


    @PostMapping("/rooms/open")
    public ChatRoomDTO openRoom(@RequestBody OpenRoomRequest request) {
        return chatService.openRoom(request);
    }


    @GetMapping("/rooms/{roomId}/messages")
    public Page<ChatMessageDTO> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return chatService.getMessages(roomId, page, size);
    }


    @PostMapping("/rooms/{roomId}/messages")
    public ChatMessageDTO sendMessage(
            @PathVariable Long roomId,
            @RequestBody SendMessageRequest request
    ) {
        return chatService.sendMessage(roomId, request);
    }


    @PostMapping("/rooms/{roomId}/read")
    public void markAsRead(
            @PathVariable Long roomId,
            @RequestBody MarkReadRequest request
    ) {
        chatService.markAsRead(roomId, request);
    }

    @GetMapping("/unread-count")
    public Integer getUnreadCount() {
        return chatService.getUnreadCount();
    }
}