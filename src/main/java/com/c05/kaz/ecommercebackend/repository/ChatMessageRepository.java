package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.ChatMessage;
import com.c05.kaz.ecommercebackend.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByRoom_IdOrderByCreatedAtAsc(Long roomId, Pageable pageable);}