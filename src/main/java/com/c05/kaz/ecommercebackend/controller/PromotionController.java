package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.entity.Promotion;
import com.c05.kaz.ecommercebackend.services.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/validate")
    public ResponseEntity<?> validate(
            @RequestParam String code,
            @RequestParam Long shopId
    ) {
        return promotionService.findValidByCodeAndShop(code, shopId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Mã khuyến mãi không hợp lệ hoặc đã hết hạn"));
    }
}
