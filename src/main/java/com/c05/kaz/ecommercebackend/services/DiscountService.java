package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.discount.DiscountRequest;
import com.c05.kaz.ecommercebackend.dto.discount.DiscountResponse;
import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.DiscountStatus;
import com.c05.kaz.ecommercebackend.enums.DiscountType;
import com.c05.kaz.ecommercebackend.exception.ResourceNotFoundException;
import com.c05.kaz.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final DiscountUsageLogRepository logRepository;

    // ================================
    // LẤY DS MÃ GIẢM GIÁ CỦA SHOP
    // ================================
    public List<DiscountResponse> getBySupplier(Long supplierId) {

        SupplierShop supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy shop"));

        return discountRepository.findBySupplier(supplier)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ================================
    // TẠO MÃ GIẢM GIÁ
    // ================================
    public DiscountResponse create(Long supplierId, DiscountRequest req) {

        SupplierShop supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy shop"));

        // VALIDATION
        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        if (discountRepository.findByCodeAndSupplier(req.getCode(), supplier).isPresent()) {
            throw new IllegalArgumentException("Mã giảm giá đã tồn tại trong shop");
        }

        if (req.getType().equals("PERCENT") && req.getValue() > 100) {
            throw new IllegalArgumentException("Giảm theo % không được vượt quá 100%");
        }

        if (req.getType().equals("AMOUNT")
                && req.getMinOrderValue() != null
                && req.getValue() > req.getMinOrderValue()) {
            throw new IllegalArgumentException("Giảm tiền không thể vượt quá giá trị đơn tối thiểu");
        }

        // Lấy danh sách sản phẩm áp dụng
        List<Product> products = productRepository.findAllById(req.getProductIds());
        if (products.size() != req.getProductIds().size()) {
            throw new IllegalArgumentException("Tồn tại sản phẩm không hợp lệ");
        }

        for (Product p : products) {
            if (!p.getSupplier().getId().equals(supplierId)) {
                throw new IllegalArgumentException("Sản phẩm không thuộc shop");
            }
        }

        Discount discount = Discount.builder()
                .supplier(supplier)
                .code(req.getCode())
                .type(DiscountType.valueOf(req.getType()))
                .value(req.getValue())
                .minOrderValue(req.getMinOrderValue())
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .totalUsage(req.getTotalUsage())
                .usedCount(0)
                .limitPerUser(req.getLimitPerUser())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .status(DiscountStatus.ACTIVE)
                .applicableProducts(products)
                .build();

        discountRepository.save(discount);

        return mapToResponse(discount);
    }

    // ================================
    // BẬT/TẮT MÃ GIẢM GIÁ
    // ================================
    public DiscountResponse toggleStatus(Long supplierId, Long discountId) {

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá"));

        if (!discount.getSupplier().getId().equals(supplierId)) {
            throw new IllegalStateException("Không có quyền thay đổi mã giảm giá này");
        }

        LocalDateTime now = LocalDateTime.now();

        // ⭐ MÃ ĐÃ HẾT HẠN → TỰ ĐỘNG DISABLED & KHÔNG CHO BẬT LÊN
        if (discount.getEndDate().isBefore(now)) {
            discount.setStatus(DiscountStatus.DISABLED);
            discountRepository.save(discount);
            return mapToResponse(discount);
        }

        // ⭐ MÃ CÒN HẠN → BẬT / TẮT NHƯ BÌNH THƯỜNG
        if (discount.getStatus() == DiscountStatus.ACTIVE) {
            discount.setStatus(DiscountStatus.DISABLED);
        } else {
            discount.setStatus(DiscountStatus.ACTIVE);
        }

        discountRepository.save(discount);

        return mapToResponse(discount);
    }

    // ================================
    // ÁP DỤNG MÃ GIẢM GIÁ CHO ĐƠN HÀNG
    // ================================
    public Long apply(Long supplierId, String code, Long orderValue, UserAccount user) {

        SupplierShop supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop không tồn tại"));

        Discount discount = discountRepository.findByCodeAndSupplier(code, supplier)
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không hợp lệ"));

        // VALIDATION
        if (discount.getStatus() != DiscountStatus.ACTIVE) {
            throw new IllegalStateException("Mã hiện không hoạt động");
        }

        if (LocalDateTime.now().isBefore(discount.getStartDate()) ||
                LocalDateTime.now().isAfter(discount.getEndDate())) {
            throw new IllegalStateException("Mã giảm giá đã hết hạn");
        }

        if (discount.getUsedCount() >= discount.getTotalUsage()) {
            throw new IllegalStateException("Mã giảm giá đã hết lượt sử dụng");
        }

        if (orderValue < discount.getMinOrderValue()) {
            throw new IllegalArgumentException("Đơn hàng chưa đủ điều kiện sử dụng mã");
        }

        long usedAmount = logRepository.countByDiscountIdAndUserId(discount.getId(), user.getId());
        if (usedAmount >= discount.getLimitPerUser()) {
            throw new IllegalStateException("Bạn đã sử dụng tối đa số lần cho phép");
        }

        long reducedAmount;

        if (discount.getType() == DiscountType.PERCENT) {
            reducedAmount = Math.round(orderValue * discount.getValue() / 100);

            if (discount.getMaxDiscountAmount() != null) {
                reducedAmount = Math.min(reducedAmount, discount.getMaxDiscountAmount());
            }

        } else { // AMOUNT
            reducedAmount = discount.getValue().longValue();
        }

        discount.setUsedCount(discount.getUsedCount() + 1);
        discountRepository.save(discount);

        return reducedAmount;
    }

    // ================================
    // MAPPER
    // ================================
    private DiscountResponse mapToResponse(Discount d) {
        DiscountResponse r = new DiscountResponse();
        r.setId(d.getId());
        r.setCode(d.getCode());
        r.setType(d.getType().name());
        r.setValue(d.getValue());
        r.setMinOrderValue(d.getMinOrderValue());
        r.setMaxDiscountAmount(d.getMaxDiscountAmount());
        r.setTotalUsage(d.getTotalUsage());
        r.setUsedCount(d.getUsedCount());
        r.setLimitPerUser(d.getLimitPerUser());
        r.setStatus(d.getStatus().name());
        r.setStartDate(d.getStartDate());
        r.setEndDate(d.getEndDate());
        r.setProductIds(d.getApplicableProducts().stream().map(Product::getId).toList());
        return r;
    }

    public void delete(Long supplierId, Long discountId) {

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá"));

        if (!discount.getSupplier().getId().equals(supplierId)) {
            throw new IllegalStateException("Không có quyền xoá mã giảm giá này");
        }

        // Xoá liên kết với sản phẩm trước
        discount.getApplicableProducts().clear();
        discountRepository.save(discount);

        // Xoá luôn mã giảm giá
        discountRepository.delete(discount);
    }

}
