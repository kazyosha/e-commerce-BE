package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.product.AdvancedProductFilterDTO;
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

    // ===============================
    // GET ALL PRODUCTS (HOME)
    // ===============================
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.getAllProducts(page, size));
    }
    @GetMapping("/all")
    public ResponseEntity<?> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // ===============================
    // PUBLIC SEARCH (FE đang gọi)
    // ===============================
    @GetMapping("/search")
    public ResponseEntity<?> searchPublic(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (keyword.trim().isEmpty()) {
            return ResponseEntity.ok(Page.empty());
        }

        return ResponseEntity.ok(
                productService.searchPublic(keyword.trim(), page, size)
        );
    }

    // ===============================
    // OLD SEARCH (DÙNG TRANG HOME FILTER)
    //  → vẫn giữ, không xoá
    // ===============================
    @GetMapping("/filter")
    public ResponseEntity<?> searchByCategoryAndKeyword(
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

    @GetMapping("/suggest")
    public ResponseEntity<?> suggest(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.suggest(keyword));
    }

    @GetMapping("/advanced-search")
    public ResponseEntity<?> advanced(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        AdvancedProductFilterDTO dto = new AdvancedProductFilterDTO();

        dto.setSearch(search);
        dto.setSort(sort);
        dto.setMinPrice(minPrice);
        dto.setMaxPrice(maxPrice);
        dto.setCategory(category);
        dto.setLocation(location);
        dto.setRating(rating);
        dto.setPage(page);
        dto.setSize(size);

        return ResponseEntity.ok(productService.advancedSearch(dto));
    }


}