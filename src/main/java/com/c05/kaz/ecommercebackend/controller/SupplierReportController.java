package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.services.SupplierReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers/report")
@RequiredArgsConstructor
public class SupplierReportController {

    private final SupplierReportService reportService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFinanceReport(
            @RequestParam String from,
            @RequestParam String to,
            @AuthenticationPrincipal UserDetails user
    ) {
        Long supplierId = reportService.getSupplierIdFromUser(user);

        byte[] bytes = reportService.exportFinanceExcel(supplierId, from, to);

        System.out.println("Export bytes = " + bytes.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=finance_supplier_" + supplierId + ".xlsx")
                .header(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }
}
