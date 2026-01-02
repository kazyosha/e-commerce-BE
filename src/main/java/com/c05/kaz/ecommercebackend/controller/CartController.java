// src/main/java/com/c05/kaz/ecommercebackend/controller/CartController.java
package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.cart.AddToCartRequest;
import com.c05.kaz.ecommercebackend.dto.cart.CartResponse;
import com.c05.kaz.ecommercebackend.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<?> addToCart(@RequestBody AddToCartRequest request) {
        try {
            cartService.addToCart(request);
            return ResponseEntity.ok().body(
                    java.util.Map.of("message", "Đã thêm sản phẩm vào giỏ hàng")
            );
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<CartResponse> getMyCart() {
        return ResponseEntity.ok(cartService.getMyCart());
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateItemQuantity(
            @PathVariable Long itemId,
            @RequestBody java.util.Map<String, Integer> body
    ) {
        Integer quantity = body.get("quantity");
        try {
            CartResponse cart = cartService.updateItemQuantity(itemId, quantity);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItem(@PathVariable Long itemId) {
        try {
            CartResponse cart = cartService.removeItem(itemId);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
