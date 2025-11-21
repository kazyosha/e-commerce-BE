package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.services.GhnAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GhnAddressController {

    private final GhnAddressService ghnAddressService;

    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        return ResponseEntity.ok(ghnAddressService.getProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(ghnAddressService.getDistricts(provinceId));
    }

    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(ghnAddressService.getWards(districtId));
    }
}

