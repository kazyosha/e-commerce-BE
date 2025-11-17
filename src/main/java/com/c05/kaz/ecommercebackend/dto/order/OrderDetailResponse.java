package com.c05.kaz.ecommercebackend.dto.order;

import com.c05.kaz.ecommercebackend.entity.Order;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailResponse {

    private Long id;
    private String supplierName;
    private LocalDateTime createdAt;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private boolean paid;

    private Long originalTotal;
    private Long discountAmount;
    private Long finalTotal;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    private List<OrderItemResponse> items;

    public static OrderDetailResponse fromEntity(Order order) {
        OrderDetailResponse dto = new OrderDetailResponse();
        dto.setId(order.getId());
        dto.setSupplierName(
                order.getSupplier() != null ? order.getSupplier().getShopName() : null
        );
        dto.setCreatedAt(order.getCreatedAt());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setPaid(order.isPaid());

        dto.setOriginalTotal(order.getOriginalTotal());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setFinalTotal(order.getFinalTotal());

        dto.setReceiverName(order.getReceiverName());
        dto.setReceiverPhone(order.getReceiverPhone());
        dto.setReceiverAddress(order.getReceiverAddress());

        dto.setItems(
                order.getItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .toList()
        );

        return dto;
    }
}
