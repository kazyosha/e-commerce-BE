package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.ChatRoom;
import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByCustomerAndSupplier(CustomerProfile customer, SupplierShop supplier);
}