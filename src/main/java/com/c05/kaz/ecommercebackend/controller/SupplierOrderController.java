package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.order.OrderResponse;
import com.c05.kaz.ecommercebackend.dto.order.SupplierRejectRequest;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import com.c05.kaz.ecommercebackend.repository.OrderRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.services.OrderBuilderService;
import com.c05.kaz.ecommercebackend.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierOrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepo;
    private final OrderBuilderService currentUserService; // service tự viết để lấy UserAccount hiện tại
    private final SupplierRepository supplierRepository;
    // Lấy user hiện tại
    private Long getCurrentSupplierId() {
        UserAccount user = currentUserService.getCurrentUser();

        SupplierShop shop = (SupplierShop) supplierRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop của tài khoản"));

        return shop.getId();
    }

    // Danh sách đơn PENDING / CONFIRMED / SHIPPING... cho shop
    @GetMapping
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @RequestParam(required = false) String status
    ) {
        Long supplierId = getCurrentSupplierId();

        List<OrderResponse> result;
        if (status == null || status.isBlank()) {
            result = orderRepo.findBySupplier_IdOrderByCreatedAtDesc(supplierId)
                    .stream()
                    .map(OrderResponse::fromEntity)
                    .toList();
        } else {
            OrderStatus st = OrderStatus.valueOf(status.toUpperCase());
            result = orderRepo.findBySupplier_IdAndStatusOrderByCreatedAtDesc(supplierId, st)
                    .stream()
                    .map(OrderResponse::fromEntity)
                    .toList();
        }

        return ResponseEntity.ok(result);
    }

    // ✅ Chủ shop xác nhận đơn: PENDING → CONFIRMED
    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable Long orderId) {
        Long supplierId = getCurrentSupplierId();
        OrderResponse res = orderService.supplierConfirmOrder(supplierId, orderId);
        return ResponseEntity.ok(res);
    }

    // ✅ Chủ shop chuyển sang đang giao: CONFIRMED → SHIPPING
    @PostMapping("/{orderId}/shipping")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<OrderResponse> markShipping(@PathVariable Long orderId) {
        Long supplierId = getCurrentSupplierId();
        OrderResponse res = orderService.supplierMarkShipping(supplierId, orderId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetailForSupplier(orderId));
    }

    @PostMapping("/{orderId}/reject")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<?> rejectOrder(
            @PathVariable Long orderId,
            @RequestBody SupplierRejectRequest request
    ) {
        OrderResponse response = orderService.supplierRejectOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

}
