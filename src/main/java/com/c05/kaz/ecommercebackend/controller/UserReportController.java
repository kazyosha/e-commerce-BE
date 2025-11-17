package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.UserReportRequest;
import com.c05.kaz.ecommercebackend.services.UserReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService reportService;

    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportUser(
            @PathVariable Long id,
            @RequestBody UserReportRequest request
    ) {
        reportService.createReport(id, request);
        return ResponseEntity.ok("Đã gửi báo cáo!");
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @PutMapping("/reports/{id}/approve")
    public ResponseEntity<?> approveReport(@PathVariable Long id) {
        reportService.approveReport(id);
        return ResponseEntity.ok("Đã chấp nhận báo cáo. Tài khoản đã bị khóa!");
    }

    @PostMapping("/reports/{id}/send-email")
    public ResponseEntity<?> sendWarningEmail(@PathVariable Long id) {
        reportService.sendWarningEmail(id);
        return ResponseEntity.ok("Email sent");
    }

}
