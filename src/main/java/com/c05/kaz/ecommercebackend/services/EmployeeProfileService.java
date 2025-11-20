package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.user.EmployeeProfileDTO;
import com.c05.kaz.ecommercebackend.entity.EmployeeProfile;
import com.c05.kaz.ecommercebackend.repository.EmployeeProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmployeeProfileService {

    private final EmployeeProfileRepository repository;
    private final CloudinaryService cloudinaryService;

    /**
     * ✅ Lấy thông tin profile theo username (từ token)
     */
    public EmployeeProfileDTO getProfileByUsername(String username) {
        EmployeeProfile profile = repository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên có username: " + username));

        return EmployeeProfileDTO.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .age(profile.getAge())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .salary(profile.getSalary())
                .avatarUrl(profile.getAvatarUrl())
                .email(profile.getUser().getEmail())
                .build();
    }

    /**
     * ✅ Upload hoặc thay đổi avatar dựa trên username lấy từ token
     */
    public EmployeeProfileDTO updateAvatarByUsername(String username, MultipartFile file) throws IOException {
        EmployeeProfile profile = repository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên có username: " + username));

        // Xoá avatar cũ nếu có
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
            String oldPublicId = extractPublicId(profile.getAvatarUrl());
            if (oldPublicId != null) {
                try {
                    cloudinaryService.deleteFile(oldPublicId);
                } catch (Exception e) {
                    System.err.println("⚠️ Không thể xoá avatar cũ trên Cloudinary: " + e.getMessage());
                }
            }
        }

        // Upload ảnh mới lên Cloudinary
        String imageUrl = cloudinaryService.uploadFile(file, "employee_avatars");
        profile.setAvatarUrl(imageUrl);
        profile.setUpdatedAt(LocalDateTime.now());
        repository.save(profile);

        return EmployeeProfileDTO.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .age(profile.getAge())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .salary(profile.getSalary())
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }


    private String extractPublicId(String imageUrl) {
        try {
            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) return null;
            String afterUpload = parts[1];
            int slashIndex = afterUpload.indexOf("/");
            if (slashIndex != -1) {
                afterUpload = afterUpload.substring(slashIndex + 1);
            }
            return afterUpload.replaceAll("\\.[^.]+$", ""); // xoá đuôi .jpg, .png,...
        } catch (Exception e) {
            return null;
        }
    }
    public EmployeeProfileDTO updateProfileByUsername(String username, EmployeeProfileDTO dto) {
        EmployeeProfile profile = repository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên có username: " + username));

        // Cập nhật thông tin
        if (dto.getFullName() != null) profile.setFullName(dto.getFullName());
        if (dto.getPhone() != null) profile.setPhone(dto.getPhone());
        if (dto.getAddress() != null) profile.setAddress(dto.getAddress());
        if (dto.getAge() != null) profile.setAge(dto.getAge());
        if (dto.getSalary() != null) profile.setSalary(dto.getSalary());
        profile.setUpdatedAt(LocalDateTime.now());

        repository.save(profile);

        return EmployeeProfileDTO.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .age(profile.getAge())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .salary(profile.getSalary())
                .avatarUrl(profile.getAvatarUrl())
                .email(profile.getUser().getEmail())
                .build();
    }

}
