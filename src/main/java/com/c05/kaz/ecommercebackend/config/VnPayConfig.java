// src/main/java/com/c05/kaz/ecommercebackend/config/VNPayConfig.java
package com.c05.kaz.ecommercebackend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VnPayConfig {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl; // https://sandbox.vnpayment.vn/paymentv2/vpcpay.html

    @Value("${vnpay.return-url}")
    private String returnUrl; // FE return URL

    @Value("${vnpay.version}")
    private String version;   // 2.1.0

    @Value("${vnpay.command}")
    private String command;   // pay

    @Value("${vnpay.curr-code}")
    private String currCode;  // VND
}
