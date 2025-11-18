package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.order.*;
import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import com.c05.kaz.ecommercebackend.enums.PaymentMethod;
import com.c05.kaz.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        // Hiện tại chỉ có COD
        req.setPaymentMethod(PaymentMethod.COD);

        // build các Order nháp (chia theo nhà cung cấp, áp mã giảm giá...)
        List<Order> draftOrders = orderBuilderService.buildDraftOrders(req);
        UserAccount user = orderBuilderService.getCurrentUser();

        // Giỏ hàng (nếu có) để xoá cartItem sau khi checkout
        Cart cart = cartRepo.findByCustomer(user).orElse(null);

        // Các cartItemId được checkout (chỉ khi checkout từ giỏ)
        List<Long> cartItemIdsToRemove = req.getItems().stream()
                .map(CheckoutItemRequest::getCartItemId)
                .filter(Objects::nonNull)
                .toList();

        List<OrderSummaryResponse> responses = new ArrayList<>();

        for (Order order : draftOrders) {
            // ✅ Kiểm tra tồn kho + trừ tồn + cộng soldQuantity
            for (OrderItem oi : order.getItems()) {
                Product product = oi.getProduct();
                int stock = product.getQuantity() != null ? product.getQuantity() : 0;
                int qty = oi.getQuantity();

                if (qty <= 0 || stock < qty) {
                    throw new RuntimeException(
                            "Sản phẩm " + product.getName() + " không đủ tồn kho."
                    );
                }

                product.setQuantity(stock - qty);

                long currentSold =
                        product.getSoldQuantity() != null ? product.getSoldQuantity() : 0;
                product.setSoldQuantity(currentSold + qty);

                productRepo.save(product);
            }

            // 🔥 Khách vừa đặt → chờ shop xác nhận
            order.setStatus(OrderStatus.PENDING);
            // KHÔNG set payment / paid ở đây

            order = orderRepo.save(order);

            responses.add(buildOrderSummary(order));
        }

        // Nếu checkout từ giỏ: xoá các cart item tương ứng
        if (cart != null && !cartItemIdsToRemove.isEmpty()) {
            cart.getItems().removeIf(ci -> cartItemIdsToRemove.contains(ci.getId()));
            cartItemRepo.deleteAllByIdInBatch(cartItemIdsToRemove);

            if (cart.getItems().isEmpty()) {
                cartRepo.delete(cart);
            } else {
                cartRepo.save(cart);
            }
        }

        return responses;
    }

    // ====== BUILD SUMMARY ======

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

    // ====== CUSTOMER SIDE ======

    // Lấy id customer từ username trong token
    public Long getCustomerIdByUsername(String username) {
        CustomerProfile customer = customerRepository.findByUser_Username(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
        return customer.getId();
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
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể xác nhận những đơn đang giao"
            );
        }

        order.setStatus(OrderStatus.COMPLETED);
        // ❌ Không set paid nữa nếu em không muốn quản lý payment tại đây

        Order saved = orderRepo.save(order);
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
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        // Nếu đã hủy rồi
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng đã được hủy trước đó."
            );
        }

        // Không cho hủy nếu đang giao hoặc đã hoàn thành
        if (order.getStatus() == OrderStatus.SHIPPING
                || order.getStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể hủy đơn hàng ở trạng thái hiện tại."
            );
        }

        // Nếu PENDING: chỉ cần chuyển sang CANCELLED
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);

            Order saved = orderRepo.save(order);
            return OrderResponse.fromEntity(saved);
        }

        // Nếu CONFIRMED: thông báo cho cửa hàng + cộng lại tồn kho rồi hủy
        if (order.getStatus() == OrderStatus.CONFIRMED) {

            // Thông báo tới cửa hàng (supplier)
            notificationService.notifyOrderCancelledForSupplier(order.getSupplier(), orderId);

            order.setStatus(OrderStatus.CANCELLED);
            restoreStock(order);

            Order saved = orderRepo.save(order);
            return OrderResponse.fromEntity(saved);
        }

        // Các trạng thái khác (nếu có) – fallback
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Không thể hủy đơn hàng ở trạng thái hiện tại."
        );
    }

    private void restoreStock(Order order) {
        for (OrderItem oi : order.getItems()) {
            Product product = oi.getProduct();
            int stock = product.getQuantity() != null ? product.getQuantity() : 0;
            product.setQuantity(stock + oi.getQuantity());
            productRepo.save(product);
        }
    }

    // ====== SUPPLIER SIDE ======

    // Shop xác nhận đơn: PENDING → CONFIRMED
    @Transactional
    public OrderResponse supplierConfirmOrder(Long supplierId, Long orderId) {
        Order order = orderRepo.findByIdAndSupplier_Id(orderId, supplierId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        // 🔥 LOG để debug
        System.out.println("[CONFIRM] orderId=" + orderId
                + ", supplierId=" + supplierId
                + ", statusInDB=" + order.getStatus());

        // Nếu trạng thái khác PENDING => trả 400 + kèm trạng thái hiện tại
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể xác nhận đơn ở trạng thái PENDING. Trạng thái hiện tại: "
                            + (order.getStatus() == null ? "null" : order.getStatus().name())
            );
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepo.save(order);

        // Thông báo cho khách
        try {
            notificationService.notifyOrderConfirmedForCustomer(
                    order.getCustomer().getUser(),
                    order.getId()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return OrderResponse.fromEntity(saved);
    }

    // Shop đánh dấu đang giao: CONFIRMED → SHIPPING
    @Transactional
    public OrderResponse supplierMarkShipping(Long supplierId, Long orderId) {
        Order order = orderRepo.findByIdAndSupplier_Id(orderId, supplierId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        System.out.println("[SHIPPING] orderId=" + orderId
                + ", supplierId=" + supplierId
                + ", statusInDB=" + order.getStatus());

        if (order.getStatus() == null || order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể chuyển sang SHIPPING từ trạng thái CONFIRMED. Trạng thái hiện tại: "
                            + (order.getStatus() == null ? "null" : order.getStatus().name())
            );
        }

        order.setStatus(OrderStatus.SHIPPING);
        Order saved = orderRepo.save(order);

        try {
            notificationService.notifyOrderShippingForCustomer(
                    order.getCustomer().getUser(),
                    order.getId()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

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
}
