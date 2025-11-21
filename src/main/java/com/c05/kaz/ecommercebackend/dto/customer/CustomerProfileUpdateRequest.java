package com.c05.kaz.ecommercebackend.dto.customer;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerProfileUpdateRequest {

    @NotBlank(message = "Họ tên là bắt buộc")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số")
    private String phone;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    private Integer provinceId;
    private Integer districtId;
    private String wardCode;

    @Past(message = "Ngày sinh không hợp lệ")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}
