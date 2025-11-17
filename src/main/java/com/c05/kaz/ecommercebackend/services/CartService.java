package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.cart.AddToCartRequest;
import com.c05.kaz.ecommercebackend.dto.cart.CartItemResponse;
import com.c05.kaz.ecommercebackend.dto.cart.CartResponse;
import com.c05.kaz.ecommercebackend.entity.Cart;
import com.c05.kaz.ecommercebackend.entity.CartItem;
import com.c05.kaz.ecommercebackend.entity.Product;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.repository.CartItemRepository;
import com.c05.kaz.ecommercebackend.repository.CartRepository;
import com.c05.kaz.ecommercebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final UserAccountService customerService;

    // ================== HELPER ==================

    // Lấy cart của customer, nếu chưa có thì tạo mới (chỉ dùng cho thêm SP)
    private Cart getOrCreateCart(UserAccount customer) {
        return cartRepo.findByCustomer(customer)
                .orElseGet(() -> cartRepo.save(
                        Cart.builder()
                                .customer(customer)
                                .build()
                ));
    }

    // Build CartResponse từ customer hiện tại
    private CartResponse buildCartResponse(UserAccount customer) {
        var cartOpt = cartRepo.findByCustomer(customer);

        if (cartOpt.isEmpty()) {
            return CartResponse.builder()
                    .items(java.util.List.of())
                    .totalAmount(0L)
                    .totalItems(0)
                    .build();
        }

        Cart cart = cartOpt.get();

        var itemResponses = cart.getItems().stream()
                .map(item -> {
                    long lineTotal = item.getPriceAtAdd() * item.getQuantity();

                    String thumbnail = null;
                    if (item.getProduct().getImages() != null
                            && !item.getProduct().getImages().isEmpty()) {
                        thumbnail = item.getProduct().getImages().get(0).getImageUrl();
                    }

                    return CartItemResponse.builder()
                            .itemId(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .thumbnail(thumbnail)
                            .price(item.getPriceAtAdd())
                            .quantity(item.getQuantity())
                            .lineTotal(lineTotal)
                            .build();
                })
                .toList();

        long totalAmount = itemResponses.stream()
                .mapToLong(CartItemResponse::getLineTotal)
                .sum();

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .items(itemResponses)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
    }

    // ================== ADD TO CART ==================

    @Transactional
    public void addToCart(AddToCartRequest req) {
        // 1. Lấy customer đang đăng nhập
        UserAccount customer = customerService.getCurrentCustomer();

        // 2. Lấy product
        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại."));

        if (!product.isActive()) {
            throw new RuntimeException("Sản phẩm đang ngừng bán.");
        }

        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0.");
        }

        final int stock = product.getQuantity(); // tồn kho hiện có
        if (stock <= 0) {
            throw new RuntimeException("Sản phẩm đã hết hàng.");
        }

        // 3. Lấy / tạo Cart
        Cart cart = getOrCreateCart(customer);

        // 4. Kiểm tra xem item đã có trong cart chưa
        CartItem item = cartItemRepo.findByCartAndProduct(cart, product)
                .orElse(null);

        final int currentQtyInCart = (item != null) ? item.getQuantity() : 0;
        final int requestedQty = req.getQuantity();
        final int newTotal = currentQtyInCart + requestedQty;

        // Đã có trong giỏ đủ bằng tồn kho → không cho thêm nữa
        if (currentQtyInCart >= stock) {
            throw new RuntimeException(
                    "Bạn đã thêm hết số lượng sản phẩm hiện có. Sản phẩm tạm hết hàng."
            );
        }

        // Thêm lần này sẽ vượt kho → báo chỉ còn X sản phẩm
        if (newTotal > stock) {
            int remaining = stock - currentQtyInCart; // còn lại > 0
            throw new RuntimeException(
                    "Chỉ còn " + remaining + " sản phẩm trong kho. Vui lòng giảm số lượng."
            );
        }

        // 5. Cập nhật / tạo mới CartItem
        if (item == null) {
            item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(newTotal)           // dùng tổng mới
                    .priceAtAdd(product.getPrice())
                    .build();
        } else {
            item.setQuantity(newTotal);
        }

        cartItemRepo.save(item);
    }

    // ================== GET MY CART ==================

    @Transactional(readOnly = true)
    public CartResponse getMyCart() {
        UserAccount customer = customerService.getCurrentCustomer();
        return buildCartResponse(customer);
    }

    // ================== UPDATE ITEM QUANTITY ==================

    @Transactional
    public CartResponse updateItemQuantity(Long itemId, int newQuantity) {
        if (newQuantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0.");
        }

        UserAccount customer = customerService.getCurrentCustomer();

        // Tìm item trong giỏ
        CartItem item = cartItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm trong giỏ không tồn tại."));

        // Đảm bảo item thuộc về customer hiện tại (so theo customer, không dùng getOrCreateCart)
        if (!item.getCart().getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa sản phẩm này.");
        }

        Product product = item.getProduct();

        if (!product.isActive()) {
            throw new RuntimeException("Sản phẩm đang ngừng bán, không thể chỉnh sửa số lượng.");
        }

        int stock = product.getQuantity();
        if (stock <= 0) {
            throw new RuntimeException("Sản phẩm đã hết hàng.");
        }

        if (newQuantity > stock) {
            throw new RuntimeException(
                    "Chỉ còn " + stock + " sản phẩm trong kho. Vui lòng không tăng số lượng quá mức."
            );
        }

        item.setQuantity(newQuantity);
        cartItemRepo.save(item);

        // Trả về giỏ hàng sau khi cập nhật
        return buildCartResponse(customer);
    }

    // ================== REMOVE ITEM ==================

    @Transactional
    public CartResponse removeItem(Long itemId) {
        UserAccount customer = customerService.getCurrentCustomer();

        CartItem item = cartItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm trong giỏ không tồn tại."));

        if (!item.getCart().getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Bạn không có quyền xoá sản phẩm này khỏi giỏ.");
        }

        Cart cart = item.getCart();

        // Xoá khỏi list trong Cart (không bắt buộc nhưng tốt cho state trong persistence context)
        cart.getItems().remove(item);

        cartItemRepo.delete(item);

        // Nếu muốn chắc chắn, có thể:
        // cartItemRepo.flush();

        return buildCartResponse(customer);
    }
}
