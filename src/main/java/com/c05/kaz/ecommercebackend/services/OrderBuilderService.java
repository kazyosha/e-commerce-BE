package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.order.CheckoutItemRequest;
import com.c05.kaz.ecommercebackend.dto.order.CheckoutRequest;
import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import com.c05.kaz.ecommercebackend.enums.PaymentMethod;
import com.c05.kaz.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderBuilderService {

    private final CartRepository cartRepo;
    private final UserAccountService userAccountService;
    private final CustomerProfileService customerProfileService;
    private final PromotionService promotionService;

    public List<Order> buildDraftOrders(CheckoutRequest req) {
        UserAccount user = userAccountService.getCurrentCustomer();
        CustomerProfile customer = customerProfileService.getByUser(user);

        Cart cart = cartRepo.findByCustomer(user)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng rỗng."));

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new RuntimeException("Không có sản phẩm nào được chọn để thanh toán.");
        }

        Map<Long, Integer> requestedQtyMap = req.getItems().stream()
                .collect(Collectors.toMap(
                        CheckoutItemRequest::getCartItemId,
                        CheckoutItemRequest::getQuantity
                ));

        List<CartItem> selectedCartItems = cart.getItems().stream()
                .filter(ci -> requestedQtyMap.containsKey(ci.getId()))
                .collect(Collectors.toList());

        if (selectedCartItems.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm hợp lệ trong giỏ.");
        }

        // Validate
        for (CartItem ci : selectedCartItems) {
            Product product = ci.getProduct();
            Integer reqQty = requestedQtyMap.get(ci.getId());

            if (reqQty == null || reqQty <= 0) {
                throw new RuntimeException("Số lượng phải lớn hơn 0.");
            }
            if (!product.isActive()) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " đang ngừng bán.");
            }
            int stock = product.getQuantity() != null ? product.getQuantity() : 0;
            if (stock < reqQty) {
                throw new RuntimeException("Sản phẩm " + product.getName() +
                        " chỉ còn " + stock + " sản phẩm trong kho.");
            }
        }

        Map<SupplierShop, List<CartItem>> bySupplier = selectedCartItems.stream()
                .collect(Collectors.groupingBy(ci -> ci.getProduct().getSupplier()));

        List<Order> draftOrders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<SupplierShop, List<CartItem>> entry : bySupplier.entrySet()) {
            SupplierShop supplier = entry.getKey();
            List<CartItem> items = entry.getValue();

            Order order = new Order();
            order.setSupplier(supplier);
            order.setCustomer(customer);
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentMethod(
                    req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethod.COD
            );
            order.setReceiverName(
                    req.getReceiverName() != null && !req.getReceiverName().isBlank()
                            ? req.getReceiverName()
                            : customer.getFullName()
            );
            order.setReceiverPhone(
                    req.getReceiverPhone() != null && !req.getReceiverPhone().isBlank()
                            ? req.getReceiverPhone()
                            : customer.getPhone()
            );
            order.setReceiverAddress(
                    req.getReceiverAddress() != null && !req.getReceiverAddress().isBlank()
                            ? req.getReceiverAddress()
                            : customer.getAddress()
            );

            order.setCreatedAt(now);
            order.setUpdatedAt(now);

            List<OrderItem> orderItems = new ArrayList<>();
            long originalTotal = 0L;

            for (CartItem ci : items) {
                Product product = ci.getProduct();
                int reqQty = requestedQtyMap.get(ci.getId());

                long unitPrice = product.getPrice();
                long lineTotal = unitPrice * reqQty;
                originalTotal += lineTotal;

                OrderItem oi = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .unitPrice(unitPrice)
                        .quantity(reqQty)
                        .lineTotal(lineTotal)
                        .build();

                orderItems.add(oi);
            }

            long discountAmount = 0L;
            Promotion appliedPromotion = null;
            if (req.getPromotionCode() != null && !req.getPromotionCode().isBlank()) {
                appliedPromotion = promotionService.validateAndGetPromotion(
                        req.getPromotionCode(),
                        supplier.getId(),
                        originalTotal
                );
                if (appliedPromotion != null) {
                    discountAmount = promotionService.calculateDiscount(appliedPromotion, originalTotal);
                    order.setPromotion(appliedPromotion);
                }
            }

            long finalTotal = originalTotal - discountAmount;

            order.setOriginalTotal(originalTotal);
            order.setDiscountAmount(discountAmount);
            order.setFinalTotal(finalTotal);
            order.setItems(orderItems);

            draftOrders.add(order);
        }

        return draftOrders;
    }

    public UserAccount getCurrentUser() {
        return userAccountService.getCurrentCustomer();
    }
}
