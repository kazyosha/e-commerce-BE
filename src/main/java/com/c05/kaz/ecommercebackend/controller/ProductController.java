package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.entity.Product;
import com.c05.kaz.ecommercebackend.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // cho FE gọi thoải mái, sau này siết lại domain
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.getAllProducts(page, size));
    }

    // tìm kiếm theo keyword + categoryId (phục vụ trang chủ)
    @GetMapping("/search")
    public ResponseEntity<Page<Product>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.search(keyword, categoryId, page, size));
    }

    // top bán chạy của 1 shop
    @GetMapping("/top-sold/{shopId}")
    public ResponseEntity<?> topSoldByShop(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(productService.getTopSoldByShop(shopId, limit));
    }
}