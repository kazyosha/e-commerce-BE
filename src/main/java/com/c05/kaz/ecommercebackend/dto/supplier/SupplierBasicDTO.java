package com.c05.kaz.ecommercebackend.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SupplierBasicDTO {
    private Long id;           // supplierId / userId
    private String shopName;   // tên shop
    private String avatarUrl;  // ảnh đại diện
}
