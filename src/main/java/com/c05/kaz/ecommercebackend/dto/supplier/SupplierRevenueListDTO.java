package com.c05.kaz.ecommercebackend.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRevenueListDTO {

    private List<SupplierRevenueDTO> items;
    private Long totalRevenueAllSuppliers;
}
