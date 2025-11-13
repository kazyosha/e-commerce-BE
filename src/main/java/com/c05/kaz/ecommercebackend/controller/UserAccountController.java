package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.UserAccountDTO;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import com.c05.kaz.ecommercebackend.services.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final UserAccountRepository userAccountRepository;


    @GetMapping
    public ResponseEntity<Page<UserAccount>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String userType
    ) {
        Page<UserAccount> result = userAccountService.getUsers(page, size, search,userType);
        return ResponseEntity.ok(result);
    }


    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserAccountDTO req) {
        userAccountService.createEmployee(req);
        return ResponseEntity.ok("Tạo tài khoản nhân viên thành công!");
    }


    @PostMapping("/employee")
    public ResponseEntity<?> createEmployee(@Valid @RequestBody UserAccountDTO req) {
        userAccountService.createEmployee(req);
        return ResponseEntity.ok("Tạo tài khoản nhân viên thành công!");
    }


    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id) {
        userAccountService.resetDefaultPassword(id);
        return ResponseEntity.ok("Đã reset mật khẩu về mặc định!");
    }


    @PutMapping("/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long id) {
        userAccountService.blockUser(id);
        return ResponseEntity.ok("Đã khóa tài khoản!");
    }


    @PutMapping("/{id}/active")
    public ResponseEntity<?> activeUser(@PathVariable Long id) {
        userAccountService.activeUser(id);
        return ResponseEntity.ok("Đã kích hoạt tài khoản!");
    }

    @GetMapping("/user-stats")
    public ResponseEntity<?> getUserStats() {
        List<Object[]> stats = userAccountRepository.countUsersByType();

        Map<String, Long> result = new HashMap<>();
        for (Object[] row : stats) {
            String type = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = (Long) row[1];
            result.put(type, count);
        }

        return ResponseEntity.ok(result);
    }
}
