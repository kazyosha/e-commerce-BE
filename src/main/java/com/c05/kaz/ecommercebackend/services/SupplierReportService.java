package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.OrderItem;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.repository.OrderItemRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierReportService {

    private final SupplierRepository supplierRepo;
    private final OrderItemRepository orderItemRepo;
    private final UserAccountRepository userRepo;

    public Long getSupplierIdFromUser(UserDetails user) {
        UserAccount acc = userRepo.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        SupplierShop shop = supplierRepo.findByUser_Id(acc.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy supplier"));

        return shop.getId();
    }


    public byte[] exportFinanceExcel(Long supplierId, String from, String to) {

        LocalDateTime start = LocalDate.parse(from).atStartOfDay();
        LocalDateTime end = LocalDate.parse(to).atTime(23, 59, 59);

        List<Object[]> raw = orderItemRepo.findFinanceReport(supplierId, start, end);

        System.out.println("RAW SIZE = " + raw.size());
        for(Object[] r : raw){
            System.out.println(java.util.Arrays.toString(r));
        }

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Báo cáo tài chính");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Tên SP");
            header.createCell(1).setCellValue("SL");
            header.createCell(2).setCellValue("Giá bán");
            header.createCell(3).setCellValue("Doanh số");
            header.createCell(4).setCellValue("Phí sàn 5%");
            header.createCell(5).setCellValue("Tiền hàng");
            header.createCell(6).setCellValue("Doanh thu");
            header.createCell(7).setCellValue("Lợi nhuận");
            header.createCell(8).setCellValue("Ngày cập nhật");

            int index = 1;

            for (Object[] row : raw) {
                Row r = sheet.createRow(index++);

                r.createCell(0).setCellValue((String) row[0]);
                r.createCell(1).setCellValue(((Number) row[1]).longValue());
                r.createCell(2).setCellValue(((Number) row[2]).longValue());
                r.createCell(3).setCellValue(((Number) row[3]).longValue());
                r.createCell(4).setCellValue(((Number) row[4]).longValue());
                r.createCell(5).setCellValue(((Number) row[5]).longValue());
                r.createCell(6).setCellValue(((Number) row[6]).longValue());
                r.createCell(7).setCellValue(((Number) row[7]).longValue());
                r.createCell(8).setCellValue(row[8].toString());
            }

            for (int i = 0; i <= 8; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất báo cáo tài chính", e);
        }
    }
}
