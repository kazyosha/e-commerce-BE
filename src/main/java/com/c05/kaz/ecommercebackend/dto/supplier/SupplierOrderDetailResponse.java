package com.c05.kaz.ecommercebackend.dto.supplier;

import com.c05.kaz.ecommercebackend.entity.Order;
import com.c05.kaz.ecommercebackend.entity.OrderItem;
import com.c05.kaz.ecommercebackend.entity.ProductImage;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplierOrderDetailResponse {

    private Long orderId;
    private String status;
    private LocalDateTime createdAt;

    private CustomerInfo customer;

    private List<ItemInfo> items;

    private Long originalTotal;
    private Long discountAmount;
    private Long finalTotal;

    private String paymentMethod;
    private boolean paid;

    /* ============================================
        CONVERTER TỪ Order → DTO
    ============================================ */
    public static SupplierOrderDetailResponse from(Order order) {

        return SupplierOrderDetailResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())

                .customer(new CustomerInfo(
                        order.getReceiverName(),
                        order.getReceiverPhone(),
                        order.getReceiverAddress()
                ))

                .items(order.getItems()
                        .stream()
                        .map(ItemInfo::fromOrderItem)
                        .toList())

                .originalTotal(order.getOriginalTotal())
                .discountAmount(order.getDiscountAmount())
                .finalTotal(order.getFinalTotal())

                .paymentMethod(order.getPaymentMethod().name())
                .paid(order.isPaid())

                .build();
    }

    /* ============================================
        CUSTOMER INFO DTO
    ============================================ */
    @Data
    @AllArgsConstructor
    public static class CustomerInfo {
        private String name;
        private String phone;
        private String address;
    }

    /* ============================================
        ITEM DTO
    ============================================ */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ItemInfo {
        private Long productId;
        private String productName;
        private String thumbnail;
        private int quantity;
        private Long unitPrice;
        private Long lineTotal;

        public static ItemInfo fromOrderItem(OrderItem item) {

            // Lấy ảnh sản phẩm: ưu tiên ảnh đầu tiên
            String imgUrl = null;
            try {
                List<ProductImage> images = item.getProduct().getImages();
                if (images != null && !images.isEmpty()) {
                    imgUrl = images.get(0).getImageUrl();
                }
            } catch (Exception ignored) {}

            return ItemInfo.builder()
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .thumbnail(imgUrl)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(item.getUnitPrice() * item.getQuantity())
                    .build();
        }
    }
}