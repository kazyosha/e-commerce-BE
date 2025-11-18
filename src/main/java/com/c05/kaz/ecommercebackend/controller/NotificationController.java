package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.entity.Notification;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.repository.NotificationRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository notificationRepo;
    private final UserAccountRepository userRepo;

    // Lấy user hiện tại từ SecurityContext
    private UserAccount getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập");
        }

        String username = auth.getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
    }

    @GetMapping("/me")
    public ResponseEntity<List<Notification>> getMyNotifications(
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly
    ) {
        UserAccount user = getCurrentUser();

        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepo.findByReceiverAndReadFlagFalse(user);
        } else {
            notifications = notificationRepo.findByReceiverOrderByCreatedAtDesc(user);
        }

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        UserAccount user = getCurrentUser();
        long count = notificationRepo.countByReceiverAndReadFlagFalse(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        UserAccount user = getCurrentUser();

        Notification noti = notificationRepo.findByIdAndReceiver(id, user)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"));

        if (!noti.isReadFlag()) {
            noti.setReadFlag(true);
            notificationRepo.save(noti);
        }

        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu đã đọc"));
    }

    @PostMapping("/me/read-all")
    public ResponseEntity<?> markAllAsRead() {
        UserAccount user = getCurrentUser();

        List<Notification> unreadList = notificationRepo.findByReceiverAndReadFlagFalse(user);
        if (!unreadList.isEmpty()) {
            unreadList.forEach(n -> n.setReadFlag(true));
            notificationRepo.saveAll(unreadList);
        }

        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu tất cả thông báo là đã đọc"));
    }
}
