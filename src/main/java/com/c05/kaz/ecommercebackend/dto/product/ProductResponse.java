package com.c05.kaz.ecommercebackend.dto.product;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;

    private Long supplierId;

    // ⭐ HỖ TRỢ NHIỀU DANH MỤC
    private List<Long> categoryIds;
    private List<String> categoryNames;

    private String name;
    private String description;
    private Long price;
    private Long importPrice;  // ⭐ giá nhập
    private Integer quantity;
    private boolean active;

    private String thumbnailUrl;
    private List<String> images;

    private Long soldQuantity;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String supplierName;
}
