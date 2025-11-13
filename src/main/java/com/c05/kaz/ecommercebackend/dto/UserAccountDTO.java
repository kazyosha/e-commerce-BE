package com.c05.kaz.ecommercebackend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccountDTO {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 50, message = "Username phải từ 4-50 ký tự")
    private String username;

    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 50, message = "Tên phải từ 4-50 ký tự")
    private String fullName;

    // Password mặc định
    private String password = "123456@Abc";

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;


    private int age;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private String address;

    @DecimalMin(value = "0", inclusive = false, message = "Lương phải > 0")
    @DecimalMax(value = "100000000.0", inclusive = false, message = "Lương phải < 100,000,000")
    private Long salary;
}
