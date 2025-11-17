package com.c05.kaz.ecommercebackend.controller;


import com.c05.kaz.ecommercebackend.dto.payment.SepayPaymentForm;
import com.c05.kaz.ecommercebackend.entity.Order;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import com.c05.kaz.ecommercebackend.repository.OrderRepository;
import com.c05.kaz.ecommercebackend.services.SepayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment/sepay")
@RequiredArgsConstructor
public class SepayPaymentController {

    private final SepayService sepayService;
    private final OrderRepository orderRepo;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestParam Long orderId) throws Exception {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String invoiceNo = "INV-" + order.getId() + "-" + System.currentTimeMillis();

        order.setInvoiceNo(invoiceNo);
        order.setStatus(OrderStatus.valueOf("PENDING"));
        orderRepo.save(order);

        SepayPaymentForm form = sepayService.createPayment(
                order.getFinalTotal(),
                "Thanh toán đơn hàng #" + order.getId(),
                invoiceNo,
                String.valueOf(order.getCustomer())
        );

        return ResponseEntity.ok(form);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> body) {

        String invoiceNo = body.get("order_invoice_number").toString();
        String status = body.get("status").toString();

        Order order = (Order) orderRepo.findByInvoiceNo(invoiceNo)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (status.equals("PAID")) {
            order.setStatus(OrderStatus.valueOf("PAID"));
        } else {
            order.setStatus(OrderStatus.valueOf("FAILED"));
        }

        orderRepo.save(order);

        return ResponseEntity.ok("OK");
    }
}
