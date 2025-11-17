package com.c05.kaz.ecommercebackend.dto.order;

import com.c05.kaz.ecommercebackend.entity.OrderItem;
import lombok.Data;

@Data
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Long unitPrice;
    private Long totalPrice;

    public static OrderItemResponse fromEntity(OrderItem item) {
        OrderItemResponse dto = new OrderItemResponse();
        dto.setId(item.getId());

        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());

            // 🔥 Lấy ảnh thumbnail đầu tiên
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                dto.setProductImage(item.getProduct().getImages().get(0).getImageUrl());
            } else {
                dto.setProductImage(null);
            }

        } else {
            dto.setProductName("Sản phẩm không tồn tại");
            dto.setProductImage(null);
        }

        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());

        long total = (item.getUnitPrice() != null && item.getQuantity() != null)
                ? item.getUnitPrice() * item.getQuantity()
                : 0;

        dto.setTotalPrice(total);

        return dto;
    }

}
