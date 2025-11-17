package com.c05.kaz.ecommercebackend.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePaymentResponse {

    private String paymentUrl;   // URL để FE redirect sang cổng thanh toán
    private Long orderId;        // Đơn nội bộ của mình
    private Long amount;         // Số tiền thanh toán (finalTotal)
}
