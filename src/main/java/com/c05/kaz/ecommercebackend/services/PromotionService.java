package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.Promotion;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
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
}
