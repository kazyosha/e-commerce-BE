package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.payment.VnPayCreateRequest;
import com.c05.kaz.ecommercebackend.dto.payment.VnPayCreateResponse;
import com.c05.kaz.ecommercebackend.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    // FE gọi endpoint này để lấy URL thanh toán VNPay
    @PostMapping("/vnpay/create")
    public ResponseEntity<?> createVnPayPayment(
            @RequestBody VnPayCreateRequest req,
            HttpServletRequest servletRequest
    ) {
        try {
            VnPayCreateResponse res = paymentService.createVnPayPayment(req, servletRequest);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("message", e.getMessage())
            );
        }
    }

    // VNPay redirect về đây (vnp_ReturnUrl)
    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVnPayReturn(HttpServletRequest request) {
        String result = paymentService.handleVnPayReturn(request);

        // tuỳ ý: redirect về FE, kèm theo status, hoặc trả JSON
        // Ở đây trả JSON đơn giản
        return ResponseEntity.ok(
                java.util.Map.of("status", result)
        );
    }
}
