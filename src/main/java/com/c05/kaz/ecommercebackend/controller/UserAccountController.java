package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.EmployeeAccountRequest;
import com.c05.kaz.ecommercebackend.services.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping("/employee")
    public ResponseEntity<?> createEmployee(@Valid @RequestBody EmployeeAccountRequest req) {
        userAccountService.createEmployee(req);
        return ResponseEntity.ok("Tạo tài khoản nhân viên thành công!");
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id) {
        userAccountService.resetDefaultPassword(id);
        return ResponseEntity.ok("Đã reset mật khẩu về mặc định!");
    }
}
