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
}
