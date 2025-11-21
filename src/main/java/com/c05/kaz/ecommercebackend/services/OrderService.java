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

    @Transactional
    public List<OrderSummaryResponse> checkout(CheckoutRequest req) {

        req.setPaymentMethod(PaymentMethod.COD);
        List<Order> draftOrders = orderBuilderService.buildDraftOrders(req);
        UserAccount user = orderBuilderService.getCurrentUser();

        // giỏ hàng (nếu có)
        Cart cart = cartRepo.findByCustomer(user).orElse(null);

        // cart item cần xoá
        List<Long> cartItemIdsToRemove = req.getItems().stream()
                .map(CheckoutItemRequest::getCartItemId)
                .filter(Objects::nonNull)
                .toList();

        List<OrderSummaryResponse> responses = new ArrayList<>();

        for (Order order : draftOrders) {
            order.setStatus(OrderStatus.PENDING);
            order = orderRepo.save(order);

            // Notify
            notificationService.notifyOrderCreatedForSupplier(
                    order.getSupplier(),
                    user,
                    order.getId(),
                    order.getFinalTotal()
            );

            notificationService.notifyOrderCreatedForCustomer(user, order.getId());

            responses.add(buildOrderSummary(order));
        }

        // xoá cart items
        if (cart != null && !cartItemIdsToRemove.isEmpty()) {
            cart.getItems().removeIf(ci -> cartItemIdsToRemove.contains(ci.getId()));
            cartItemRepo.deleteAllByIdInBatch(cartItemIdsToRemove);

            if (cart.getItems().isEmpty()) cartRepo.delete(cart);
            else cartRepo.save(cart);
        }

        return responses;
    }

    // ====== BUILD SUMMARY ======

    public OrderSummaryResponse buildOrderSummary(Order order) {
        List<OrderItemSummary> items = order.getItems().stream()
                .map(oi -> OrderItemSummary.builder()
                        .productId(oi.getProduct().getId())
                        .productName(oi.getProduct().getName())
                        .thumbnail(
                                oi.getProduct().getImages() != null &&
                                        !oi.getProduct().getImages().isEmpty()
                                        ? oi.getProduct().getImages().get(0).getImageUrl()
                                        : null
                        )
                        .unitPrice(oi.getUnitPrice())
                        .quantity(oi.getQuantity())
                        .lineTotal(oi.getLineTotal())
                        .build()
                ).toList();

        return OrderSummaryResponse.builder()
                .orderId(order.getId())
                .supplierId(order.getSupplier().getId())
                .supplierName(order.getSupplier().getShopName())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress())
                .originalTotal(order.getOriginalTotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .finalTotal(order.getFinalTotal())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
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

        if (order.getStatus() == OrderStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đã huỷ");

        if (order.getStatus() == OrderStatus.SHIPPING ||
                order.getStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể hủy đơn ở trạng thái hiện tại");
        }

        // PENDING → chỉ đổi trạng thái
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            return OrderResponse.fromEntity(orderRepo.save(order));
        }

        // CONFIRMED → rollback stock + sold
        if (order.getStatus() == OrderStatus.CONFIRMED) {

            for (OrderItem oi : order.getItems()) {
                Product p = oi.getProduct();

                // hoàn tồn
                p.setQuantity(
                        Optional.ofNullable(p.getQuantity()).orElse(0)
                                + oi.getQuantity()
                );

                // rollback sold (nếu shop đã cộng nhầm lúc trước)
                long sold = Optional.ofNullable(p.getSoldQuantity()).orElse(0L);
                long newSold = sold - oi.getQuantity();
                p.setSoldQuantity(Math.max(newSold, 0));

                productRepo.save(p);
            }

            order.setStatus(OrderStatus.CANCELLED);
            Order saved = orderRepo.save(order);

            notificationService.notifyOrderCancelledForSupplier(order.getSupplier(), orderId);

            return OrderResponse.fromEntity(saved);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Không thể huỷ đơn");
    }

    // ====== SUPPLIER SIDE ======

    // Shop xác nhận đơn: PENDING → CONFIRMED
    @Transactional
    public OrderResponse supplierConfirmOrder(Long supplierId, Long orderId) {
        Order order = orderRepo.findByIdAndSupplier_Id(orderId, supplierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ xác nhận đơn ở trạng thái PENDING");
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

        // notify khách
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

}
