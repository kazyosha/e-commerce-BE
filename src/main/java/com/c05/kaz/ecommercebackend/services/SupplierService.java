package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.supplier.MonthlyRevenueDTO;
import com.c05.kaz.ecommercebackend.dto.supplier.SupplierRevenueDTO;
import com.c05.kaz.ecommercebackend.dto.supplier.SupplierRevenueListDTO;
import com.c05.kaz.ecommercebackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final OrderRepository orderRepository;

    /* ==============================================
       LẤY DOANH THU 1 SUPPLIER (KHÔNG DISCOUNT)
    ============================================== */
    public SupplierRevenueDTO getRevenueBySupplier(Long supplierId) {

        List<Object[]> raw = orderRepository.getRevenueBySupplier(supplierId);

        if (raw.isEmpty()) {
            return new SupplierRevenueDTO(
                    supplierId, "Unknown", 0L,
                    0L, 0L, 0L, 0L
            );
        }

        Object[] row = raw.get(0);

        Long totalOrders = ((Number) row[2]).longValue();
        Long originalTotal = ((Number) row[3]).longValue();
        Long discount = ((Number) row[4]).longValue();   // ⭐ totalDiscount từ DB

        Long websiteFee = Math.round(originalTotal * 0.05); // 5%

        Long storeRevenue = originalTotal - websiteFee - discount; // ⭐ TRỪ DISCOUNT

        return new SupplierRevenueDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                totalOrders,
                originalTotal,
                discount,
                websiteFee,
                storeRevenue
        );
    }

    /* ==============================================
       LẤY DOANH THU TẤT CẢ SUPPLIER (KHÔNG DISCOUNT)
    ============================================== */
    public SupplierRevenueListDTO getAllSuppliersRevenue() {

        List<Object[]> raw = orderRepository.getAllSuppliersRevenue();
        List<SupplierRevenueDTO> result = new ArrayList<>();

        long totalAllRevenue = 0;

        for (Object[] row : raw) {

            Long supplierId = ((Number) row[0]).longValue();
            String shopName = (String) row[1];
            Long totalOrders = ((Number) row[2]).longValue();
            Long originalTotal = ((Number) row[3]).longValue();
            Long discount = ((Number) row[4]).longValue();  // ⭐ tổng discount

            Long websiteFee = Math.round(originalTotal * 0.05);
            Long storeRevenue = originalTotal - websiteFee - discount;

            result.add(new SupplierRevenueDTO(
                    supplierId,
                    shopName,
                    totalOrders,
                    originalTotal,
                    discount,
                    websiteFee,
                    storeRevenue
            ));

            totalAllRevenue += originalTotal;
        }

        return new SupplierRevenueListDTO(result, totalAllRevenue);
    }


    /* ==============================================
          EXPORT EXCEL 1 SUPPLIER
    ============================================== */
    public byte[] exportSupplierRevenueExcel(Long supplierId) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Revenue");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Tên cửa hàng");
            header.createCell(1).setCellValue("Tổng đơn");
            header.createCell(2).setCellValue("Tổng doanh số");
            header.createCell(3).setCellValue("Phí website (5%)");
            header.createCell(4).setCellValue("Doanh thu cửa hàng");

            SupplierRevenueDTO dto = getRevenueBySupplier(supplierId);

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(dto.getShopName());
            row.createCell(1).setCellValue(dto.getTotalOrders());
            row.createCell(2).setCellValue(dto.getOriginalTotal());
            row.createCell(3).setCellValue(dto.getWebsiteFee());
            row.createCell(4).setCellValue(dto.getStoreRevenue());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Export Excel failed", e);
        }
    }

    /* ==============================================
          EXPORT EXCEL ALL SUPPLIER
    ============================================== */
    public byte[] exportAllSuppliersRevenueExcel() {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Revenue");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Tên cửa hàng");
            header.createCell(2).setCellValue("Tổng đơn");
            header.createCell(3).setCellValue("Tổng doanh số");
            header.createCell(4).setCellValue("Phí website (5%)");
            header.createCell(5).setCellValue("Doanh thu cửa hàng");

            SupplierRevenueListDTO data = getAllSuppliersRevenue();

            int rowIndex = 1;

            for (SupplierRevenueDTO dto : data.getItems()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(dto.getSupplierId());
                row.createCell(1).setCellValue(dto.getShopName());
                row.createCell(2).setCellValue(dto.getTotalOrders());
                row.createCell(3).setCellValue(dto.getOriginalTotal());
                row.createCell(4).setCellValue(dto.getWebsiteFee());
                row.createCell(5).setCellValue(dto.getStoreRevenue());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Export Excel failed", e);
        }
    }
    public List<MonthlyRevenueDTO> getMonthlyRevenue(int year) {
        List<Object[]> raw = orderRepository.getMonthlyRevenue(year);

        List<MonthlyRevenueDTO> list = new ArrayList<>();
        for (Object[] row : raw) {
            list.add(new MonthlyRevenueDTO(
                    ((Number) row[0]).intValue(),
                    ((Number) row[1]).longValue()
            ));
        }

        List<MonthlyRevenueDTO> full = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Long rev = 0L;

            for (MonthlyRevenueDTO dto : list) {
                if (dto.getMonth() == m) {
                    rev = dto.getRevenue();
                    break;
                }
            }

            full.add(new MonthlyRevenueDTO(m, rev));
        }

        return full;
    }
}
