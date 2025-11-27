package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.discount.DiscountCheckRequest;
import com.c05.kaz.ecommercebackend.dto.discount.DiscountCheckResponse;
import com.c05.kaz.ecommercebackend.dto.discount.DiscountRequest;
import com.c05.kaz.ecommercebackend.dto.discount.DiscountResponse;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.services.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/suppliers/discounts")   // ĐÚNG PREFIX CHUẨN
public class DiscountController {

    private final DiscountService discountService;

    // ================================
    // LẤY TẤT CẢ MÃ GIẢM GIÁ CỦA SHOP
    // ================================
    @GetMapping("/{supplierId}")
    public List<DiscountResponse> getBySupplier(@PathVariable Long supplierId) {
        return discountService.getBySupplier(supplierId);
    }

    // ================================
    // TẠO MÃ GIẢM GIÁ
    // ================================
    @PostMapping("/{supplierId}")
    public DiscountResponse create(
            @PathVariable Long supplierId,
            @Valid @RequestBody DiscountRequest request
    ) {
        return discountService.create(supplierId, request);
    }

    // ================================
    // BẬT / TẮT MÃ GIẢM GIÁ
    // ================================
    @PatchMapping("/{supplierId}/toggle/{discountId}")
    public DiscountResponse toggleStatus(
            @PathVariable Long supplierId,
            @PathVariable Long discountId
    ) {
        return discountService.toggleStatus(supplierId, discountId);
    }

    // ================================
    // ÁP DỤNG MÃ GIẢM GIÁ (TRỪ LƯỢT)
    // ================================
    @PostMapping("/apply")
    public Long apply(
            @RequestParam Long supplierId,
            @RequestParam String code,
            @RequestParam Long orderValue,
            @AuthenticationPrincipal UserAccount user
    ) {
        return discountService.apply(supplierId, code, orderValue, user);
    }

    // ================================
    // XOÁ MÃ GIẢM GIÁ
    // ================================
    @DeleteMapping("/{supplierId}/{discountId}")
    public void delete(
            @PathVariable Long supplierId,
            @PathVariable Long discountId
    ) {
        discountService.delete(supplierId, discountId);
    }

    // ===========================================
    // LẤY VOUCHER HIỂN THỊ CHO USER
    // ===========================================
    @GetMapping("/supplier/{supplierId}/available")
    public List<DiscountResponse> getAvailableDiscounts(@PathVariable Long supplierId) {
        return discountService.getAvailableDiscountsForUser(supplierId);
    }

    // ===========================================
    // CHECK MÃ GIẢM GIÁ (KHÔNG TRỪ LƯỢT)
    // ===========================================
    @PostMapping("/check")
    public DiscountCheckResponse checkDiscount(
            @RequestBody DiscountCheckRequest request
    ) {
        return discountService.checkDiscountForCart(request);
    }
}
