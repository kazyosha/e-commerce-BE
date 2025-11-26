package com.c05.kaz.ecommercebackend.dto.product;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String description;

    private Long price;
    private Integer quantity;
    private boolean active;
    private Long soldQuantity;

    private String categoryName;
    private Long categoryId;

    private Long supplierId;
    private String supplierName;

    private String supplierAvatar;
    private String supplierDescription;
    private String supplierAddress;

    private List<String> images; // list URL ảnh
}
