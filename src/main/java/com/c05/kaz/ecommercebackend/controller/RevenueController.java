package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.supplier.StoreRevenueDTO;
import com.c05.kaz.ecommercebackend.services.AdminRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class RevenueController {

    private final AdminRevenueService revenueService;

    @PostMapping("/revenue/bulk")
    public ResponseEntity<List<StoreRevenueDTO>> getBulkRevenue(
            @RequestBody List<Long> supplierIds
    ) {
        return ResponseEntity.ok(revenueService.getRevenue(supplierIds));
    }
}

