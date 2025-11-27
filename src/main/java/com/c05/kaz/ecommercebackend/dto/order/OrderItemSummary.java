package com.c05.kaz.ecommercebackend.dto.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemSummary {

    private Long productId;
    private String productName;
    private String thumbnail;
    private Long unitPrice;
    private Integer quantity;
    private Long lineTotal;

    // ⭐ FIX: thêm constructor public đầy đủ
    public OrderItemSummary(
            Long productId,
            String productName,
            String thumbnail,
            Long unitPrice,
            Integer quantity,
            Long lineTotal
    ) {
        this.productId = productId;
        this.productName = productName;
        this.thumbnail = thumbnail;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }
}

