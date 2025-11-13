package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.entity.Role;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SupplierStatus;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.RoleRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.services.SupplierDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/suppliers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminSupplierController {

    private final SupplierRepository supplierRepository;
    private final SupplierDocumentService documentService;
    private final RoleRepository roleRepository;

    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        return ResponseEntity.ok(supplierRepository.findByStatus(SupplierStatus.PENDING));
    }

    /** APPROVE */
    @PutMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approve(@PathVariable Long id) {
        SupplierShop shop = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop không tồn tại"));

        UserAccount user = shop.getUser();
        user.setUserType(UserType.SUPPLIER);
        user.setStatus(AccountStatus.ACTIVE);

        Role supplierRole = roleRepository.findByCode("SUPPLIER");
        user.getRoles().add(supplierRole);

        shop.setStatus(SupplierStatus.APPROVED);

        supplierRepository.save(shop);
        return ResponseEntity.ok("Duyệt nhà cung cấp thành công");
    }

    /** REJECT → XÓA DOCUMENT */
    @PutMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<?> reject(@PathVariable Long id) {
        SupplierShop shop = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop không tồn tại"));

        // Xóa tài liệu trên cloud + DB
        documentService.deleteDocumentsBySupplier(id);

        // Reset shop về trạng thái cũ
        shop.setStatus(SupplierStatus.REJECTED);

        UserAccount user = shop.getUser();
        user.setUserType(UserType.CUSTOMER);
        user.setStatus(AccountStatus.ACTIVE);

        supplierRepository.delete(shop);
        supplierRepository.save(shop);

        return ResponseEntity.ok("Từ chối — toàn bộ tài liệu đã được xoá!");
    }
}
