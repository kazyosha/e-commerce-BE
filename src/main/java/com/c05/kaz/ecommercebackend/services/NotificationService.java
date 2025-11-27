package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.Notification;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.NotificationType;
import com.c05.kaz.ecommercebackend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notiRepo;
    private final SimpMessagingTemplate messaging;

    private void push(UserAccount receiver, Notification noti) {
        try {
            messaging.convertAndSend(
                    "/topic/notifications/" + receiver.getId(),
                    Map.of(
                            "id", noti.getId(),
                            "title", noti.getTitle(),
                            "content", noti.getContent(),
                            "type", noti.getType(),
                            "relatedOrderId", noti.getRelatedOrderId(),
                            "relatedProductId", noti.getRelatedProductId(),
                            "relatedReviewId", noti.getRelatedReviewId(),
                            "readFlag", noti.isReadFlag(),
                            "createdAt", noti.getCreatedAt().toString()
                    )
            );
        } catch (Exception e) {
            log.error("❌ Lỗi gửi realtime notification: " + e.getMessage());
        }
    }

    public void notifyOrderCreatedForSupplier(SupplierShop supplier, UserAccount customer, Long orderId, Long totalPrice) {

        UserAccount receiver = supplier.getUser();

        Notification noti = Notification.builder()
                .receiver(receiver)
                .type(NotificationType.ORDER_CREATED)
                .title("Bạn có đơn hàng mới #" + orderId)
                .content("Khách " + customer.getUsername()
                        + " vừa đặt đơn #" + orderId
                        + " • Tổng tiền: " + totalPrice + "đ")
                .relatedOrderId(orderId)
                .createdAt(LocalDateTime.now())
                .readFlag(false)
                .build();

        notiRepo.save(noti);
        push(receiver, noti);
    }

    public void notifyOrderCreatedForCustomer(UserAccount customer, Long orderId) {
        Notification noti = Notification.builder()
                .receiver(customer)
                .type(NotificationType.ORDER_CREATED)
                .title("Đặt hàng thành công #" + orderId)
                .content("Đơn hàng #" + orderId + " của bạn đã được tạo.")
                .relatedOrderId(orderId)
                .createdAt(LocalDateTime.now())
                .readFlag(false)
                .build();

        notiRepo.save(noti);
        push(customer, noti);
    }

    public void notifyOrderCancelledForSupplier(SupplierShop supplier, Long orderId) {
        UserAccount receiver = supplier.getUser();

        Notification noti = Notification.builder()
                .receiver(receiver)
                .type(NotificationType.ORDER_CANCELLED)
                .title("Đơn hàng #" + orderId + " bị hủy")
                .content("Khách hàng đã hủy đơn #" + orderId)
                .relatedOrderId(orderId)
                .createdAt(LocalDateTime.now())
                .readFlag(false)
                .build();

        notiRepo.save(noti);
        push(receiver, noti);
    }

    public void notifyOrderConfirmedForCustomer(UserAccount customer, Long orderId) {
        Notification noti = Notification.builder()
                .receiver(customer)
                .type(NotificationType.ORDER_CONFIRMED)
                .title("Đơn hàng #" + orderId + " đã được xác nhận")
                .content("Shop đã xác nhận đơn hàng của bạn.")
                .relatedOrderId(orderId)
                .createdAt(LocalDateTime.now())
                .readFlag(false)
                .build();

        notiRepo.save(noti);
        push(customer, noti);
    }

    public void notifyOrderShippingForCustomer(UserAccount customer, Long orderId) {
        Notification noti = Notification.builder()
                .receiver(customer)
                .type(NotificationType.ORDER_SHIPPING)
                .title("Đơn #" + orderId + " đang giao")
                .content("Đơn hàng của bạn đã được bàn giao cho đơn vị vận chuyển.")
                .relatedOrderId(orderId)
                .createdAt(LocalDateTime.now())
                .readFlag(false)
                .build();

        notiRepo.save(noti);
        push(customer, noti);
    }

    public void notifyOrderCompletedForSupplier(SupplierShop supplier, Long orderId, UserAccount customer) {
        UserAccount receiver = supplier.getUser();

        Notification noti = Notification.builder()
                .receiver(receiver)
                .type(NotificationType.ORDER_COMPLETED)
                .title("Đơn #" + orderId + " đã giao thành công")
                .content("Khách hàng " + customer.getUsername() + " đã xác nhận đã nhận hàng.")
                .relatedOrderId(orderId)
                .createdAt(LocalDateTime.now())
                .readFlag(false)
                .build();

        notiRepo.save(noti);
        push(receiver, noti);
    }

    public void notifyReviewCreatedForSupplier(SupplierShop supplier, UserAccount customer, Long productId, Long reviewId) {
        UserAccount receiver = supplier.getUser();

        Notification noti = Notification.builder()
                .receiver(receiver)
                .type(NotificationType.REVIEW_CREATED)
                .title("Sản phẩm của bạn vừa được đánh giá")
                .content("Khách " + customer.getUsername() + " đã đánh giá sản phẩm.")
                .relatedProductId(productId)
                .relatedReviewId(reviewId)
                .createdAt(LocalDateTime.now())
                .readFlag(false)
                .build();

        notiRepo.save(noti);
        push(receiver, noti);
    }

    public void notifyReviewRepliedForCustomer(UserAccount customer, Long productId, Long reviewId) {
        Notification noti = Notification.builder()
                .receiver(customer)
                .type(NotificationType.REVIEW_REPLIED)
                .title("Shop đã phản hồi đánh giá của bạn")
                .content("Bạn có phản hồi mới cho đánh giá của mình.")
                .relatedProductId(productId)
                .relatedReviewId(reviewId)
                .createdAt(LocalDateTime.now())
                .readFlag(false)
                .build();

        notiRepo.save(noti);
        push(customer, noti);
    }

    public void notifyOrderRejectedForCustomer(UserAccount customer, Long orderId, String reasonMessage) {
        Notification noti = Notification.builder()
                .receiver(customer)
                .type(NotificationType.ORDER_REJECTED)
                .title("Đơn #" + orderId + " bị từ chối")
                .content(reasonMessage)
                .relatedOrderId(orderId)
                .createdAt(LocalDateTime.now())
                .readFlag(false)
                .build();

        notiRepo.save(noti);
        push(customer, noti);
    }

    public void pushOrderStatusToCustomer(Long customerId, Long orderId, String status) {
        messaging.convertAndSend(
                "/topic/orders/customer/" + customerId,
                Map.of(
                        "orderId", orderId,
                        "status", status,
                        "timestamp", LocalDateTime.now().toString()
                )
        );
    }

    public void pushOrderStatusToSupplier(Long supplierId, Long orderId, String status) {
        messaging.convertAndSend(
                "/topic/orders/supplier/" + supplierId,
                Map.of(
                        "orderId", orderId,
                        "status", status,
                        "timestamp", LocalDateTime.now().toString()
                )
        );
    }

}
