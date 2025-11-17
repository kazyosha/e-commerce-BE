package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.product.ProductDetailResponse;
import com.c05.kaz.ecommercebackend.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductHomeController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> getHomeProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.getHomeProducts(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetail(id));
    }
}
