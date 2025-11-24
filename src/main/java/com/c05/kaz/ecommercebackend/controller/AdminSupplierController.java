package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.supplier.SupplierRevenueDTO;
import com.c05.kaz.ecommercebackend.dto.supplier.SupplierRevenueListDTO;
import com.c05.kaz.ecommercebackend.entity.Role;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SupplierStatus;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.RoleRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.services.RevenueExportService;
import com.c05.kaz.ecommercebackend.services.SupplierDocumentService;
import com.c05.kaz.ecommercebackend.services.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/suppliers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminSupplierController {

    private final SupplierRepository supplierRepository;
    private final SupplierDocumentService documentService;
    private final RoleRepository roleRepository;
    private final SupplierService supplierService;
    private final RevenueExportService revenueExportService;

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

        // Cập nhật trạng thái shop
        shop.setStatus(SupplierStatus.REJECTED);

        // Cập nhật user về CUSTOMER
        UserAccount user = shop.getUser();
        user.setUserType(UserType.CUSTOMER);
        user.setStatus(AccountStatus.ACTIVE);

        // 👉 CHỈ SAVE, KHÔNG ĐƯỢC delete + save cùng lúc
        supplierRepository.save(shop);

        return ResponseEntity.ok("Từ chối — toàn bộ tài liệu đã được xoá!");
    }

    @GetMapping("/{supplierId}/revenue")
    public ResponseEntity<byte[]> exportSupplierRevenue(@PathVariable Long supplierId) {

        // Lấy dữ liệu
        SupplierRevenueDTO dto = supplierService.getRevenueBySupplier(supplierId);
        List<SupplierRevenueDTO> list = List.of(dto);

        // Tạo file Excel
        ByteArrayInputStream excelStream;
        try {
            excelStream = revenueExportService.exportRevenueExcel(list);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo file Excel", e);
        }

        // Trả về dạng byte[]
        byte[] fileBytes;
        fileBytes = excelStream.readAllBytes();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=revenue_supplier_" + supplierId + ".xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(fileBytes);
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> allSuppliersRevenue() {
        SupplierRevenueListDTO data = supplierService.getAllSuppliersRevenue();

        return ResponseEntity.ok()
                .body(data);
    }

    @GetMapping("/revenue/export")
    public ResponseEntity<byte[]> exportAllSuppliersRevenue() {

        // Lấy dữ liệu toàn bộ supplier
        List<SupplierRevenueDTO> list = supplierService.getAllSuppliersRevenue().getItems();

        ByteArrayInputStream excelStream;
        try {
            excelStream = revenueExportService.exportRevenueExcel(list);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo file Excel", e);
        }

        byte[] fileBytes;
        fileBytes = excelStream.readAllBytes();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=revenue_all_suppliers.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(fileBytes);
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<?> getMonthlyRevenue(
            @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(supplierService.getMonthlyRevenue(year));
    }

}
