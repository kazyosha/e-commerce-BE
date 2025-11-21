package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.customer.CustomerProfileResponse;
import com.c05.kaz.ecommercebackend.dto.customer.CustomerProfileUpdateRequest;
import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.exception.NotFoundException;
import com.c05.kaz.ecommercebackend.repository.CustomerRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import com.c05.kaz.ecommercebackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerProfileService {

    private final CustomerRepository customerProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final SecurityUtils securityUtils;
    private final CloudinaryService cloudinaryService;
    private final GhnAddressService ghnAddressService;

    // ================================
    // GET /api/customers/me
    // ================================
    @Transactional(readOnly = true)
    public CustomerProfileResponse getMyProfile() {

        Long userId = securityUtils.getCurrentUserId();

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        CustomerProfile profile = customerProfileRepository.findByUser_Id(userId)
                .orElse(null);

        // Chưa có profile → trả về thông tin cơ bản
        if (profile == null) {
            return CustomerProfileResponse.builder()
                    .id(user.getId())
                    .fullName(user.getUsername())
                    .email(user.getEmail())
                    .build();
        }

        return toResponse(profile, user);
    }

    // ================================
    // PUT /api/customers/me
    // ================================
    public CustomerProfileResponse updateMyProfile(CustomerProfileUpdateRequest request) {

        Long userId = securityUtils.getCurrentUserId();

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        CustomerProfile profile = customerProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> initProfileForUser(user));

        // Cập nhật thông tin cơ bản
        profile.setFullName(request.getFullName().trim());
        profile.setPhone(request.getPhone().trim());
        profile.setBirthDate(request.getBirthDate());
        profile.setAddress(
                request.getAddress() != null && !request.getAddress().isBlank()
                        ? request.getAddress().trim()
                        : null
        );

        // ⭐ Cập nhật 3 trường GHN
        profile.setProvinceId(request.getProvinceId());
        profile.setDistrictId(request.getDistrictId());
        profile.setWardCode(request.getWardCode());



        profile.setUpdatedAt(LocalDateTime.now());

        CustomerProfile saved = customerProfileRepository.save(profile);
        return toResponse(saved, user);
    }

    // ================================
    // PATCH /api/customers/me/avatar
    // ================================
    public CustomerProfileResponse updateMyAvatar(MultipartFile avatarFile) {

        Long userId = securityUtils.getCurrentUserId();

        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new IllegalArgumentException("File avatar không hợp lệ");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        CustomerProfile profile = customerProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> initProfileForUser(user));

        String avatarUrl = cloudinaryService.uploadCustomerAvatar(avatarFile, userId);
        profile.setAvatarUrl(avatarUrl);
        profile.setUpdatedAt(LocalDateTime.now());

        CustomerProfile saved = customerProfileRepository.save(profile);
        return toResponse(saved, user);
    }

    // ================================
    // INTERNAL: Lấy/khởi tạo profile
    // ================================
    @Transactional(readOnly = true)
    public CustomerProfile getByUser(UserAccount user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User không hợp lệ");
        }

        return customerProfileRepository.findByUser_Id(user.getId())
                .orElseGet(() -> initProfileForUser(user));
    }

    // ⭐ Khởi tạo profile mới cho user (KHÔNG làm mất dữ liệu GHN)
    private CustomerProfile initProfileForUser(UserAccount user) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setFullName(user.getUsername());
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        return customerProfileRepository.save(profile);
    }

    // ================================
    // Mapping entity → response DTO
    // ================================
    private CustomerProfileResponse toResponse(CustomerProfile profile, UserAccount user) {

        String provinceName = ghnAddressService.getProvinceName(profile.getProvinceId());
        String districtName = ghnAddressService.getDistrictName(profile.getDistrictId());
        String wardName = ghnAddressService.getWardName(
                profile.getWardCode(),
                profile.getDistrictId()
        );

        return CustomerProfileResponse.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .email(user.getEmail())

                .phone(profile.getPhone())
                .address(profile.getAddress())

                .provinceId(profile.getProvinceId())
                .districtId(profile.getDistrictId())
                .wardCode(profile.getWardCode())

                .provinceName(provinceName)
                .districtName(districtName)
                .wardName(wardName)

                .birthDate(profile.getBirthDate())
                .avatarUrl(profile.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
