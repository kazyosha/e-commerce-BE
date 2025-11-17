package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.CartItem;
import com.c05.kaz.ecommercebackend.entity.Cart;
import com.c05.kaz.ecommercebackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}
