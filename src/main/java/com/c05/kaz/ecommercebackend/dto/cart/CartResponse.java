package com.c05.kaz.ecommercebackend.dto.cart;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponse {

    private List<CartItemResponse> items;
    private Long totalAmount;  // tổng tiền
    private Integer totalItems; // tổng số sản phẩm (tính theo quantity)
}
