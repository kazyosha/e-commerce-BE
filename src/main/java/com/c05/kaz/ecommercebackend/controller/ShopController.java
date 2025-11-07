package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.services.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/shops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShopController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<List<SupplierShop>> getAll() {
        return ResponseEntity.ok(shopService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierShop> getById(@PathVariable Long id) {
        return shopService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}