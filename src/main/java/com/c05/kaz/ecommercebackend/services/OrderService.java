package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.order.*;
import com.c05.kaz.ecommercebackend.dto.supplier.SupplierOrderDetailResponse;
import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import com.c05.kaz.ecommercebackend.enums.PaymentMethod;
import com.c05.kaz.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderBuilderService orderBuilderService;
    private final OrderRepository orderRepo;
    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;

    // ⭐ THÊM 2 REPOSITORY ĐỂ GHI LOG & TĂNG USEDCOUNT
    private final DiscountRepository discountRepository;
    private final DiscountUsageLogRepository discountUsageLogRepository;

    @Transactional
    public List<OrderSummaryResponse> checkout(CheckoutRequest req) {

        req.setPaymentMethod(PaymentMethod.COD);

        UserAccount user = orderBuilderService.getCurrentUser();
        Cart cart = cartRepo.findByCustomer(user).orElse(null);

        List<Order> draftOrders = orderBuilderService.buildDraftOrders(req);

        List<Long> removeIds = req.getItems().stream()
                .map(CheckoutItemRequest::getCartItemId)
                .filter(Objects::nonNull)
                .toList();

        List<OrderSummaryResponse> responses = new ArrayList<>();

        for (Order order : draftOrders) {

            // ⭐⭐⭐ Không để JPA overwrite discountCode / discountAmount
            String discountCode = order.getDiscountCode();
            Long discountAmount = order.getDiscountAmount();

            // save order + items
            order = orderRepo.save(order);

            // restore tránh mất do merge
            order.setDiscountCode(discountCode);
            order.setDiscountAmount(discountAmount);
            order = orderRepo.save(order);

            // Lưu log sử dụng voucher
            if (discountCode != null) {
                discountRepository.findByCode(discountCode).ifPresent(discount -> {

                    discount.setUsedCount(discount.getUsedCount() + 1);
                    discountRepository.save(discount);

                    discountUsageLogRepository.save(
                            DiscountUsageLog.builder()
                                    .discount(discount)
                                    .user(user)
                                    .usedAt(LocalDateTime.now())
                                    .build()
                    );
                });
            }

            // realtime
            notificationService.pushOrderStatusToSupplier(
                    order.getSupplier().getId(), order.getId(), "PENDING");

            notificationService.pushOrderStatusToCustomer(
                    order.getCustomer().getId(), order.getId(), "PENDING");

            notificationService.notifyOrderCreatedForSupplier(
                    order.getSupplier(),
                    order.getCustomer().getUser(),
                    order.getId(),
                    order.getFinalTotal()
            );

            notificationService.notifyOrderCreatedForCustomer(
                    order.getCustomer().getUser(),
                    order.getId()
            );

            responses.add(buildSummary(order));
        }

        // Xoá sản phẩm khỏi giỏ
        if (cart != null && !removeIds.isEmpty()) {
            cartItemRepo.deleteAllByIdInBatch(removeIds);
        }

        return responses;
    }

    // Build response
    private OrderSummaryResponse buildSummary(Order order) {
        List<OrderItemSummary> items = order.getItems().stream()
                .map(oi -> new OrderItemSummary(
                        oi.getProduct().getId(),
                        oi.getProduct().getName(),
                        oi.getProduct().getImages().isEmpty()
                                ? null
                                : oi.getProduct().getImages().get(0).getImageUrl(),
                        oi.getUnitPrice(),
                        oi.getQuantity(),
                        oi.getLineTotal()
                ))
                .toList();

        return new OrderSummaryResponse(
                order.getId(),
                order.getSupplier().getId(),
                order.getSupplier().getShopName(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getOriginalTotal(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getFinalTotal(),
                order.getDiscountCode(),
                order.getStatus().name(),
                order.getCreatedAt(),
                items
        );
    }

    // ====== CUSTOMER SIDE ======

    // Lấy id customer từ username trong token
    public Long getCustomerIdByUsername(String username) {
        CustomerProfile customer = customerRepository.findByUser_Username(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
        return customer.getId();
    }

    public List<OrderResponse> getAllOrdersOfCustomer(Long customerId) {
        List<Order> orders =
                orderRepo.findByCustomer_IdOrderByCreatedAtDesc(customerId);
        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    // Danh sách đơn đã thanh toán (nếu em vẫn dùng field paid + COMPLETED)
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

    // Khách xác nhận đã nhận hàng → SHIPPING -> COMPLETED
    @Transactional
    public OrderResponse confirmReceived(Long customerId, Long orderId) {

        Order order = orderRepo.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ xác nhận đơn SHIPPED");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setPaid(true);
        order.setUpdatedAt(LocalDateTime.now());

        // tăng sold khi hoàn tất
        for (OrderItem oi : order.getItems()) {
            Product p = oi.getProduct();
            long sold = Optional.ofNullable(p.getSoldQuantity()).orElse(0L);
            p.setSoldQuantity(sold + oi.getQuantity());
            productRepo.save(p);
        }

        Order saved = orderRepo.save(order);

        // 🔥 REALTIME
        notificationService.pushOrderStatusToSupplier(
                order.getSupplier().getId(), orderId, "COMPLETED"
        );
        notificationService.pushOrderStatusToCustomer(
                customerId, orderId, "COMPLETED"
        );

        notificationService.notifyOrderCompletedForSupplier(
                order.getSupplier(), orderId, order.getCustomer().getUser()
        );

        return OrderResponse.fromEntity(saved);
    }

    public OrderDetailResponse getOrderDetail(Long customerId, Long orderId) {
        Order order = orderRepo.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        return OrderDetailResponse.fromEntity(order);
    }

    // Khách huỷ đơn
    @Transactional
    public OrderResponse cancelOrder(Long customerId, Long orderId) {

        Order order = orderRepo.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.CANCELLED ||
                order.getStatus() == OrderStatus.SHIPPING ||
                order.getStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể huỷ đơn ở trạng thái này");
        }

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            return OrderResponse.fromEntity(orderRepo.save(order));
        }

        // CONFIRMED → rollback stock + sold
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            for (OrderItem oi : order.getItems()) {
                Product p = oi.getProduct();

                p.setQuantity(
                        Optional.ofNullable(p.getQuantity()).orElse(0)
                                + oi.getQuantity()
                );

                long sold = Optional.ofNullable(p.getSoldQuantity()).orElse(0L);
                long newSold = sold - oi.getQuantity();
                p.setSoldQuantity(Math.max(newSold, 0));

                p.setQuantity(p.getQuantity() + oi.getQuantity());
                productRepo.save(p);
            }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepo.save(order);

        // 🔥 REALTIME
        notificationService.pushOrderStatusToSupplier(
                order.getSupplier().getId(), orderId, "CANCELLED"
        );
        notificationService.pushOrderStatusToCustomer(
                customerId, orderId, "CANCELLED"
        );

        notificationService.notifyOrderCancelledForSupplier(order.getSupplier(), orderId);

            return OrderResponse.fromEntity(saved);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Không thể huỷ đơn");
    }

    // ====== SUPPLIER SIDE ======
    @Transactional
    public OrderResponse supplierConfirmOrder(Long supplierId, Long orderId) {
        Order order = orderRepo.findByIdAndSupplier_Id(orderId, supplierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ xác nhận đơn PENDING");
        }

        // Trừ tồn kho ngay khi shop xác nhận
        for (OrderItem oi : order.getItems()) {
            Product p = oi.getProduct();

            int stock = Optional.ofNullable(p.getQuantity()).orElse(0);
            if (stock < oi.getQuantity()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + p.getName() + " không đủ tồn kho");
            }

            p.setQuantity(stock - oi.getQuantity());
            productRepo.save(p);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepo.save(order);

        // 🔥 REALTIME
        notificationService.pushOrderStatusToCustomer(
                order.getCustomer().getId(), orderId, "CONFIRMED"
        );
        notificationService.pushOrderStatusToSupplier(
                supplierId, orderId, "CONFIRMED"
        );

        // thông báo bình thường
        notificationService.notifyOrderConfirmedForCustomer(
                order.getCustomer().getUser(),
                order.getId()
        );

        return OrderResponse.fromEntity(saved);
    }

    // Shop đánh dấu đang giao: CONFIRMED → SHIPPING
    @Transactional
    public OrderResponse supplierMarkShipping(Long supplierId, Long orderId) {
        Order order = orderRepo.findByIdAndSupplier_Id(orderId, supplierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ chuyển sang SHIPPING từ CONFIRMED");
        }

        order.setStatus(OrderStatus.SHIPPING);
        Order saved = orderRepo.save(order);

        // 🔥 REALTIME
        notificationService.pushOrderStatusToCustomer(
                order.getCustomer().getId(), orderId, "SHIPPING"
        );
        notificationService.pushOrderStatusToSupplier(
                supplierId, orderId, "SHIPPING"
        );

        notificationService.notifyOrderShippingForCustomer(
                order.getCustomer().getUser(), orderId
        );

        return OrderResponse.fromEntity(saved);
    }

    public List<OrderResponse> getCancellableOrdersOfCustomer(Long customerId) {
        // Các trạng thái còn được phép huỷ
        var cancellableStatuses = List.of(
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED
        );

        List<Order> orders = orderRepo.findByCustomer_IdAndStatusInOrderByCreatedAtDesc(
                customerId,
                cancellableStatuses
        );

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    public SupplierOrderDetailResponse getOrderDetailForSupplier(Long orderId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn"));

        UserAccount current = orderBuilderService.getCurrentUser();

        if (!order.getSupplier().getUser().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xem đơn này");
        }

        return SupplierOrderDetailResponse.from(order);
    }

    @Transactional
    public OrderResponse supplierRejectOrder(Long orderId, SupplierRejectRequest req) {

        UserAccount supplier = orderBuilderService.getCurrentUser();

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn"));

        if (!order.getSupplier().getUser().getId().equals(supplier.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền từ chối đơn này");
        }

        if (!(order.getStatus() == OrderStatus.PENDING ||
                order.getStatus() == OrderStatus.CONFIRMED)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể từ chối đơn ở trạng thái này");
        }

        // rollback stock nếu đã xác nhận
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                p.setQuantity(p.getQuantity() + item.getQuantity());
                productRepo.save(p);
            }
        }

        order.setStatus(OrderStatus.REJECTED);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepo.save(order);

        // 🔥 REALTIME
        notificationService.pushOrderStatusToCustomer(
                order.getCustomer().getId(), orderId, "REJECTED"
        );
        notificationService.pushOrderStatusToSupplier(
                order.getSupplier().getId(), orderId, "REJECTED"
        );

        notificationService.notifyOrderRejectedForCustomer(
                order.getCustomer().getUser(),
                orderId,
                "Lý do từ chối: " + req.getReason()
        );

        return OrderResponse.fromEntity(saved);
    }

    public List<OrderResponse> getRejectedOrdersOfCustomer(Long customerId) {

        List<Order> orders =
                orderRepo.findByCustomer_IdAndStatusOrderByCreatedAtDesc(
                        customerId,
                        OrderStatus.REJECTED
                );

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }
}


