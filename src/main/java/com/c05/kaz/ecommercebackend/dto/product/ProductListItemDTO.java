package com.c05.kaz.ecommercebackend.dto.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductListItemDTO {

    private Long id;
    private String name;
    private Long price;

    private String thumbnailUrl;

    private Long soldQuantity;
    private Double avgRating;

    private String shopName;
    private String shopAddress;
}
