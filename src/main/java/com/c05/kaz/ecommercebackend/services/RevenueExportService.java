package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.supplier.SupplierRevenueDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class RevenueExportService {

    public ByteArrayInputStream exportRevenueExcel(List<SupplierRevenueDTO> revenues) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Doanh thu nhà cung cấp");

        // ===== FONT =====
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);

        Font normalFont = workbook.createFont();
        normalFont.setFontHeightInPoints((short) 11);

        // ===== STYLE HEADER =====
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // ===== STYLE BODY LEFT =====
        CellStyle bodyLeft = workbook.createCellStyle();
        bodyLeft.setFont(normalFont);
        bodyLeft.setAlignment(HorizontalAlignment.LEFT);
        bodyLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        bodyLeft.setBorderBottom(BorderStyle.THIN);
        bodyLeft.setBorderTop(BorderStyle.THIN);
        bodyLeft.setBorderLeft(BorderStyle.THIN);
        bodyLeft.setBorderRight(BorderStyle.THIN);

        // ===== STYLE BODY CENTER =====
        CellStyle bodyCenter = workbook.createCellStyle();
        bodyCenter.cloneStyleFrom(bodyLeft);
        bodyCenter.setAlignment(HorizontalAlignment.CENTER);

        // ===== ZEBRA STYLES =====
        CellStyle zebraLeft = workbook.createCellStyle();
        zebraLeft.cloneStyleFrom(bodyLeft);
        zebraLeft.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        zebraLeft.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle zebraCenter = workbook.createCellStyle();
        zebraCenter.cloneStyleFrom(bodyCenter);
        zebraCenter.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        zebraCenter.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // ===== HEADER =====
        String[] columns = {"STT", "Tên cửa hàng", "Doanh thu", "Doanh thu website (5%)"};
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // ===== DATA =====
        int rowIdx = 1;
        int index = 1;

        for (SupplierRevenueDTO dto : revenues) {

            Row row = sheet.createRow(rowIdx);
            boolean isZebra = rowIdx % 2 == 0;

            // STT
            Cell c1 = row.createCell(0);
            c1.setCellValue(index++);
            c1.setCellStyle(isZebra ? zebraCenter : bodyCenter);

            // Tên shop
            Cell c2 = row.createCell(1);
            c2.setCellValue(dto.getShopName());
            c2.setCellStyle(isZebra ? zebraLeft : bodyLeft);

            // Doanh thu
            String revenue = String.format("%,d ₫", dto.getTotalRevenue());

            Cell c3 = row.createCell(2);
            c3.setCellValue(revenue);
            c3.setCellStyle(isZebra ? zebraLeft : bodyLeft);

            // Website fee 3%
            long websiteFee = Math.round(dto.getTotalRevenue() * 0.05);
            String websiteFeeStr = String.format("%,d ₫", websiteFee);

            Cell c4 = row.createCell(3);
            c4.setCellValue(websiteFeeStr);
            c4.setCellStyle(isZebra ? zebraLeft : bodyLeft);

            rowIdx++;
        }

        // ===== AUTO SIZE =====
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return new ByteArrayInputStream(out.toByteArray());
    }
}
