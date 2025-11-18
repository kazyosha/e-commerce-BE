package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.supplier.StoreRevenueDTO;
import com.c05.kaz.ecommercebackend.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRevenueService {

    private final OrderRepository orderRepository;

    public List<StoreRevenueDTO> getRevenue(List<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) {
            return List.of();
        }
        return orderRepository.calculateRevenue(supplierIds);
    }
}

