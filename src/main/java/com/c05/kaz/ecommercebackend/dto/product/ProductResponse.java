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

    // ⭐ ID nội bộ của nhà cung cấp (bạn vừa thêm)
    private Integer supplierProductIndex;

    // ⭐ MANY-TO-MANY
    private List<Long> categoryIds;
    private List<String> categoryNames;

    private String name;
    private String description;

    private Long price;
    private Long importPrice;
    private Integer quantity;
    private boolean active;

    private String thumbnailUrl;
    private List<String> images;

    private Long soldQuantity;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String supplierName;

    private Long totalRevenue;     // doanh số
    private Long netRevenue;       // doanh thu thực

    // ⭐ THÊM TRƯỜNG NÀY
    private List<String> discountCodes;
}
