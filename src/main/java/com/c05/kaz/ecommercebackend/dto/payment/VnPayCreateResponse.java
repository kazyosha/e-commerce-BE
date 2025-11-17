package com.c05.kaz.ecommercebackend.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VnPayCreateResponse {
    private String paymentUrl;
}
