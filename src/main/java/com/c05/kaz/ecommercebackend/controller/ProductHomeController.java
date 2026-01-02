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
            @RequestParam(defaultValue = "15") int size
    ) {
        return ResponseEntity.ok(productService.getHomeProducts(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetail(id));
    }

    @GetMapping("/related")
    public ResponseEntity<?> getRelatedProducts(
            @RequestParam Long categoryId,
            @RequestParam Long excludeId
    ) {
        return ResponseEntity.ok(productService.getRelatedByCategory(categoryId, excludeId));
    }


    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<?> getProductsBySupplier(
            @PathVariable Long supplierId,
            @RequestParam Long excludeId
    ) {
        return ResponseEntity.ok(productService.getBySupplier(supplierId, excludeId));
    }
}
