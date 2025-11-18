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
}
