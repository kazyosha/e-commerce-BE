package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.ChatRoom;
import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByCustomerId(Long customerId);

    List<ChatRoom> findBySupplierId(Long supplierId);

    Optional<ChatRoom> findByCustomerIdAndSupplierId(Long customerId, Long supplierId);

    Optional<ChatRoom> findByCustomerIdAndSupplierIdAndOrderId(Long customerId, Long supplierId, Long orderId);
}