package com.c05.kaz.ecommercebackend.dto.cart;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemResponse {

    private Long itemId;

    private Long productId;
    private String productName;
    private String thumbnail;

    private Long price;       // giá 1 sản phẩm
    private Integer quantity; // số lượng trong giỏ

    private Long lineTotal;   // price * quantity
}
