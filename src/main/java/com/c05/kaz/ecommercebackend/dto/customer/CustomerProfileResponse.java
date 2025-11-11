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
    private LocalDate birthDate;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
