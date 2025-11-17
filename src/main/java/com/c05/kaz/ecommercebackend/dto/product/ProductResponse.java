package com.c05.kaz.ecommercebackend.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private Long price;
    private String thumbnail; // ảnh chính (image đầu tiên)
    private String categoryName;
    private String supplierName;
    private Long soldQuantity;
    private boolean active;
}
