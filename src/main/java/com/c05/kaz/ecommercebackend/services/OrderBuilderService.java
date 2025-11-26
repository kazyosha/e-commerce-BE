package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.discount.DiscountCheckRequest;
import com.c05.kaz.ecommercebackend.dto.discount.DiscountCheckResponse;
import com.c05.kaz.ecommercebackend.dto.order.CheckoutItemRequest;
import com.c05.kaz.ecommercebackend.dto.order.CheckoutRequest;
import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import com.c05.kaz.ecommercebackend.enums.PaymentMethod;
import com.c05.kaz.ecommercebackend.repository.CartRepository;
import com.c05.kaz.ecommercebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderBuilderService {

    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserAccountService userAccountService;
    private final CustomerProfileService customerProfileService;
    private final DiscountService discountService;

    public List<Order> buildDraftOrders(CheckoutRequest req) {

        UserAccount user = userAccountService.getCurrentCustomer();
        CustomerProfile customer = customerProfileService.getByUser(user);

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new RuntimeException("Không có sản phẩm nào được chọn để thanh toán.");
        }

        Cart cart = cartRepo.findByCustomer(user).orElse(null);

        Map<SupplierShop, List<ProductSelection>> itemsBySupplier = new HashMap<>();

        // Gom sản phẩm theo supplier
        for (CheckoutItemRequest itemReq : req.getItems()) {

            Product product;
            int reqQty;

            if (itemReq.getCartItemId() != null) {
                if (cart == null) throw new RuntimeException("Giỏ hàng rỗng.");

                CartItem ci = cart.getItems().stream()
                        .filter(c -> Objects.equals(c.getId(), itemReq.getCartItemId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ."));

                product = ci.getProduct();
                reqQty = itemReq.getQuantity() != null ? itemReq.getQuantity() : ci.getQuantity();
            }
            else if (itemReq.getProductId() != null) {
                product = productRepo.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại."));

                reqQty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            }
            else {
                throw new RuntimeException("Thiếu cartItemId hoặc productId.");
            }

            if (reqQty <= 0) throw new RuntimeException("Số lượng phải lớn hơn 0.");
            if (!product.isActive()) throw new RuntimeException("Sản phẩm đang ngừng bán.");
            if (product.getQuantity() < reqQty) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " không đủ tồn kho.");
            }

            SupplierShop supplier = product.getSupplier();

            itemsBySupplier
                    .computeIfAbsent(supplier, k -> new ArrayList<>())
                    .add(new ProductSelection(product, reqQty));
        }

        List<Order> draftOrders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Build đơn hàng
        for (Map.Entry<SupplierShop, List<ProductSelection>> entry : itemsBySupplier.entrySet()) {

            SupplierShop supplier = entry.getKey();
            List<ProductSelection> selections = entry.getValue();

            Order order = new Order();
            order.setSupplier(supplier);
            order.setCustomer(customer);
            order.setStatus(OrderStatus.PENDING);

            long shippingFee = req.getShippingFee() != null ? req.getShippingFee() : 0L;
            order.setShippingFee(shippingFee);

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
            List<Long> productIds = new ArrayList<>();

            for (ProductSelection ps : selections) {
                Product product = ps.product;
                int qty = ps.quantity;

                long unitPrice = product.getPrice();
                long lineTotal = unitPrice * qty;

                originalTotal += lineTotal;
                productIds.add(product.getId());

                orderItems.add(
                        OrderItem.builder()
                                .order(order)
                                .product(product)
                                .supplier(product.getSupplier())
                                .unitPrice(unitPrice)
                                .quantity(qty)
                                .lineTotal(lineTotal)
                                .build()
                );
            }

            // =========== APPLY DISCOUNT ==============
            long discountAmount = 0L;

            if (req.getDiscountCode() != null && !req.getDiscountCode().isBlank()) {

                DiscountCheckRequest check = new DiscountCheckRequest();
                check.setSupplierId(supplier.getId());
                check.setCode(req.getDiscountCode());
                check.setCartTotal(originalTotal);
                check.setProductIds(productIds);
                check.setUserId(user.getId());

                DiscountCheckResponse res = discountService.checkDiscountForCart(check);

                if (!res.isValid()) {
                    throw new RuntimeException(res.getReason());
                }

                discountAmount = res.getDiscountAmount();

                // ⭐ LƯU discountCode vào đơn hàng để OrderService tăng usedCount
                order.setDiscountCode(req.getDiscountCode());
            }

            long finalTotal = originalTotal - discountAmount + shippingFee;

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

    private static class ProductSelection {
        private final Product product;
        private final int quantity;

        public ProductSelection(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }
}
