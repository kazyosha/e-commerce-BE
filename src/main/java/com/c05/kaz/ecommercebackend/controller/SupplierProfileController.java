package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.supplier.SupplierProfileResponse;
import com.c05.kaz.ecommercebackend.dto.supplier.SupplierProfileUpdateRequest;
import com.c05.kaz.ecommercebackend.services.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/suppliers/me")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierProfileController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<SupplierProfileResponse> getMyProfile() {
        return ResponseEntity.ok(shopService.getMyProfile());
    }

    @PutMapping
    public ResponseEntity<SupplierProfileResponse> updateMyProfile(
            @RequestBody @Valid SupplierProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(shopService.updateMyProfile(request));
    }

    @PatchMapping("/avatar")
    public ResponseEntity<SupplierProfileResponse> updateAvatar(
            @RequestParam("avatar") MultipartFile avatar
    ) {
        return ResponseEntity.ok(shopService.updateMyAvatar(avatar));
    }
}
