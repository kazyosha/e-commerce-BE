package com.c05.kaz.ecommercebackend.dto.order;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderSummaryResponse {

    private Long orderId;
    private Long supplierId;
    private String supplierName;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    private Long originalTotal;
    private Long discountAmount;
    private Long shippingFee;
    private Long finalTotal;

    private String discountCode;
    private String status;
    private LocalDateTime createdAt;

    private List<OrderItemSummary> items;

    // ⭐ FIX: thêm constructor public đầy đủ fields
    public OrderSummaryResponse(
            Long orderId,
            Long supplierId,
            String supplierName,
            String receiverName,
            String receiverPhone,
            String receiverAddress,
            Long originalTotal,
            Long discountAmount,
            Long shippingFee,
            Long finalTotal,
            String discountCode,
            String status,
            LocalDateTime createdAt,
            List<OrderItemSummary> items
    ) {
        this.orderId = orderId;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.originalTotal = originalTotal;
        this.discountAmount = discountAmount;
        this.shippingFee = shippingFee;
        this.finalTotal = finalTotal;
        this.discountCode = discountCode;
        this.status = status;
        this.createdAt = createdAt;
        this.items = items;
    }
}
