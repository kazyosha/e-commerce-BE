package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final SupplierRepository supplierRepository;

    public List<SupplierShop> getAll() {
        return supplierRepository.findAll();
    }

    public Optional<SupplierShop> getById(Long id) {
        return supplierRepository.findById(id);
    }
}
