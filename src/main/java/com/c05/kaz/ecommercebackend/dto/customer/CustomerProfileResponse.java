package com.c05.kaz.ecommercebackend.dto.customer;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String address;

    private Integer provinceId;
    private Integer districtId;
    private String wardCode;


    private String provinceName;
    private String districtName;
    private String wardName;

    private LocalDate birthDate;
    private String avatarUrl;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
