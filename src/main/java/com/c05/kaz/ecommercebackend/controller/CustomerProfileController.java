package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.customer.CustomerProfileResponse;
import com.c05.kaz.ecommercebackend.dto.customer.CustomerProfileUpdateRequest;
import com.c05.kaz.ecommercebackend.services.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/customers/me")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping
    public ResponseEntity<CustomerProfileResponse> getMyProfile() {
        return ResponseEntity.ok(customerProfileService.getMyProfile());
    }

    @PutMapping
    public ResponseEntity<CustomerProfileResponse> updateMyProfile(
            @RequestBody @Valid CustomerProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(customerProfileService.updateMyProfile(request));
    }

    @PatchMapping("/avatar")
    public ResponseEntity<CustomerProfileResponse> updateAvatar(
            @RequestParam("avatar") MultipartFile avatar
    ) {
        return ResponseEntity.ok(customerProfileService.updateMyAvatar(avatar));
    }
}
