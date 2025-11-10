package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.supplier.SupplierAvatarUpdateRequest;
import com.c05.kaz.ecommercebackend.dto.supplier.SupplierProfileResponse;
import com.c05.kaz.ecommercebackend.dto.supplier.SupplierProfileUpdateRequest;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.exception.NotFoundException;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopService {

    private final SupplierRepository supplierRepository;
    private final SecurityUtils securityUtils;

    // PUBLIC: dùng cho /api/public/shops
    public List<SupplierShop> getAll() {
        return supplierRepository.findAll();
    }

    public Optional<SupplierShop> getById(Long id) {
        return supplierRepository.findById(id);
    }

    // SUPPLIER: /api/suppliers/me

    @Transactional(readOnly = true)
    public SupplierProfileResponse getMyProfile() {
        Long userId = securityUtils.getCurrentUserId();

        SupplierShop supplier = supplierRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Nhà cung cấp không tồn tại"));

        return mapToResponse(supplier);
    }

    public SupplierProfileResponse updateMyProfile(SupplierProfileUpdateRequest request) {
        Long userId = securityUtils.getCurrentUserId();

        SupplierShop supplier = supplierRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Nhà cung cấp không tồn tại"));

        supplier.setShopName(request.getShopName());
        supplier.setDescription(request.getDescription());
        supplier.setAddress(request.getAddress());
        supplier.setMapLocation(request.getMapLocation());

        SupplierShop saved = supplierRepository.save(supplier);
        return mapToResponse(saved);
    }

    public SupplierProfileResponse updateMyAvatar(SupplierAvatarUpdateRequest request) {
        Long userId = securityUtils.getCurrentUserId();

        SupplierShop supplier = supplierRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Nhà cung cấp không tồn tại"));

        supplier.setAvatarUrl(request.getAvatarUrl());

        SupplierShop saved = supplierRepository.save(supplier);
        return mapToResponse(saved);
    }

    private SupplierProfileResponse mapToResponse(SupplierShop s) {
        SupplierProfileResponse res = new SupplierProfileResponse();

        res.setSupplierId(s.getId());

        if (s.getUser() != null) {
            res.setUsername(s.getUser().getUsername());
            res.setEmail(s.getUser().getEmail());
        }

        res.setShopName(s.getShopName());
        res.setDescription(s.getDescription());
        res.setAddress(s.getAddress());
        res.setAvatarUrl(s.getAvatarUrl());
        res.setMapLocation(s.getMapLocation());
        res.setStatus(s.getStatus());
        res.setCreatedAt(s.getCreatedAt());
        res.setUpdatedAt(s.getUpdatedAt());

        return res;
    }
}
