package com.c05.kaz.ecommercebackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * ✅ Upload file chung (tự động xác định loại file)
     */
    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File ảnh trống");
        }

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folderName,
                        "resource_type", "auto" // cho phép ảnh/video/pdf
                )
        );

        return (String) uploadResult.get("secure_url");
    }


    /**
     * ✅ Upload avatar cho khách hàng
     */
    public String uploadCustomerAvatar(MultipartFile file, Long userId) {
        return uploadWithCustomPath(file, "ecommerce/customers", "customer_" + userId);
    }

    /**
     * ✅ Upload avatar cho shop / nhà cung cấp
     */
    public String uploadShopAvatar(MultipartFile file, Long supplierId) {
        return uploadWithCustomPath(file, "ecommerce/shops", "shop_" + supplierId);
    }

    /**
     * ✅ Hàm helper: upload với đường dẫn tùy chỉnh
     */
    private String uploadWithCustomPath(MultipartFile file, String folder, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File ảnh trống");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", prefix + "_" + System.currentTimeMillis(),
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );

            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Upload file lên Cloudinary thất bại", e);
        }
    }

    /**
     * ✅ Xóa file khỏi Cloudinary bằng publicId
     * (publicId là phần sau "upload/" nhưng không có .jpg, .png,...)
     */
    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    public String uploadImage(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "reviews")  // folder lưu ảnh review
            );
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh thất bại", e);
        }
    }

    public List<String> uploadFiles(MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : files) {
            urls.add(uploadImage(f));
        }
        return urls;
    }
}
