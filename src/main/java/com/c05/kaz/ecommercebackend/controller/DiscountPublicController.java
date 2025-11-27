package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.discount.DiscountCheckRequest;
import com.c05.kaz.ecommercebackend.dto.discount.DiscountResponse;
import com.c05.kaz.ecommercebackend.entity.Discount;
import com.c05.kaz.ecommercebackend.entity.Product;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.enums.DiscountStatus;
import com.c05.kaz.ecommercebackend.repository.DiscountRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.services.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/discounts")
@RequiredArgsConstructor
public class DiscountPublicController {

    private final DiscountService discountService;
    private final DiscountRepository discountRepository;
    private final SupplierRepository supplierRepository;

    // ====================================
    // LẤY DS MÃ GIẢM GIÁ CÔNG KHAI CỦA SHOP
    // ====================================
    @GetMapping("/supplier/{supplierId}/available")
    public List<DiscountResponse> getAvailable(@PathVariable Long supplierId) {

        SupplierShop supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Shop không tồn tại"));

        List<Discount> list = discountRepository.findBySupplier(supplier);

        return list.stream()
                .filter(d -> d.getStatus() == DiscountStatus.ACTIVE)
                .filter(d -> d.getStartDate().isBefore(LocalDateTime.now()))
                .filter(d -> d.getEndDate().isAfter(LocalDateTime.now()))
                .map(d -> {
                    DiscountResponse r = new DiscountResponse();
                    r.setId(d.getId());
                    r.setCode(d.getCode());
                    r.setType(d.getType().name());
                    r.setValue(d.getValue());
                    r.setMinOrderValue(d.getMinOrderValue());
                    r.setMaxDiscountAmount(d.getMaxDiscountAmount());
                    r.setProductIds(d.getApplicableProducts()
                            .stream()
                            .map(Product::getId)
                            .toList());
                    return r;
                })
                .toList();
    }

    // ====================================
    // CHECK MÃ GIẢM GIÁ (PUBLIC - CHECK KHÔNG TRỪ LƯỢT)
    // ====================================
    @PostMapping("/check")
    public Map<String, Object> check(@RequestBody DiscountCheckRequest req) {

        Map<String, Object> res = new HashMap<>();

        try {
            Long reduced = discountService.checkDiscount(
                    req.getSupplierId(),
                    req.getCode(),
                    req.getCartTotal(),
                    req.getProductIds()
            );

            res.put("valid", true);
            res.put("discountAmount", reduced);

        } catch (Exception e) {
            res.put("valid", false);
            res.put("reason", e.getMessage());
        }

        return res;
    }
}
