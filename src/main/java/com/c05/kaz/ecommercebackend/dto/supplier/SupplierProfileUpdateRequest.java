package com.c05.kaz.ecommercebackend.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierProfileUpdateRequest {

    @NotBlank(message = "Tên shop là bắt buộc")
    @Size(max = 150, message = "Tên shop tối đa 150 ký tự")
    private String shopName;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Size(max = 255, message = "Link map tối đa 255 ký tự")
    private String mapLocation;
}
