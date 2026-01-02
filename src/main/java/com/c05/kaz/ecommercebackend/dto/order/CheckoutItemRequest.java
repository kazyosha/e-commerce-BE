package com.c05.kaz.ecommercebackend.dto.order;

import lombok.Data;

@Data
public class CheckoutItemRequest {
    private Long productId;
    private Long cartItemId;
    private Integer quantity;
}
