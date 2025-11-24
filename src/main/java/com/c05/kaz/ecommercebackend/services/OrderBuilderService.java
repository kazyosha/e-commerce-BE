package com.c05.kaz.ecommercebackend.services;

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
    private final ProductRepository productRepo; // hỗ trợ BUY NOW
    private final UserAccountService userAccountService;
    private final CustomerProfileService customerProfileService;
    private final PromotionService promotionService;

    public List<Order> buildDraftOrders(CheckoutRequest req) {

        UserAccount user = userAccountService.getCurrentCustomer();
        CustomerProfile customer = customerProfileService.getByUser(user);

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new RuntimeException("Không có sản phẩm nào được chọn để thanh toán.");
        }

        Cart cart = cartRepo.findByCustomer(user).orElse(null);

        Map<SupplierShop, List<ProductSelection>> itemsBySupplier = new HashMap<>();

        for (CheckoutItemRequest itemReq : req.getItems()) {

            Product product;
            int reqQty;

            // --- Checkout từ giỏ ---
            if (itemReq.getCartItemId() != null) {

                if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                    throw new RuntimeException("Giỏ hàng rỗng.");
                }

                CartItem ci = cart.getItems().stream()
                        .filter(c -> Objects.equals(c.getId(), itemReq.getCartItemId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ."));

                product = ci.getProduct();
                reqQty = itemReq.getQuantity() != null ? itemReq.getQuantity() : ci.getQuantity();

            }
            // --- BUY NOW ---
            else if (itemReq.getProductId() != null) {

                product = productRepo.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại."));
                reqQty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            }
            else {
                throw new RuntimeException("Thiếu cartItemId hoặc productId.");
            }

            // Validate chung
            if (reqQty <= 0) throw new RuntimeException("Số lượng phải lớn hơn 0.");
            if (!product.isActive()) throw new RuntimeException("Sản phẩm " + product.getName() + " đang ngừng bán.");
            if ((product.getQuantity() == null ? 0 : product.getQuantity()) < reqQty) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " không đủ tồn kho.");
            }

            SupplierShop supplier = product.getSupplier();
            if (supplier == null) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " không thuộc cửa hàng nào.");
            }

            itemsBySupplier
                    .computeIfAbsent(supplier, k -> new ArrayList<>())
                    .add(new ProductSelection(product, reqQty));
        }

        if (itemsBySupplier.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm để tạo đơn.");
        }

        // ===== BUILD ORDER THEO MỖI SUPPLIER =====
        List<Order> draftOrders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<SupplierShop, List<ProductSelection>> entry : itemsBySupplier.entrySet()) {

            SupplierShop supplier = entry.getKey();
            List<ProductSelection> selections = entry.getValue();

            Order order = new Order();
            order.setSupplier(supplier);
            order.setCustomer(customer);
            order.setStatus(OrderStatus.PENDING);

            // 💥 GÁN SHIPPING FEE (FE gửi lên)
            long shippingFee = req.getShippingFee() != null ? req.getShippingFee() : 0L;
            order.setShippingFee(shippingFee);

            order.setPaymentMethod(
                    req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethod.COD
            );

            // Receiver Info
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

            // Tính tổng tiền sản phẩm
            for (ProductSelection ps : selections) {
                Product product = ps.product;
                int qty = ps.quantity;

                long unitPrice = product.getPrice();
                long lineTotal = unitPrice * qty;

                originalTotal += lineTotal;

                OrderItem oi = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .supplier(product.getSupplier())
                        .unitPrice(unitPrice)
                        .quantity(qty)
                        .lineTotal(lineTotal)
                        .build();

                orderItems.add(oi);
            }

            // Áp mã giảm giá
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

            // ⭐⭐⭐ CỘNG SHIPPING FEE VÀO FINAL TOTAL ⭐⭐⭐
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

    /**
     * Helper nội bộ
     */
    private static class ProductSelection {
        private final Product product;
        private final int quantity;

        public ProductSelection(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }
}
