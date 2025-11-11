package com.c05.kaz.ecommercebackend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfileDTO {
    private Long id;
    private String fullName;
    private Integer age;
    private String phone;
    private String address;
    private Long salary;
    private String avatarUrl; // 🌟 trả URL Cloudinary về FE
    private String email;
}
