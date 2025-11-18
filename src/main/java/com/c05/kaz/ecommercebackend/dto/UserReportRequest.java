package com.c05.kaz.ecommercebackend.dto;

import lombok.Data;

@Data
public class UserReportRequest {
    private String reason;
    private String detail;
    private String severity;
}
