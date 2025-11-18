package com.c05.kaz.ecommercebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserReportDTO {
    private Long id;
    private String reportedUsername;
    private String reporterUsername;
    private String reason;
    private String detail;
    private String severity;
    private LocalDateTime createdAt;
    private Boolean emailSent;

}
