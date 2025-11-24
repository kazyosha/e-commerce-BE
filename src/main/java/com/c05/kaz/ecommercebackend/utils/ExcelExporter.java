// utils/ExcelExporter.java
package com.c05.kaz.ecommercebackend.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class ExcelExporter {

    public static ByteArrayInputStream exportRevenueExcel(List<Object[]> rows) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Revenue");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Supplier ID");
            header.createCell(1).setCellValue("Supplier Name");
            header.createCell(2).setCellValue("Total Orders");
            header.createCell(3).setCellValue("Total Revenue");

            // Body
            int rowIndex = 1;
            for (Object[] row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(row[0].toString());
                dataRow.createCell(1).setCellValue(row[1].toString());
                dataRow.createCell(2).setCellValue(Integer.parseInt(row[2].toString()));
                dataRow.createCell(3).setCellValue(Double.parseDouble(row[3].toString()));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Cannot export Excel", e);
        }
    }
}
