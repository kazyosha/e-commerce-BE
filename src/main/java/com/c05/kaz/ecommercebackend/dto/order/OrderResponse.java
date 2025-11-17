package com.c05.kaz.ecommercebackend.dto.order;

import com.c05.kaz.ecommercebackend.entity.Order;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderResponse {

    private Long id;
    private String supplierName;
    private LocalDateTime createdAt;
    private String status;
    private String paymentMethod;
    private boolean paid;
    private Long originalTotal;
    private Long discountAmount;
    private Long finalTotal;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    public static OrderResponse fromEntity(Order order) {
        OrderResponse dto = new OrderResponse();
        dto.setId(order.getId());
        dto.setSupplierName(
                order.getSupplier() != null ? order.getSupplier().getShopName() : null
        );
        dto.setCreatedAt(order.getCreatedAt());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setPaid(order.isPaid());

        dto.setOriginalTotal(order.getOriginalTotal());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setFinalTotal(order.getFinalTotal());

        dto.setReceiverName(order.getReceiverName());
        dto.setReceiverPhone(order.getReceiverPhone());
        dto.setReceiverAddress(order.getReceiverAddress());

        return dto;
    }
}
