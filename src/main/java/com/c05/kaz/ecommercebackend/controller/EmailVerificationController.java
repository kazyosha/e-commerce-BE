package com.c05.kaz.ecommercebackend.controller;


import com.c05.kaz.ecommercebackend.services.EmailVerificationCustomerService;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import com.c05.kaz.ecommercebackend.services.EmailVerificationCustomerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/authentic/email")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmailVerificationController {

    private final EmailVerificationCustomerService emailVerificationService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendEmailOtp(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Bạn cần đăng nhập");
        }
        emailVerificationService.sendEmailVerifyOtp(principal.getName());
        return ResponseEntity.ok("Đã gửi mã OTP xác thực email");
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest req,
                                         Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Bạn cần đăng nhập");
        }
        emailVerificationService.verifyEmail(principal.getName(), req.getOtp());
        return ResponseEntity.ok("Xác thực email thành công");
    }

    @Data
    public static class VerifyEmailRequest {
        private String otp;
    }
}
