package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.supplier.SupplierBasicDTO;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatUserController {

    private final SupplierRepository supplierRepo;

    // Lấy tất cả shop (hoặc lọc theo active)
    @GetMapping("/suppliers")
    public List<SupplierBasicDTO> getAllSuppliers() {
        return supplierRepo.findAll().stream()
                .map(s -> new SupplierBasicDTO(
                        s.getId(),
                        s.getShopName(),
                        s.getAvatarUrl()
                ))
                .toList();
    }
}
