package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.DocumentType;
import com.c05.kaz.ecommercebackend.enums.SupplierStatus;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierUpgradeService {

    private final UserAccountRepository userRepo;
    private final SupplierRepository supplierRepo;
    private final SupplierDocumentRepository docRepo;
    private final RoleRepository roleRepo;
    private final OtpService otpService;

    private UserAccount getCurrentUser(String principalName) {
        // principalName là username (theo JWT)
        return userRepo.findByUsernameOrEmail(principalName, principalName)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // B1: tạo / cập nhật thông tin shop
    @Transactional
    public SupplierShop saveShopInfo(String principalName, String shopName, String address, String description) {
        UserAccount user = getCurrentUser(principalName);

        if (user.getUserType() != UserType.CUSTOMER) {
            throw new RuntimeException("Chỉ tài khoản khách hàng mới được đăng ký nhà cung cấp");
        }

        SupplierShop shop = supplierRepo.findByUser_Id(user.getId()).orElse(
                SupplierShop.builder()
                        .user(user)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        shop.setShopName(shopName);
        shop.setAddress(address);
        shop.setDescription(description);
        shop.setUpdatedAt(LocalDateTime.now());

        return supplierRepo.save(shop);
    }

    // B2: lưu danh sách chứng từ (FE gửi URL sau khi upload)
    @Transactional
    public void saveDocuments(String principalName, Long supplierId, List<DocumentRequest> docs) {
        UserAccount user = getCurrentUser(principalName);
        SupplierShop shop = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier không tồn tại"));

        if (!shop.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Không thể sửa thông tin nhà cung cấp của người khác");
        }

        for (DocumentRequest d : docs) {
            SupplierDocument doc = SupplierDocument.builder()
                    .supplier(shop)
                    .type(d.getType() != null ? d.getType() : DocumentType.OTHER)
                    .fileUrl(d.getFileUrl())
                    .uploadedAt(LocalDateTime.now())
                    .approved(null)
                    .build();
            docRepo.save(doc);
        }
    }

    // B3: gửi OTP qua email
    public void sendOtp(String principalName) {
        UserAccount user = getCurrentUser(principalName);
        otpService.sendUpgradeOtp(user);
    }

    @Transactional
    public void verifyOtpAndUpgrade(String principalName, String code) {
        UserAccount user = getCurrentUser(principalName);

        boolean ok = otpService.verifyOtp(user, code);
        if (!ok) {
            throw new RuntimeException("OTP không hợp lệ hoặc đã hết hạn");
        }

        SupplierShop shop = supplierRepo.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Chưa có thông tin shop"));

        // User vẫn login bình thường
        user.setStatus(AccountStatus.ACTIVE);

        // Có thể set SUPPLIER luôn + role SUPPLIER
        user.setUserType(UserType.SUPPLIER);
        user.getRoles().add(roleRepo.findByCode("SUPPLIER"));
        userRepo.save(user);

        // Hồ sơ nhà cung cấp ở trạng thái CHỜ DUYỆT
        shop.setStatus(SupplierStatus.PENDING);
        shop.setUpdatedAt(LocalDateTime.now());
        supplierRepo.save(shop);
    }

    // DTO nhỏ cho documents
    @lombok.Data
    public static class DocumentRequest {
        private DocumentType type;
        private String fileUrl;
    }
}
