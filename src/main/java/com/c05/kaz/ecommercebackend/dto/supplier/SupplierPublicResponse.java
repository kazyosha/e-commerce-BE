package com.c05.kaz.ecommercebackend.dto.supplier;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupplierPublicResponse {

    private Long id;

    private String shopName;

    private String avatarUrl;

    private String address;

    private String description;
}
