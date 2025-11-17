package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.Promotion;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.enums.DiscountType;
import com.c05.kaz.ecommercebackend.repository.PromotionRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final SupplierRepository supplierRepository;

    public Optional<Promotion> findValidByCodeAndShop(String code, Long shopId) {
        SupplierShop shop = supplierRepository.findById(shopId).orElse(null);
        if (shop == null) return Optional.empty();

        return promotionRepository.findByCode(code)
                .filter(p -> p.isActive()
                        && p.getSupplier().getId().equals(shopId)
                        && p.getStartAt().isBefore(LocalDateTime.now())
                        && p.getEndAt().isAfter(LocalDateTime.now())
                        && (p.getMaxUsage() == null || p.getUsedCount() < p.getMaxUsage()));
    }

    public Promotion validateAndGetPromotion(String code, Long shopId, long originalTotal) {

        if (code == null || code.isBlank()) {
            throw new RuntimeException("Mã khuyến mãi không hợp lệ.");
        }

        Promotion promotion = findValidByCodeAndShop(code.trim(), shopId)
                .orElseThrow(() -> new RuntimeException("Mã khuyến mãi không tồn tại hoặc đã hết hạn."));

        // Kiểm tra đơn tối thiểu
        if (promotion.getMinOrderValue() != null &&
                originalTotal < promotion.getMinOrderValue()) {

            throw new RuntimeException(
                    "Đơn hàng phải đạt tối thiểu " + promotion.getMinOrderValue() + "₫ để áp dụng mã."
            );
        }

        return promotion;
    }

    public long calculateDiscount(Promotion promotion, long originalTotal) {
        if (promotion == null || originalTotal <= 0) return 0L;

        long discount = 0;

        // 🎯 CASE 1: Giảm theo %
        if (promotion.getDiscountType() == DiscountType.PERCENT) {

            if (promotion.getDiscountValue() == null || promotion.getDiscountValue() <= 0) {
                return 0L;
            }

            double percent = promotion.getDiscountValue();
            discount = Math.round(originalTotal * (percent / 100.0));
        }

        // 🎯 CASE 2: Giảm số tiền cố định
        else if (promotion.getDiscountType() == DiscountType.AMOUNT) {

            if (promotion.getDiscountValue() == null || promotion.getDiscountValue() <= 0) {
                return 0L;
            }

            discount = promotion.getDiscountValue().longValue();
        }

        // Nếu giảm vượt quá tổng tiền → fix
        if (discount > originalTotal) {
            discount = originalTotal;
        }

        if (discount < 0) discount = 0;

        return discount;
    }
}
