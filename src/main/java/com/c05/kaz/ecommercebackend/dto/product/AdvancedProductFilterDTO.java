package com.c05.kaz.ecommercebackend.dto.product;

import lombok.Data;

@Data
public class AdvancedProductFilterDTO {

    private String search;     // tên sản phẩm (optional)
    private String sort;       // popular, lowToHigh, highToLow
    private Integer minPrice;  // giá tối thiểu
    private Integer maxPrice;  // giá tối đa

    private String category;   // category code / slug
    private String location;   // địa chỉ shop
    private Integer rating;    // số sao >=

    private Integer page = 0;  // default
    private Integer size = 12; // default
}
