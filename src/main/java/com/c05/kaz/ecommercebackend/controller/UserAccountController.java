package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.UserBasicDTO;
import com.c05.kaz.ecommercebackend.dto.user.UserAccountDTO;
import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.repository.CustomerRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import com.c05.kaz.ecommercebackend.services.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final UserAccountRepository userRepo;
    private final CustomerRepository customerRepo;
    private final SupplierRepository supplierRepo;


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
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {
        return ResponseEntity.ok(userAccountService.getUserStats());
    }

    @GetMapping("/basic/{id}")
    public ResponseEntity<?> getBasicInfo(@PathVariable Long id) {
        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Nếu là customer
        if (user.getUserType().name().equals("CUSTOMER")) {
            CustomerProfile c = customerRepo.findByUser_Id(id)
                    .orElse(null);

            return ResponseEntity.ok(
                    new UserBasicDTO(
                            user.getId(),
                            c != null ? c.getFullName() : null,
                            user.getUsername(),
                            null,
                            c != null ? c.getAvatarUrl() : null
                    )
            );
        }

        // Nếu là supplier
        if (user.getUserType().name().equals("SUPPLIER")) {
            SupplierShop s = supplierRepo.findByUser_Id(id)
                    .orElse(null);

            return ResponseEntity.ok(
                    new UserBasicDTO(
                            user.getId(),
                            null,
                            user.getUsername(),
                            s != null ? s.getShopName() : null,
                            s != null ? s.getAvatarUrl() : null
                    )
            );
        }

        // Mặc định (admin, HR)
        return ResponseEntity.ok(
                new UserBasicDTO(
                        user.getId(),
                        null,
                        user.getUsername(),
                        null,
                        null
                )
        );
    }


}
