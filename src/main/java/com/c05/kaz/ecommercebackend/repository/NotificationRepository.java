package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.Notification;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverOrderByCreatedAtDesc(UserAccount receiver);

    long countByReceiverAndReadFlagFalse(UserAccount receiver);

    Optional<Notification> findByIdAndReceiver(Long id, UserAccount receiver);

    List<Notification> findByReceiverAndReadFlagFalse(UserAccount receiver);
}
