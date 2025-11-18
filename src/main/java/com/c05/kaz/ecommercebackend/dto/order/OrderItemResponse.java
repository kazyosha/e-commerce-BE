package com.c05.kaz.ecommercebackend.dto.order;

import com.c05.kaz.ecommercebackend.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String thumbnail;
    private Long unitPrice;
    private Integer quantity;
    private Long lineTotal;

    public static OrderItemResponse fromEntity(OrderItem oi) {
        Long thumb = null;
        String thumbnail = null;
        if (oi.getProduct() != null
                && oi.getProduct().getImages() != null
                && !oi.getProduct().getImages().isEmpty()) {
            thumbnail = oi.getProduct().getImages().get(0).getImageUrl();
        }

        // Nếu entity OrderItem đã có field lineTotal
        Long lineTotal = oi.getLineTotal();
        // fallback: tự tính nếu null
        if (lineTotal == null && oi.getUnitPrice() != null && oi.getQuantity() != null) {
            lineTotal = oi.getUnitPrice() * oi.getQuantity(); // unitPrice là Long, quantity là Integer
        }

        return OrderItemResponse.builder()
                .id(oi.getId())
                .productId(oi.getProduct().getId())
                .productName(oi.getProduct().getName())
                .thumbnail(thumbnail)
                .unitPrice(oi.getUnitPrice())
                .quantity(oi.getQuantity())
                .lineTotal(lineTotal)
                .build();
    }
}
