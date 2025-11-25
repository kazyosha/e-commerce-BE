package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.chat.ChatMessageDTO;
import com.c05.kaz.ecommercebackend.dto.chat.ChatRoomDTO;
import com.c05.kaz.ecommercebackend.dto.chat.request.MarkReadRequest;
import com.c05.kaz.ecommercebackend.dto.chat.request.OpenRoomRequest;
import com.c05.kaz.ecommercebackend.dto.chat.request.SendMessageRequest;
import com.c05.kaz.ecommercebackend.entity.ChatMessage;
import com.c05.kaz.ecommercebackend.entity.ChatRoom;
import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.MessageStatus;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.mapper.ChatMessageMapper;
import com.c05.kaz.ecommercebackend.mapper.ChatRoomMapper;
import com.c05.kaz.ecommercebackend.repository.ChatMessageRepository;
import com.c05.kaz.ecommercebackend.repository.ChatRoomRepository;
import com.c05.kaz.ecommercebackend.repository.CustomerRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository roomRepo;
    private final ChatMessageRepository msgRepo;
    private final UserAccountRepository userRepo;
    private final CustomerRepository customerRepo;
    private final SupplierRepository supplierRepo;
    private final SimpMessagingTemplate websocket;

    // ================================================
    // 🔥 GET CURRENT USER
    // ================================================
    private UserAccount getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    // ================================================
    // 🔥 GET ROOMS OF CURRENT USER
    // ================================================
    public List<ChatRoomDTO> getRoomsOfCurrentUser() {
        UserAccount user = getCurrentUser();

        List<ChatRoom> rooms = user.getUserType() == UserType.CUSTOMER
                ? roomRepo.findByCustomerId(user.getId())
                : roomRepo.findBySupplierId(user.getId());

        return rooms.stream().map(ChatRoomMapper::toDTO).toList();
    }

    // ================================================
    // 🔥 OPEN OR CREATE ROOM
    // ================================================
    @Transactional
    public ChatRoomDTO openRoom(OpenRoomRequest req) {

        ChatRoom room = roomRepo
                .findByCustomerIdAndSupplierIdAndOrderId(
                        req.getCustomerId(),
                        req.getSupplierId(),
                        req.getOrderId()
                )
                .orElse(null);

        if (room != null) {
            return ChatRoomMapper.toDTO(room);
        }

        // ⭐ Lấy customer qua user_id = req.customerId
        CustomerProfile customer = customerRepo.findByUser_Id(req.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // ⭐ Lấy supplier qua user_id = req.supplierId
        SupplierShop supplier = supplierRepo.findByUserId(req.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        ChatRoom newRoom = ChatRoom.builder()
                .customer(customer)
                .supplier(supplier)
                .orderId(req.getOrderId())
                .lastMessage("")
                .customerUnread(0)
                .supplierUnread(0)
                .build();

        roomRepo.save(newRoom);

        return ChatRoomMapper.toDTO(newRoom);
    }

    // ================================================
    // 🔥 LOAD MESSAGES
    // ================================================
    public Page<ChatMessageDTO> getMessages(Long roomId, int page, int size) {
        return msgRepo.findByRoom_IdOrderByCreatedAtAsc(roomId, PageRequest.of(page, size))
                .map(ChatMessageMapper::toDTO);
    }

    // ================================================
    // 🔥 SEND MESSAGE
    // ================================================
    @Transactional
    public ChatMessageDTO sendMessage(Long roomId, SendMessageRequest req) {

        ChatRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .senderId(req.getSenderId())
                .receiverId(req.getReceiverId())
                .content(req.getContent())
                .imageUrl(req.getImageUrl())
                .status(MessageStatus.SENT)
                .build();

        message = msgRepo.save(message);

        // cập nhật room
        room.setLastMessage(req.getContent() == null || req.getContent().isBlank() ? "[Image]" : req.getContent());
        room.setLastSenderId(req.getSenderId());

        if (req.getSenderId().equals(room.getCustomer().getId())) {
            room.setSupplierUnread(room.getSupplierUnread() + 1);
        } else {
            room.setCustomerUnread(room.getCustomerUnread() + 1);
        }

        roomRepo.save(room);

        // gửi message real-time
        websocket.convertAndSend("/topic/chat/" + roomId, ChatMessageMapper.toDTO(message));

        return ChatMessageMapper.toDTO(message);
    }

    // ================================================
    // 🔥 MARK AS READ
    // ================================================
    @Transactional
    public void markAsRead(Long roomId, MarkReadRequest req) {

        ChatRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (req.getReaderId().equals(room.getCustomer().getId())) {
            room.setCustomerUnread(0);
        } else {
            room.setSupplierUnread(0);
        }

        roomRepo.save(room);

        websocket.convertAndSend("/topic/chat/" + roomId + "/read", req);
    }

    // ================================================
    // 🔥 TOTAL UNREAD COUNT
    // ================================================
    public Integer getUnreadCount() {
        UserAccount user = getCurrentUser();

        List<ChatRoom> rooms = user.getUserType() == UserType.CUSTOMER
                ? roomRepo.findByCustomerId(user.getId())
                : roomRepo.findBySupplierId(user.getId());

        return rooms.stream().mapToInt(room ->
                user.getUserType() == UserType.CUSTOMER
                        ? room.getCustomerUnread()
                        : room.getSupplierUnread()
        ).sum();
    }
}
