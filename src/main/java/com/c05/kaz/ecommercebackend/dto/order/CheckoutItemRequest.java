package com.c05.kaz.ecommercebackend.dto.order;

import lombok.Data;

@Data
public class CheckoutItemRequest {
    private Long cartItemId;
    private Integer quantity; // số lượng muốn mua (có thể = quantity trong cart)
}
