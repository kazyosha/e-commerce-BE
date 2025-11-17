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
    private Long finalTotal;

    private String status;
    private LocalDateTime createdAt;

    private List<OrderItemSummary> items;
}
