package com.c05.kaz.ecommercebackend.dto.discount;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiscountRequest {

    @NotBlank
    @Size(min = 3, max = 30)
    private String code;

    @NotBlank
    @Pattern(regexp = "PERCENT|AMOUNT")
    private String type;

    @NotNull
    @DecimalMin("0.01")
    private Double value;

    @DecimalMin("0")
    private Long minOrderValue;

    @DecimalMin("0")
    private Long maxDiscountAmount;

    @NotNull
    @Min(1)
    private Integer totalUsage;

    @NotNull
    @Min(1)
    private Integer limitPerUser;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    @NotEmpty
    private List<Long> productIds;
}

