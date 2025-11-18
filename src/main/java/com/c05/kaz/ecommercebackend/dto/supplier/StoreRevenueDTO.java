package com.c05.kaz.ecommercebackend.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoreRevenueDTO {
    private Long storeId;
    private String storeName;
    private Long storeRevenue;    // tổng doanh thu cửa hàng
    private Long websiteRevenue;  // 3% doanh thu
}