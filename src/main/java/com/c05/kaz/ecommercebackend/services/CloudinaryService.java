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

    /**
     * Upload file lên Cloudinary và trả về URL công khai.
     *
     * @param file file ảnh từ FE (MultipartFile)
     * @param folderName tên thư mục trong Cloudinary (VD: employee_avatars)
     * @return secure_url (link ảnh công khai)
     */
    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folderName,         // thư mục Cloudinary
                        "resource_type", "auto"       // auto: cho phép ảnh/video/pdf
                )
        );
        return uploadResult.get("secure_url").toString();
    }

    /**
     * Xóa file khỏi Cloudinary bằng publicId.
     * (publicId là phần đường dẫn không có .jpg, ví dụ: employee_avatars/abc123)
     */
    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
