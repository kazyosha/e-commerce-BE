package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.discount.DiscountCheckRequest;
import com.c05.kaz.ecommercebackend.dto.discount.DiscountCheckResponse;
import com.c05.kaz.ecommercebackend.dto.order.CheckoutItemRequest;
import com.c05.kaz.ecommercebackend.dto.order.CheckoutRequest;
import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
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
            throw new RuntimeException("Không có sản phẩm để thanh toán.");
        }

        Cart cart = cartRepo.findByCustomer(user).orElse(null);

        // Gom sản phẩm theo từng supplier
        Map<SupplierShop, List<ProductSelection>> itemsBySupplier = new HashMap<>();

        for (CheckoutItemRequest itemReq : req.getItems()) {
            Product product;
            int qty;

            if (itemReq.getCartItemId() != null) {
                if (cart == null) throw new RuntimeException("Giỏ hàng rỗng.");

                CartItem ci = cart.getItems().stream()
                        .filter(c -> c.getId().equals(itemReq.getCartItemId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ."));

                product = ci.getProduct();
                qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : ci.getQuantity();

            } else {
                product = productRepo.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

                qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            }

            if (qty <= 0) throw new RuntimeException("Số lượng không hợp lệ.");
            if (!product.isActive()) throw new RuntimeException("Sản phẩm ngừng bán.");
            if (product.getQuantity() < qty)
                throw new RuntimeException("Không đủ tồn kho cho: " + product.getName());

            itemsBySupplier.computeIfAbsent(product.getSupplier(), k -> new ArrayList<>())
                    .add(new ProductSelection(product, qty));
        }

        List<Order> draftOrders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Lặp theo từng shop → tạo 1 order / shop
        for (var entry : itemsBySupplier.entrySet()) {

            SupplierShop supplier = entry.getKey();
            List<ProductSelection> selections = entry.getValue();

            Order order = new Order();
            order.setSupplier(supplier);
            order.setCustomer(customer);
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentMethod(req.getPaymentMethod());
            order.setReceiverName(req.getReceiverName());
            order.setReceiverPhone(req.getReceiverPhone());
            order.setReceiverAddress(req.getReceiverAddress());
            order.setShippingFee(req.getShippingFee() == null ? 0L : req.getShippingFee());
            order.setCreatedAt(now);
            order.setUpdatedAt(now);

            long originalTotal = 0L;
            List<OrderItem> orderItems = new ArrayList<>();
            List<Long> productIds = new ArrayList<>();

            // Tính tiền từng sản phẩm trong đơn
            for (ProductSelection ps : selections) {

                long lineTotal = ps.product.getPrice() * ps.quantity;
                originalTotal += lineTotal;
                productIds.add(ps.product.getId());

                OrderItem oi = OrderItem.builder()
                        .order(order)
                        .product(ps.product)
                        .supplier(ps.product.getSupplier())
                        .unitPrice(ps.product.getPrice())
                        .quantity(ps.quantity)
                        .lineTotal(lineTotal)
                        .build();

                orderItems.add(oi);
            }

            order.setOriginalTotal(originalTotal);

            long discountAmount = 0L;

            // ===================== KIỂM TRA MÃ GIẢM GIÁ =====================
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
                order.setDiscountCode(req.getDiscountCode());
            }

            order.setDiscountAmount(discountAmount);

            // ===================== TIỀN KHÁCH PHẢI TRẢ =====================
            long finalTotal = originalTotal + order.getShippingFee() - discountAmount;
            order.setFinalTotal(finalTotal);

            // ===================== TIỀN SHOP NHẬN (KHÔNG TÍNH SHIP) =====================
            long shopRevenue = originalTotal - discountAmount;
            order.setShopRevenue(shopRevenue);

            // ⭐ Đặt items sau khi mọi thông tin tiền tệ đã sẵn sàng
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

        ProductSelection(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }

}
