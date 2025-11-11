package com.c05.kaz.ecommercebackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadShopAvatar(MultipartFile file, Long supplierId) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File ảnh trống");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "ecommerce/shops", // 📁 thư mục Cloudinary
                            "public_id", "shop_" + supplierId + "_" + System.currentTimeMillis(),
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );

            // Cloudinary trả về nhiều field, lấy secure_url là đường dẫn https
            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
            throw new RuntimeException("Upload avatar lên Cloudinary thất bại", e);
        }
    }

    // Avatar khách hàng
    public String uploadCustomerAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File ảnh trống");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "ecommerce/customers",
                            "public_id", "customer_" + userId + "_" + System.currentTimeMillis(),
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Upload avatar khách hàng lên Cloudinary thất bại", e);
        }
    }
}
