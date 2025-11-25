package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.Notification;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.NotificationType;
import com.c05.kaz.ecommercebackend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;  // <── THÊM DÒNG NÀY

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notiRepo;

    public void notifyOrderCreatedForSupplier(SupplierShop supplier, UserAccount customer, Long orderId, Long totalPrice) {

        UserAccount receiver = supplier.getUser();  // Chủ shop nhận thông báo

        Notification noti = Notification.builder()
                .receiver(receiver)
                .type(NotificationType.ORDER_CREATED)   // bạn nên thêm enum này
                .title("Bạn có đơn hàng mới #" + orderId)
                .content("Khách hàng " + customer.getUsername()
                        + " vừa đặt đơn hàng #" + orderId
                        + " với tổng tiền " + totalPrice + "đ.")
                .relatedOrderId(orderId)
                .readFlag(false)
                .createdAt(LocalDateTime.now())
                .build();

        notiRepo.save(noti);
    }

    public void notifyOrderCreatedForCustomer(UserAccount customer, Long orderId) {
        try {
            Notification noti = Notification.builder()
                    .receiver(customer)
                    .type(NotificationType.ORDER_CREATED)
                    .title("Đặt hàng thành công #" + orderId)
                    .content("Bạn đã đặt đơn hàng #" + orderId + " thành công. "
                            + "Đơn hàng đang chờ shop xác nhận.")
                    .relatedOrderId(orderId)
                    .readFlag(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notiRepo.save(noti);

        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo đặt hàng mới cho customer: " + ex.getMessage());
        }
    }

    /**
     * Gửi thông báo đơn hàng bị hủy cho nhà cung cấp
     */
    public void notifyOrderCancelledForSupplier(SupplierShop supplier, Long orderId) {

        UserAccount receiver = supplier.getUser(); // supplier → useraccount

        Notification noti = Notification.builder()
                .receiver(receiver)
                .type(NotificationType.ORDER_CANCELLED)
                .title("Đơn hàng #" + orderId + " đã bị hủy")
                .content("Khách hàng đã hủy đơn hàng #" + orderId +
                        " sau khi cửa hàng đã xác nhận. Vui lòng kiểm tra lại đơn hàng.")
                .relatedOrderId(orderId)
                .readFlag(false)
                .createdAt(LocalDateTime.now())
                .build();

        notiRepo.save(noti);
    }

    public void notifyOrderConfirmedForCustomer(UserAccount customer, Long orderId) {
        try {
            Notification noti = Notification.builder()
                    .receiver(customer)
                    .type(NotificationType.ORDER_CONFIRMED)
                    .title("Đơn hàng #" + orderId + " đã được xác nhận")
                    .content("Shop đã xác nhận đơn hàng #" + orderId + ". Đang chuẩn bị giao hàng.")
                    .relatedOrderId(orderId)
                    .createdAt(LocalDateTime.now())
                    .readFlag(false)
                    .build();

            notiRepo.save(noti);
        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo xác nhận đơn cho customer: " + ex.getMessage());
        }
    }

    public void notifyOrderShippingForCustomer(UserAccount customer, Long orderId) {
        try {
            Notification noti = Notification.builder()
                    .receiver(customer)
                    .type(NotificationType.ORDER_SHIPPING)
                    .title("Đơn hàng #" + orderId + " đang được giao")
                    .content("Shop đã bàn giao đơn hàng #" + orderId + " cho đơn vị vận chuyển.")
                    .relatedOrderId(orderId)
                    .createdAt(LocalDateTime.now())
                    .readFlag(false)
                    .build();

            notiRepo.save(noti);
        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo SHIPPING cho customer: " + ex.getMessage());
        }
    }

    public void notifyOrderCompletedForSupplier(SupplierShop supplier, Long orderId, UserAccount customer) {
        try {
            Notification noti = Notification.builder()
                    .receiver(supplier.getUser())  // chủ shop
                    .type(NotificationType.ORDER_COMPLETED)
                    .title("Đơn hàng #" + orderId + " đã giao thành công")
                    .content("Khách hàng " + customer.getUsername() + " đã xác nhận đã nhận đơn hàng #" + orderId + ".")
                    .relatedOrderId(orderId)
                    .createdAt(LocalDateTime.now())
                    .readFlag(false)
                    .build();

            notiRepo.save(noti);
        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo hoàn tất đơn cho supplier: " + ex.getMessage());
        }
    }

    public void notifyReviewCreatedForSupplier(SupplierShop supplier, UserAccount customer, Long productId, Long reviewId) {
        try {
            Notification noti = Notification.builder()
                    .receiver(supplier.getUser()) // chủ shop
                    .type(NotificationType.REVIEW_CREATED)
                    .title("Sản phẩm của bạn vừa được đánh giá")
                    .content("Khách hàng " + customer.getUsername() + " đã đánh giá sản phẩm của shop.")
                    .relatedReviewId(reviewId)
                    .relatedProductId(productId)
                    .createdAt(LocalDateTime.now())
                    .readFlag(false)
                    .build();

            notiRepo.save(noti);

        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo REVIEW_CREATED cho supplier: " + ex.getMessage());
        }
    }

    public void notifyReviewRepliedForCustomer(UserAccount customer, Long productId, Long reviewId) {
        try {
            Notification noti = Notification.builder()
                    .receiver(customer)
                    .type(NotificationType.REVIEW_REPLIED)
                    .title("Nhà cung cấp đã phản hồi đánh giá của bạn")
                    .content("Shop đã phản hồi đánh giá của bạn về sản phẩm.")
                    .relatedReviewId(reviewId)
                    .relatedProductId(productId)
                    .createdAt(LocalDateTime.now())
                    .readFlag(false)
                    .build();

            notiRepo.save(noti);

        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo REVIEW_REPLIED cho customer: " + ex.getMessage());
        }
    }

    public void notifyOrderRejectedForCustomer(UserAccount customer, Long orderId, String reasonMessage) {
        try {
            Notification noti = Notification.builder()
                    .receiver(customer)
                    .type(NotificationType.ORDER_REJECTED)
                    .title("Đơn hàng #" + orderId + " bị từ chối")
                    .content(reasonMessage)
                    .relatedOrderId(null)
                    .createdAt(LocalDateTime.now())
                    .readFlag(false)
                    .build();

            notiRepo.save(noti);
        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo cho customer: " + ex.getMessage());
        }
    }

}
