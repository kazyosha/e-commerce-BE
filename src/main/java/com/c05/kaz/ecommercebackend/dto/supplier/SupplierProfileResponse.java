package com.c05.kaz.ecommercebackend.dto.supplier;

import com.c05.kaz.ecommercebackend.enums.SupplierStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SupplierProfileResponse {

    private Long supplierId;

    private String username;
    private String email;

    private String shopName;
    private String description;
    private String address;
    private String avatarUrl;
    private String mapLocation;

    private SupplierStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
