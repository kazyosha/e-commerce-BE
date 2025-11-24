package com.c05.kaz.ecommercebackend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FinanceReportItemDTO {
    private String productName;
    private long quantity;
    private long unitPrice;
    private long sale;
    private long platformFee;
    private long cost;
    private long revenue;
    private long profit;
    private String updatedAt;
}
