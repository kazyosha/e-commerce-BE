package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.order.CheckoutRequest;
import com.c05.kaz.ecommercebackend.dto.order.OrderDetailResponse;
import com.c05.kaz.ecommercebackend.dto.order.OrderResponse;
import com.c05.kaz.ecommercebackend.dto.order.OrderSummaryResponse;
import com.c05.kaz.ecommercebackend.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request) {
        try {
            List<OrderSummaryResponse> orders = orderService.checkoutCOD(request);
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    // ===== ĐƠN ĐÃ THANH TOÁN =====
    @GetMapping("/paid")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> getPaidOrders(Principal principal) {
        String username = principal.getName();
        Long customerId = orderService.getCustomerIdByUsername(username);

        List<OrderResponse> orders = orderService.getPaidOrdersOfCustomer(customerId);
        return ResponseEntity.ok(orders);
    }

    // ===== ĐƠN ĐANG GIAO =====
    @GetMapping("/shipping")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> getShippingOrders(Principal principal) {
        String username = principal.getName();
        Long customerId = orderService.getCustomerIdByUsername(username);

        List<OrderResponse> orders = orderService.getShippingOrdersOfCustomer(customerId);
        return ResponseEntity.ok(orders);
    }

    // ===== XÁC NHẬN ĐÃ NHẬN HÀNG =====
    @PutMapping("/{orderId}/received")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> confirmReceived(
            @PathVariable Long orderId,
            Principal principal
    ) {
        String username = principal.getName();
        Long customerId = orderService.getCustomerIdByUsername(username);

        OrderResponse updated = orderService.confirmReceived(customerId, orderId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @PathVariable Long orderId,
            Principal principal
    ) {
        String username = principal.getName();
        Long customerId = orderService.getCustomerIdByUsername(username);

        OrderDetailResponse detail = orderService.getOrderDetail(customerId, orderId);
        return ResponseEntity.ok(detail);
    }
}