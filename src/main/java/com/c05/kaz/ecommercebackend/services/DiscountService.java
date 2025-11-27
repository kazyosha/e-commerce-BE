package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.discount.DiscountCheckRequest;
import com.c05.kaz.ecommercebackend.dto.discount.DiscountCheckResponse;
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

        List<Discount> list = discountRepository.findBySupplier(supplier);

        LocalDateTime now = LocalDateTime.now();

        // ⭐ AUTO UPDATE STATUS IF EXPIRED
        boolean changed = false;
        for (Discount d : list) {
            if (d.getStatus() == DiscountStatus.ACTIVE && d.getEndDate().isBefore(now)) {
                d.setStatus(DiscountStatus.DISABLED);
                changed = true;
            }
        }
        if (changed) discountRepository.saveAll(list);

        return list.stream().map(this::mapToResponse).toList();
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

        // ⭐ HẾT HẠN → không cho bật lên
        if (discount.getEndDate().isBefore(now)) {
            discount.setStatus(DiscountStatus.DISABLED);
            discountRepository.save(discount);
            return mapToResponse(discount);
        }

        // ⭐ CÒN HẠN → bật/tắt bình thường
        if (discount.getStatus() == DiscountStatus.ACTIVE) {
            discount.setStatus(DiscountStatus.DISABLED);
        } else {
            discount.setStatus(DiscountStatus.ACTIVE);
        }

        discountRepository.save(discount);

        return mapToResponse(discount);
    }

    // ================================
    // ÁP DỤNG MÃ GIẢM GIÁ (TRỪ LƯỢT)
    // ================================
    public Long apply(Long supplierId, String code, Long orderValue, UserAccount user) {

        SupplierShop supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop không tồn tại"));

        Discount discount = discountRepository.findByCodeAndSupplier(code, supplier)
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không hợp lệ"));

        // VALIDATION
        if (discount.getStatus() != DiscountStatus.ACTIVE)
            throw new IllegalStateException("Mã hiện không hoạt động");

        if (LocalDateTime.now().isBefore(discount.getStartDate())
                || LocalDateTime.now().isAfter(discount.getEndDate()))
            throw new IllegalStateException("Mã giảm giá đã hết hạn");

        if (discount.getUsedCount() >= discount.getTotalUsage())
            throw new IllegalStateException("Mã giảm giá đã hết lượt sử dụng");

        if (orderValue < discount.getMinOrderValue())
            throw new IllegalArgumentException("Đơn hàng chưa đủ điều kiện sử dụng mã");

        long usedAmount = logRepository.countByDiscountIdAndUserId(discount.getId(), user.getId());
        if (usedAmount >= discount.getLimitPerUser())
            throw new IllegalStateException("Bạn đã sử dụng tối đa số lần cho phép");

        long reducedAmount;

        if (discount.getType() == DiscountType.PERCENT) {

            reducedAmount = Math.round(orderValue * discount.getValue() / 100);

            if (discount.getMaxDiscountAmount() != null) {
                reducedAmount = Math.min(reducedAmount, discount.getMaxDiscountAmount());
            }

        } else {
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

    // ================================
    // XOÁ MÃ GIẢM GIÁ
    // ================================
    public void delete(Long supplierId, Long discountId) {

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá"));

        if (!discount.getSupplier().getId().equals(supplierId)) {
            throw new IllegalStateException("Không có quyền xoá mã giảm giá này");
        }

        discount.getApplicableProducts().clear();
        discountRepository.save(discount);

        discountRepository.delete(discount);
    }

    // ================================
    // LẤY VOUCHER (HIỂN THỊ CHO USER)
    // ================================
    public List<DiscountResponse> getAvailableDiscountsForUser(Long supplierId) {

        SupplierShop supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop không tồn tại"));

        LocalDateTime now = LocalDateTime.now();

        return discountRepository.findBySupplier(supplier)
                .stream()
                .filter(d ->
                        d.getStatus() == DiscountStatus.ACTIVE &&
                                d.getStartDate().isBefore(now) &&
                                d.getEndDate().isAfter(now)
                )
                .map(this::mapToResponse)
                .toList();
    }

    // ================================
    // CHECK MÃ GIẢM GIÁ CHO GIỎ HÀNG (KHÔNG TRỪ LƯỢT)
    // ================================
    public DiscountCheckResponse checkDiscountForCart(DiscountCheckRequest req) {

        SupplierShop supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop không tồn tại"));

        Discount discount = discountRepository.findByCodeAndSupplier(req.getCode(), supplier)
                .orElse(null);

        if (discount == null) {
            return new DiscountCheckResponse(false, 0L, "Mã không tồn tại");
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(discount.getStartDate()) || now.isAfter(discount.getEndDate()))
            return new DiscountCheckResponse(false, 0L, "Mã đã hết hạn");

        if (discount.getStatus() != DiscountStatus.ACTIVE)
            return new DiscountCheckResponse(false, 0L, "Mã đang tắt");

        if (discount.getUsedCount() >= discount.getTotalUsage())
            return new DiscountCheckResponse(false, 0L, "Mã đã hết lượt");

        boolean matchProduct =
                discount.getApplicableProducts().stream()
                        .anyMatch(p -> req.getProductIds().contains(p.getId()));

        if (!matchProduct)
            return new DiscountCheckResponse(false, 0L, "Không áp dụng cho sản phẩm trong giỏ");

        if (discount.getMinOrderValue() != null
                && req.getCartTotal() < discount.getMinOrderValue())
            return new DiscountCheckResponse(false, 0L, "Chưa đạt giá trị đơn tối thiểu");

        if (req.getUserId() != null && discount.getLimitPerUser() != null) {
            long usedByUser = logRepository.countByDiscountIdAndUserId(discount.getId(), req.getUserId());
            if (usedByUser >= discount.getLimitPerUser())
                return new DiscountCheckResponse(false, 0L, "Đã sử dụng tối đa số lần");
        }

        long discountAmount;

        if (discount.getType() == DiscountType.PERCENT) {

            discountAmount = Math.round(req.getCartTotal() * discount.getValue() / 100);

            if (discount.getMaxDiscountAmount() != null) {
                discountAmount = Math.min(discountAmount, discount.getMaxDiscountAmount());
            }

        } else {
            discountAmount = discount.getValue().longValue();
        }

        return new DiscountCheckResponse(true, discountAmount, null);
    }

    // ================================
    // CHECK MÃ GIẢM GIÁ (PUBLIC API)
    // ================================
    public Long checkDiscount(Long supplierId, String code, Long cartTotal, List<Long> productIds) {

        SupplierShop supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Shop không tồn tại"));

        Discount discount = discountRepository.findByCodeAndSupplier(code, supplier)
                .orElseThrow(() -> new RuntimeException("Mã không tồn tại"));

        if (LocalDateTime.now().isBefore(discount.getStartDate()))
            throw new RuntimeException("Mã chưa đến thời gian sử dụng");

        if (LocalDateTime.now().isAfter(discount.getEndDate()))
            throw new RuntimeException("Mã đã hết hạn");

        if (discount.getStatus() != DiscountStatus.ACTIVE)
            throw new RuntimeException("Mã không hoạt động");

        if (cartTotal < discount.getMinOrderValue())
            throw new RuntimeException("Chưa đạt giá trị đơn tối thiểu");

        if (!discount.getApplicableProducts().isEmpty()) {
            boolean ok =
                    productIds.stream().anyMatch(id ->
                            discount.getApplicableProducts().stream()
                                    .anyMatch(p -> p.getId().equals(id))
                    );

            if (!ok) {
                throw new RuntimeException("Mã không áp dụng cho sản phẩm này");
            }
        }

        long reduced = 0L;

        if (discount.getType() == DiscountType.PERCENT) {

            reduced = Math.round(cartTotal * discount.getValue() / 100);

            if (discount.getMaxDiscountAmount() != null) {
                reduced = Math.min(reduced, discount.getMaxDiscountAmount());
            }

        } else {
            reduced = discount.getValue().longValue();
        }

        return reduced;
    }

}
