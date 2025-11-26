package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.supplier.SupplierProfileResponse;
import com.c05.kaz.ecommercebackend.dto.supplier.SupplierProfileUpdateRequest;
import com.c05.kaz.ecommercebackend.dto.supplier.SupplierPublicResponse;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.services.ShopService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierProfileController {

    private final ShopService shopService;
    private final SupplierRepository supplierRepository;

    /* ============================================
       1. SUPPLIER SELF PROFILE (/me)
    ============================================ */
    @GetMapping("/me")
    public ResponseEntity<SupplierProfileResponse> getMyProfile() {
        return ResponseEntity.ok(shopService.getMyProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<SupplierProfileResponse> updateMyProfile(
            @RequestBody @Valid SupplierProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(shopService.updateMyProfile(request));
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<SupplierProfileResponse> updateAvatar(
            @RequestParam("avatar") MultipartFile avatar
    ) {
        return ResponseEntity.ok(shopService.updateMyAvatar(avatar));
    }


    /* ============================================
       2. PUBLIC SHOP INFO (/api/suppliers/{id})
       Cho khách hàng xem trang shop
    ============================================ */
    @GetMapping("/{id}")
    public ResponseEntity<SupplierPublicResponse> getPublicShop(@PathVariable Long id) {

        SupplierShop shop = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        SupplierPublicResponse res = SupplierPublicResponse.builder()
                .id(shop.getId())
                .shopName(shop.getShopName())
                .avatarUrl(shop.getAvatarUrl())
                .address(shop.getAddress())
                .description(shop.getDescription())
                .build();

        return ResponseEntity.ok(res);
    }
}
