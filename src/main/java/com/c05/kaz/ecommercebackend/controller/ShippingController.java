package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.shipping.ShippingFeeRequest;
import com.c05.kaz.ecommercebackend.services.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/services")
    public ResponseEntity<?> getServices(@RequestBody Map<String, Integer> req) {
        int fromDistrict = req.get("fromDistrictId");
        int toDistrict = req.get("toDistrictId");

        return ResponseEntity.ok(shippingService.getAvailableServices(fromDistrict, toDistrict));
    }

    @PostMapping("/fee")
    public ResponseEntity<?> getFee(@RequestBody ShippingFeeRequest req) {
        return ResponseEntity.ok(shippingService.calculateFee(req));
    }
}
