package com.c05.kaz.ecommercebackend.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRevenueDTO {

    private Long supplierId;
    private String shopName;
    private Long totalOrders;
    private Long originalTotal;
    private Long discount;
    private Long websiteFee;
    private Long storeRevenue;

}
