// dto/payment/VnPayCreateRequest.java
package com.c05.kaz.ecommercebackend.dto.payment;

import lombok.Data;

@Data
public class VnPayCreateRequest {
    private Long orderId;    // đơn hàng cần thanh toán
    private Long amount;     // tổng tiền (finalTotal của order)
}
