package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SupplierStatus;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/suppliers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminSupplierController {

    private final SupplierRepository supplierRepository;

    @GetMapping("/pending")
    public ResponseEntity<List<SupplierShop>> getPendingSuppliers() {
        List<SupplierShop> list = supplierRepository.findByStatus(SupplierStatus.PENDING);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        SupplierShop shop = supplierRepository.findById(id).orElseThrow();

        // Cập nhật trạng thái của supplier
        shop.setStatus(SupplierStatus.APPROVED);

        // Cập nhật thông tin user
        shop.getUser().setUserType(UserType.SUPPLIER);
        shop.getUser().setStatus(AccountStatus.ACTIVE);

        supplierRepository.save(shop);
        return ResponseEntity.ok("✅ Đã duyệt nhà cung cấp thành công");
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        SupplierShop shop = supplierRepository.findById(id).orElseThrow();

        // Cập nhật trạng thái supplier bị từ chối
        shop.setStatus(SupplierStatus.REJECTED);

        // Trả lại quyền customer, vẫn hoạt động bình thường
        shop.getUser().setUserType(UserType.CUSTOMER);
        shop.getUser().setStatus(AccountStatus.ACTIVE);

        supplierRepository.save(shop);

        // Có thể ghi log lý do từ chối nếu cần
        return ResponseEntity.ok("❌ Đã từ chối hồ sơ nhà cung cấp");
    }
}
