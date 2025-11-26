package com.c05.kaz.ecommercebackend.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTopResponse {

    private Long id;
    private String name;
    private Long price;
    private String thumbnailUrl;
    private Integer soldQuantity;

}
