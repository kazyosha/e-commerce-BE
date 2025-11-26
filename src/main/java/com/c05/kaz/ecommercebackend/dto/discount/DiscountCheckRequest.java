package com.c05.kaz.ecommercebackend.dto.discount;

import lombok.Data;

import java.util.List;

@Data
public class DiscountCheckRequest {

    private Long supplierId;
    private String code;
    private Long cartTotal;
    private List<Long> productIds;

    // ⭐ Thêm userId để check limitPerUser
    private Long userId;
}

