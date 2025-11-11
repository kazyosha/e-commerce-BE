package com.c05.kaz.ecommercebackend.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierAvatarUpdateRequest {

    @NotBlank(message = "Avatar URL là bắt buộc")
    @Size(max = 255, message = "Avatar URL tối đa 255 ký tự")
    private String avatarUrl;
}
