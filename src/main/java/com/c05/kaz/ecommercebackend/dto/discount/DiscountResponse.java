package com.c05.kaz.ecommercebackend.dto.discount;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiscountResponse {
    private Long id;
    private String code;
    private String type;
    private Double value;
    private Long minOrderValue;
    private Long maxDiscountAmount;
    private Integer totalUsage;
    private Integer usedCount;
    private Integer limitPerUser;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<Long> productIds;
}

