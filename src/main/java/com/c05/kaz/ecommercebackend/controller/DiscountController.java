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

    @GetMapping("/{supplierId}")
    public List<DiscountResponse> getBySupplier(@PathVariable Long supplierId) {
        return discountService.getBySupplier(supplierId);
    }


    @PostMapping("/{supplierId}")
    public DiscountResponse create(
            @PathVariable Long supplierId,
            @Valid @RequestBody DiscountRequest request
    ) {
        return discountService.create(supplierId, request);
    }


    @PatchMapping("/{supplierId}/toggle/{discountId}")
    public DiscountResponse toggleStatus(
            @PathVariable Long supplierId,
            @PathVariable Long discountId
    ) {
        return discountService.toggleStatus(supplierId, discountId);
    }


    @PostMapping("/apply")
    public Long apply(
            @RequestParam Long supplierId,
            @RequestParam String code,
            @RequestParam Long orderValue,
            @AuthenticationPrincipal UserAccount user
    ) {
        return discountService.apply(supplierId, code, orderValue, user);
    }


    @DeleteMapping("/{supplierId}/{discountId}")
    public void delete(
            @PathVariable Long supplierId,
            @PathVariable Long discountId
    ) {
        discountService.delete(supplierId, discountId);
    }

    @GetMapping("/supplier/{supplierId}/available")
    public List<DiscountResponse> getAvailableDiscounts(@PathVariable Long supplierId) {
        return discountService.getAvailableDiscountsForUser(supplierId);
    }

    @PostMapping("/check")
    public DiscountCheckResponse checkDiscount(
            @RequestBody DiscountCheckRequest request
    ) {
        return discountService.checkDiscountForCart(request);
    }
}
