package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.entity.SupplierDocument;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.services.SupplierDocumentService;
import com.c05.kaz.ecommercebackend.services.SupplierUpgradeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/supplier/upgrade")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierUpgradeController {

    private final SupplierUpgradeService upgradeService;
    private final SupplierDocumentService supplierDocumentService;

    // B1: lưu thông tin shop
    @PostMapping("/info")
    public ResponseEntity<?> saveInfo(@RequestBody ShopInfoRequest req,
                                      Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        SupplierShop shop = upgradeService.saveShopInfo(
                principal.getName(),
                req.getShopName(),
                req.getAddress(),
                req.getDescription()
        );
        return ResponseEntity.ok(shop.getId());
    }

    // B2: upload documents -> Cloudinary
    @PostMapping(
            value = "/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadDocuments(
            @RequestParam("supplierId") Long supplierId,
            @RequestParam("files") List<MultipartFile> files,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng chọn ít nhất 1 file");
        }

        List<SupplierDocument> docs = supplierDocumentService.uploadDocuments(supplierId, files);
        return ResponseEntity.ok(docs);
    }

    // B3: gửi OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        upgradeService.sendOtp(principal.getName());
        return ResponseEntity.ok("Đã gửi mã OTP đến email của bạn");
    }

    // B4: verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest req,
                                       Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        upgradeService.verifyOtpAndUpgrade(principal.getName(), req.getOtp());
        return ResponseEntity.ok("Đăng ký nhà cung cấp thành công, vui lòng chờ admin duyệt");
    }

    // DTOs
    @Data
    public static class ShopInfoRequest {
        private String shopName;
        private String address;
        private String description;
    }

    @Data
    public static class VerifyOtpRequest {
        private String otp;
    }
}
