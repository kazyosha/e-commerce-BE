package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.order.CheckoutRequest;
import com.c05.kaz.ecommercebackend.dto.order.OrderDetailResponse;
import com.c05.kaz.ecommercebackend.dto.order.OrderResponse;
import com.c05.kaz.ecommercebackend.dto.order.OrderSummaryResponse;
import com.c05.kaz.ecommercebackend.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> checkout(@RequestBody @Valid CheckoutRequest request) {
        try {
            List<OrderSummaryResponse> orders = orderService.checkout(request);
            return ResponseEntity.ok(orders);
        } catch (ResponseStatusException ex) {
            // nếu service ném ResponseStatusException
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("message", ex.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        Long customerId = orderService.getCustomerIdByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        return ResponseEntity.ok(orderService.getAllOrdersOfCustomer(customerId));
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

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            Principal principal
    ) {
        String username = principal.getName();
        Long customerId = orderService.getCustomerIdByUsername(username);

        try {
            OrderResponse res = orderService.cancelOrder(customerId, orderId);
            return ResponseEntity.ok(res);
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("message", ex.getReason()));
        }
    }

    @GetMapping("/cancellable")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> getCancellableOrders(
            Authentication authentication
    ) {
        String username = authentication.getName();
        Long customerId = orderService.getCustomerIdByUsername(username);

        List<OrderResponse> result = orderService.getCancellableOrdersOfCustomer(customerId);
        return ResponseEntity.ok(result);
    }
}