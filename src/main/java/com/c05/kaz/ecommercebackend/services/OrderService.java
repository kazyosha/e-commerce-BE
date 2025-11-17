package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.order.*;
import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import com.c05.kaz.ecommercebackend.enums.PaymentMethod;
import com.c05.kaz.ecommercebackend.enums.PaymentStatus;
import com.c05.kaz.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderBuilderService orderBuilderService;
    private final OrderRepository orderRepo;
    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepository;

    @Transactional
    public List<OrderSummaryResponse> checkoutCOD(CheckoutRequest req) {
        req.setPaymentMethod(PaymentMethod.COD);

        List<Order> draftOrders = orderBuilderService.buildDraftOrders(req);
        UserAccount user = orderBuilderService.getCurrentUser();
        Cart cart = cartRepo.findByCustomer(user)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng rỗng."));

        List<OrderSummaryResponse> responses = new ArrayList<>();

        for (Order order : draftOrders) {
            // Trừ tồn kho + cộng soldQuantity
            for (OrderItem oi : order.getItems()) {
                Product product = oi.getProduct();
                int stock = product.getQuantity() != null ? product.getQuantity() : 0;
                int qty = oi.getQuantity();

                product.setQuantity(stock - qty);
                long currentSold = product.getSoldQuantity() != null ? product.getSoldQuantity() : 0;
                product.setSoldQuantity(currentSold + qty);

                productRepo.save(product);
            }

            // COD: thanh toán xem như PAID luôn
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.COMPLETED);

            order = orderRepo.save(order);

            // Xoá các cartItem tương ứng
            Order finalOrder = order;
            cart.getItems().removeIf(ci ->
                    finalOrder.getItems().stream().anyMatch(oi -> oi.getProduct().getId().equals(ci.getProduct().getId()))
            );
            cartItemRepo.deleteAllInBatch(cart.getItems());

            // Build response
            responses.add(buildOrderSummary(order));
        }

        if (cart.getItems().isEmpty()) {
            cartRepo.delete(cart);
        }

        return responses;
    }

    public OrderSummaryResponse buildOrderSummary(Order order) {
        List<OrderItemSummary> itemSummaries = order.getItems().stream()
                .map(oi -> {
                    String thumbnail = null;
                    if (oi.getProduct().getImages() != null
                            && !oi.getProduct().getImages().isEmpty()) {
                        thumbnail = oi.getProduct().getImages().get(0).getImageUrl();
                    }
                    return OrderItemSummary.builder()
                            .productId(oi.getProduct().getId())
                            .productName(oi.getProduct().getName())
                            .thumbnail(thumbnail)
                            .unitPrice(oi.getUnitPrice())
                            .quantity(oi.getQuantity())
                            .lineTotal(oi.getLineTotal())
                            .build();
                })
                .toList();

        return OrderSummaryResponse.builder()
                .orderId(order.getId())
                .supplierId(order.getSupplier().getId())
                .supplierName(order.getSupplier().getShopName())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress())
                .originalTotal(order.getOriginalTotal())
                .discountAmount(order.getDiscountAmount())
                .finalTotal(order.getFinalTotal())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .items(itemSummaries)
                .build();
    }

    // Lấy id customer từ username trong token
    public Long getCustomerIdByUsername(String username) {
        CustomerProfile customer = customerRepository.findByUser_Username(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
        return customer.getId();
    }

    // Danh sách đơn đã thanh toán (COMPLETED + paid = true)
    public List<OrderResponse> getPaidOrdersOfCustomer(Long customerId) {
        List<Order> orders =
                orderRepo.findByCustomer_IdAndStatusAndPaidTrueOrderByCreatedAtDesc(
                        customerId,
                        OrderStatus.COMPLETED
                );

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    // Danh sách đơn đang giao (SHIPPING)
    public List<OrderResponse> getShippingOrdersOfCustomer(Long customerId) {
        List<Order> orders =
                orderRepo.findByCustomer_IdAndStatusOrderByCreatedAtDesc(
                        customerId,
                        OrderStatus.SHIPPING
                );

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    // Xác nhận đã nhận hàng → SHIPPING -> COMPLETED + paid = true (nếu cần)
    public OrderResponse confirmReceived(Long customerId, Long orderId) {
        Order order = orderRepo.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể xác nhận những đơn đang giao"
            );
        }

        order.setStatus(OrderStatus.COMPLETED);

        // Nếu là COD, em có thể set paid = true ở đây sau khi khách xác nhận
        if (!order.isPaid()) {
            order.setPaid(true);
        }

        Order saved = orderRepo.save(order);
        return OrderResponse.fromEntity(saved);
    }

    public OrderDetailResponse getOrderDetail(Long customerId, Long orderId) {
        Order order = orderRepo.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        return OrderDetailResponse.fromEntity(order);
    }
}
